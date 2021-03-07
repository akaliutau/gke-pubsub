resource "kubernetes_namespace" "custom-metrics" {
  metadata {
    name = "custom-metrics"
    labels = {}
  }
}

resource "kubernetes_service_account" "metrics_driver"{
  metadata {
    name        = "custom-metrics-stackdriver-adapter"
    namespace   = "custom-metrics"

    annotations = {
      "iam.gke.io/gcp-service-account"="custom-metrics-stackdriver-adapter@${var.project}.iam.gserviceaccount.com"
    }
  }
  depends_on = [kubernetes_namespace.custom-metrics]
}

resource "kubernetes_deployment" "driver-deployment" {
  metadata {
    name = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
    labels = {
      run = "custom-metrics-stackdriver-adapter"
      k8s-app = "custom-metrics-stackdriver-adapter"
    }
  }
  depends_on = [kubernetes_namespace.custom-metrics, kubernetes_service_account.metrics_driver]

  spec {
    replicas = 1
    selector {
      match_labels = {
        run = "custom-metrics-stackdriver-adapter"
        k8s-app = "custom-metrics-stackdriver-adapter"
      }
    }
    template {
      metadata {
        labels = {
          run = "custom-metrics-stackdriver-adapter"
          k8s-app = "custom-metrics-stackdriver-adapter"
          "kubernetes.io/cluster-service" = "true"
        }
      }

      spec {
        service_account_name = "custom-metrics-stackdriver-adapter"
        container {
          image = "gcr.io/gke-release/custom-metrics-stackdriver-adapter:v0.12.2-gke.0"
          image_pull_policy = "Always"
          name = "pod-custom-metrics-stackdriver-adapter"
          command = ["/adapter", "--use-new-resource-model=true", "--fallback-for-container-metrics=true"]
          resources {
            limits = {
              cpu = "550m"
              memory = "600Mi"
            }
            requests = {
              cpu = "250m"
              memory = "200Mi"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "adapter-service" {
  metadata {
    name = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
    labels = {
      run = "custom-metrics-stackdriver-adapter"
      k8s-app = "custom-metrics-stackdriver-adapter"
      "kubernetes.io/cluster-service" = "true"
      "kubernetes.io/name" = "Adapter"
    }
  }
  spec {
    selector = {
      run = "custom-metrics-stackdriver-adapter"
      k8s-app = "custom-metrics-stackdriver-adapter"
    }
    port {
      port = 443
      target_port = 443
      protocol = "TCP"
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_api_service" "custom-metrics-api-service1" {
  metadata {
    name = "v1beta1.custom.metrics.k8s.io"
  }
  spec {
    group = "custom.metrics.k8s.io"
    group_priority_minimum = 100
    version = "v1beta1"
    version_priority = 100
    insecure_skip_tls_verify = "true"
    service {
      name = "custom-metrics-stackdriver-adapter"
      namespace = "custom-metrics"
    }
  }
}

resource "kubernetes_api_service" "custom-metrics-api-service2" {
  metadata {
    name = "v1beta2.custom.metrics.k8s.io"
  }
  spec {
    group = "custom.metrics.k8s.io"
    group_priority_minimum = 100
    version = "v1beta2"
    version_priority = 200
    insecure_skip_tls_verify = "true"
    service {
      name = "custom-metrics-stackdriver-adapter"
      namespace = "custom-metrics"
    }
  }
}

resource "kubernetes_api_service" "custom-metrics-api-ext-service" {
  metadata {
    name = "v1beta1.external.metrics.k8s.io"
  }
  spec {
    group = "external.metrics.k8s.io"
    group_priority_minimum = 100
    version = "v1beta1"
    version_priority = 100
    insecure_skip_tls_verify = "true"
    service {
      name = "custom-metrics-stackdriver-adapter"
      namespace = "custom-metrics"
    }
  }
}


resource "kubernetes_cluster_role" "external-reader" {
  metadata {
    name = "metrics-reader"
  }
  rule {
    api_groups = ["external.metrics.k8s.io"]
    resources = ["*"]
    verbs = ["list", "get", "watch"]
  }
}

resource "kubernetes_cluster_role_binding" "external-metrics-reader" {
  metadata {
    name = "metrics-reader"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "metrics-reader"
  }
  subject {
    kind      = "ServiceAccount"
    name      = "horizontal-pod-autoscaler"
    namespace = "kube-system"
  }
}
