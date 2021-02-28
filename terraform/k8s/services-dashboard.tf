#####################################################################
# K8S Service
#####################################################################
resource "kubernetes_service" "dashboard" {
  metadata {
    name = "dashboard"
  }
  spec {
    selector = {
      app = "dashboard"
    }
    port {
      name = "rest-api"
      port = 9000
      target_port = 9000
    }
    type = "NodePort"
  }
}