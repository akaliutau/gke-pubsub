resource "kubernetes_horizontal_pod_autoscaler" "default-scaler" {
  metadata {
    name = "letter-reader"
  }

  spec {
    min_replicas = 1
    max_replicas = 10

    scale_target_ref {
      kind = "Deployment"
      name = "letter-reader"
    }

    metric {
      type = "External"
      external {
        metric {
          name = "pubsub.googleapis.com|subscription|num_undelivered_messages"
          selector {
            match_labels = {
              lb_name = "postbox"
            }
          }
        }
        target {
          type  = "Value"
          value = "2"
        }
      }
    }
  }
}