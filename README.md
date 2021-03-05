# About

Alice and Bob are big friends, but they live in different cities (Alice in Amsterdam and Bob in Boston) and can't see each other in person.
So they communicate mostly via letters writing.

Alice just loves receiving letters, but has little time to read them. 
She decided to create a Reading Machine on the basis of Kubernetes cluster.


# Implementation

This is a simple Spring Boot - based project to build a k8s cluster of scalable apps to process messages from PubSub topic. 

## Further details

There is a topic - `postbox` -  where Bob can publish messages to, and a cluster of listeners (letter-reader app) subscribed to this topic. 
These applications can pickup messages from the topic and process them.

Each message has the following format:

```
{
  "id": "<unique uuid>",
  "recent_attemtps": 1
}
```

- Field `id` is required and must be unique across all letters
- Field `recent_attemtps` is optional and indicates the number of recent attempts made for this message.

There is a 2nd topic with name `read_letters`, which is used to count the total number of processed messages. 
The dashboard app is basically just an interface to this topic, and collects other useful statistics along the way.
Dashboard's api is available via Swagger at `https://localhost:9000/swagger-ui/`

# How to build

Here the sequence of steps to perform (most of these steps will be covered in next sections)

* Create Docker images for `dashboard` and `letter-reader` apps
* Upload the Docker images to a private Docker registry at Google using `docker push` command
* Use Terraform to define infrastructure-as-a-code which includes a Google Cloud k8s instances and PubSub topics/subscriptions.
* Deploy a Kubernetes cluster using Terraform to Google Cloud
* Deploy one instance of the `letter-reader` Docker image to the Kubernetes cluster
* Deploy one instance of the `dashboard` Docker image to the Kubernetes cluster
* Scale out the core processing part of the cluster to 3 instances of the `letter-reader` application without any changes 
  to production java code


# Requirements

* gcloud (see [2])
* Java 11 SDK
* terraform (see [4])
* Kubectl (Kubectl aka Kube, is a command line tool for controlling Kubernetes clusters)
* ssh


# Environment settings

The Spring Cloud GCP Core Boot starter can be auto-configured using properties from the properties file (`src/main/resources/application.yml`) 
which always have precedence over the Spring Boot configuration.

The GCP project ID is auto-configured from the `GOOGLE_CLOUD_PROJECT` environment variable, among several other sources. 

The OAuth2 credentials are auto-configured from the `GOOGLE_APPLICATION_CREDENTIALS` environment variable.

This var can be set manually after auto-generating json with google account credentials:

```
gcloud auth application-default login
```


The path to gcloud creds usually has the form:

```
/$HOME/.config/gcloud/legacy_credentials/$EMAIL/adc.json
```

where variable $EMAIL can be obtained via command:

```
gcloud config list account --format "value(core.account)"
```

Add GOOGLE_APPLICATION_CREDENTIALS as permanent vars into the file:

```
sudo -H gedit /etc/environment
```

To access cloud instances the SSH key is needed, it can be created using the following command:

```
ssh-keygen -t rsa -f ~/.ssh/pubsub_rsa -C $USERNAME -b 2048
```


# Settings on GCP side

(0) For convenience and generalization, set the env variable GOOGLE_CLOUD_PROJECT in file set_env.sh to your project id, f.e. 
`export GOOGLE_CLOUD_PROJECT=message-multi-processor`

(1) Create a project:

```
gcloud projects create $GOOGLE_CLOUD_PROJECT
```

After successful creation project_id must be visible via command `gcloud projects list`

(2) Activate billing account for project and enable PubSub and GKE services

(3) Build an image with app and push it to GCloud private docker registry, f.e. :

```
sudo docker build -t gcp-dashboard:0.0.4 ./dashboard/
sudo docker build -t gcp-letter-reader:0.0.4 ./letter-reader/
```

Docker image can be tested with the help of command (`ctrl+shift+c` to stop):
```
sudo docker run -p 9000:9000 \
    --env=GOOGLE_CLOUD_PROJECT=$GOOGLE_CLOUD_PROJECT \
    --env=GOOGLE_APPLICATION_CREDENTIALS=/secrets/adc.json \
    --volume=$GOOGLE_APPLICATION_CREDENTIALS:/secrets/adc.json \
    gcp-dashboard:0.0.1
```

(4) Authenticate the docker registry (after update, the following will be written to your Docker config file located at  
`/root/.docker/config.json`), and push the image:

```
curl -fsSL "https://github.com/GoogleCloudPlatform/docker-credential-gcr/releases/download/v${VERSION}/docker-credential-gcr_${OS}_${ARCH}-${VERSION}.tar.gz" > helper.tar.gz
tar xz -f ./helper.tar.gz
sudo mv ./docker-credential-gcr  /usr/local/bin/docker-credential-gcr
sudo chmod +x /usr/local/bin/docker-credential-gcr
sudo docker-credential-gcr configure-docker
```

then tag the image and push it to the registry:

```
sudo docker tag gcp-dashboard:0.0.1 gcr.io/$GOOGLE_CLOUD_PROJECT/gcp-dashboard:v1
sudo docker push gcr.io/$GOOGLE_CLOUD_PROJECT/gcp-dashboard:v1
```

(5) Verify the pulling docker image from GCP registry: test the image with the following command, 
which will run a Docker container as a daemon on port 9000 from your newly created container image:

```
sudo docker run -ti --rm -p 9000:9000 \
  --env=GOOGLE_CLOUD_PROJECT=$GOOGLE_CLOUD_PROJECT \
  --env=GOOGLE_APPLICATION_CREDENTIALS=/secrets/adc.json \
  --volume=$GOOGLE_APPLICATION_CREDENTIALS:/secrets/adc.json \
  gcr.io/$GOOGLE_CLOUD_PROJECT/gcp-dashboard:v1
```
(6) Generate temporary SSL certificates with the help of `cerbot` 

```
python3 -m venv venv
source ./venv/bin/activate
pip install -r requirements.txt
python3 cerbot <args>
```

OR as an alternative create self-signed certificates with the help of openssl utility:

```
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout ./terraform/secrets/dashboard-private.key \
        -out ./terraform/secrets/dashboard-certificate.crt
```

(7) Create infrastructure using Terraform:

For Terraform it's necessary to set in file set_env.sh the following variables:

```
export TF_VAR_google_app_creds=$GOOGLE_APPLICATION_CREDENTIALS
export TF_VAR_project_id=$GOOGLE_CLOUD_PROJECT
```
(8) Run terraform init to download the latest version of the provider and build the .terraform directory

```
terraform init
terraform plan
terraform apply
```

Instances will be available via command:

```
gcloud compute instances list
```

One can login to them and inspect using SSH access and then see the startup logs, check the java version, etc:

```
gcloud beta compute ssh --zone "us-west1-a" "proc-vm-d1c76370a95aa91c"  --project $GOOGLE_CLOUD_PROJECT
cat /var/log/syslog 
java --version
```
Note, instance ID (here it's `proc-vm-d1c76370a95aa91c`) may change after each recreation.

## Clean up

(1) First, destroy the resources created by Terraform: 

```
terraform apply -destroy
```

## Set up and tune up a scaling group

Here and next a recipe from [5] is applied

First, deploying the Custom Metrics Adapter:

```
https://cloud.google.com/kubernetes-engine/docs/tutorials/autoscaling-metrics#step1
```

The name of metric can be picked up from [metrics-explorer](https://console.cloud.google.com/monitoring/metrics-explorer)

Here we are using `pubsub.googleapis.com|subscription|num_undelivered_messages`

Created metrics are available via GKE -> Workloads -> letter-reader -> Autoscaler


# Running application locally

Build app using command `mvn clean package`, then use the command `java -jar target/pubsub-0.0.1-SNAPSHOT.jar` to run an app in a separate window.
 

# References

[1] https://github.com/spring-guides/gs-messaging-gcp-pubsub

[2] https://cloud.google.com/sdk/docs/install#deb

[3] https://cloud.google.com/sdk/docs/components

[4] https://cloud.google.com/docs/terraform (an open-source infrastructure as code software tool created by HashiCorp)

[5] https://cloud.google.com/kubernetes-engine/docs/tutorials/autoscaling-metrics#pubsub

[6] https://www.gcpweekly.com/gcp-resources

[7] https://cloud.google.com/compute/docs/instances/access-overview

[8] https://kubernetes.io/docs/tasks/tools/install-kubectl/ (Kubectl is a command line tool for controlling Kubernetes clusters)

[9] https://github.com/steinim/gcp-terraform-workshop


# Appendix 1. Terraform installation on Ubuntu 20.04 LTS

```
sudo apt-get update && sudo apt-get install -y gnupg software-properties-common curl
curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo apt-key add -
sudo apt-add-repository "deb [arch=amd64] https://apt.releases.hashicorp.com $(lsb_release -cs) main"
sudo apt-get update && sudo apt-get install terraform
```

Check the valid installation:

```
terraform -help
```

# Appendix 2. Terraform basics

*Providers*: a provider is responsible for understanding API interactions and exposing resources. 
Providers generally are an IaaS (e.g. AWS, GCP, Microsoft Azure, OpenStack), PaaS (e.g. Heroku), or SaaS services

*Resources*: resources are the most important element in the Terraform language. 
Each resource block describes one or more infrastructure objects, such as virtual networks, compute instances

*Variables*: a variable can have a default value. In case of omitted default values, Terraform will ask to provide it 
when running a terraform command

*Modules*: a module is just a folder which combines related terraform files

*Outputs*: sometimes a variable is needed which is only known after terraform has done a change on a cloud provider — 
f.e. ip-addresses that are given to application. So output serves as an intermediate holding agent - 
it takes that value and exposes it to your variables

# Appendix 3. Docker commands

The list of all available images can be accessed using `sudo docker image list` command.

Removing tagged images:

First, untag it, f.e.:

```
sudo docker image rm gcr.io/message-multi-processor/gcp-pubsub:v1
sudo docker image rm gcr.io/message-multi-processor/gcp-pubsub:latest
```

# Appendix 4. Troubleshooting

(1) This command can turn on services:

```
gcloud services enable <service>
f.e.
gcloud services enable container
```

(2) Error `Node pools of f1-micro machines are not supported due to insufficient memory`: 

https://cloud.google.com/kubernetes-engine/docs/concepts/cluster-architecture#memory_cpu

https://cloud.google.com/compute/docs/general-purpose-machines#e2-shared-core

Note, for Docker-based java apps its better to choose `e2-small` at least 

(3) Boot time optimisation:

```
o.s.boot.StartupInfoLogger : InetAddress.getLocalHost().getHostName() took 264 milliseconds to respond. Please verify your network configuration.
```

```
[Channel<7>: (pubsub.googleapis.com:443)] Failed to resolve name. status=Status{code=UNAVAILABLE, description=Unable to resolve host pubsub.googleapis.com, 
cause=java.lang.RuntimeException: java.net.UnknownHostException: pubsub.googleapis.com 
at io.grpc.internal.DnsNameResolver.resolveAddresses(DnsNameResolver.java:223) 
at io.grpc.internal.DnsNameResolver.doResolve(DnsNameResolver.java:282)
```

Such messages are solid hints that some configuration parameters (such as timeouts, etc) need tuning
For example, for liveness_probe.timeout_seconds it's better to set a min value starting from 5s (the default value in 1s is too small)

(4) Autoscaling

check custom metrics driver was deployed and running:

```
kubectl describe hpa
```

