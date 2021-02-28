#####################################################################
# K8S Deployment
#####################################################################

resource "kubernetes_deployment" "dashboard" {
  metadata {
    name = "dashboard"
    labels = {
      app = "dashboard"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "dashboard"
      }
    }
    template {
      metadata {
        labels = {
          app = "dashboard"
        }
      }
      spec {
        container {
          image = "gcr.io/${var.project}/gcp-dashboard:v4"
          name = "dashboard"
          port {
            container_port = 9000
          }

          volume_mount {
            name       = "adc-json"
            mount_path = "/secrets"
          }

          env {
            name = "GOOGLE_CLOUD_PROJECT"
            value = var.project
          }
          env {
            name = "GOOGLE_APPLICATION_CREDENTIALS"
            value = "/secrets/adc.json"
          }

          liveness_probe {
            initial_delay_seconds = "20"
            period_seconds = "30"
            timeout_seconds = "5"
            http_get {
              port =  "9000"
              path = "/actuator/health"
            }
          }

          readiness_probe {
            initial_delay_seconds = "20"
            period_seconds = "30"
            timeout_seconds = "5"
            http_get {
              port =  "9000"
              path = "/actuator/health"
            }
          }

        }

        volume {
          name = "adc-json"
          config_map {
            name = "adc"
            items {
              key  = "adc.json"
              path = "adc.json"
            }
          }
        }

      }
    }
  }
}