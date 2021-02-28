#####################################################################
# K8S Provider
# some examples can be found at
# https://registry.terraform.io/providers/hashicorp/kubernetes/latest/docs/guides/getting-started
#####################################################################
data "google_client_config" "provider" {}
resource "kubernetes_cluster_role_binding" "sa-cluster-bind" {
  metadata {
    name = "cluster-admin-binding"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind = "ClusterRole"
    name = "cluster-admin"
  }
  subject {
    kind = "User"
    name = "admin"
    api_group = "rbac.authorization.k8s.io"
  }
  subject {
    kind = "User"
    name = "system:anonymous"
    api_group = "rbac.authorization.k8s.io"
  }
  subject {
    kind      = "ServiceAccount"
    name      = "default"
    namespace = "kube-system"
  }
  subject {
    kind      = "Group"
    name      = "system:masters"
    api_group = "rbac.authorization.k8s.io"
  }
  subject {
    kind      = "Group"
    name      = "system:anonymous"
    api_group = "rbac.authorization.k8s.io"
  }
}

provider "kubernetes" {
  host = var.host

#  should be either key+cert or username/password pair
#  username = var.username
#  password = var.password

  #client_certificate = base64decode(var.client_certificate)
  #client_key = base64decode(var.client_key)
  cluster_ca_certificate = base64decode(var.cluster_ca_certificate)
  token                  = data.google_client_config.provider.access_token
}