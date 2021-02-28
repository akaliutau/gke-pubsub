#####################################################################
# K8S Service
#####################################################################
resource "kubernetes_service" "letter-reader" {
  metadata {
    name = "letter-reader"
  }
  spec {
    selector = {
      app = "letter-reader"
    }
    port {
      name = "rest-api"
      port = 8080
      target_port = 8080
    }
    type = "NodePort"
  }
}