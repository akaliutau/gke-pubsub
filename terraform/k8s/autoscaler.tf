resource "kubernetes_horizontal_pod_autoscaler" "default-scaler" {
  metadata {
    name = "letter-reader-scaler"
    namespace = "default"
  }

  spec {
    min_replicas = 1
    max_replicas = 10

    scale_target_ref {
      kind = "Deployment"
      name = "letter-reader"
      api_version = "apps/v1"
    }

    metric {
      type = "External"
      external {
        metric {
          name = "pubsub.googleapis.com|subscription|num_undelivered_messages"
          selector {
            match_labels = {
               "resource.labels.subscription_id" = "postbox"
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