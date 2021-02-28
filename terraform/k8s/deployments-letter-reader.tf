#####################################################################
# K8S Deployment
#####################################################################

resource "kubernetes_deployment" "letter-reader" {
  metadata {
    name = "letter-reader"
    labels = {
      app = "letter-reader"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "letter-reader"
      }
    }
    template {
      metadata {
        labels = {
          app = "letter-reader"
        }
      }
      spec {
        container {
          image = "gcr.io/${var.project}/gcp-letter-reader:v4"
          name = "letter-reader"
          port {
            container_port = 8080
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
              port =  "8080"
              path = "/actuator/health"
            }
          }

         readiness_probe {
            initial_delay_seconds = "20"
            period_seconds = "30"
           timeout_seconds = "5"
            http_get {
              port =  "8080"
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