resource "google_compute_ssl_certificate" "default" {
  name        = "tls-cert"
  description = "Provides SSL encryption for dashboard app endpoint"
  private_key = file("./secrets/dashboard-private.key")
  certificate = file("./secrets/dashboard-certificate.crt")

  lifecycle {
    create_before_destroy = true
  }
}

resource "kubernetes_secret" "default" {

  metadata {
    name      = "tls-secret"
    labels = {
      "sensitive" = "true"
      "app"       = "dashboard"
    }
  }
  type = "kubernetes.io/tls"
  data = {
    "tls.crt" = file("./secrets/dashboard-certificate.crt")
    "tls.key" = file("./secrets/dashboard-private.key")
  }
}

resource "kubernetes_ingress" "default" {
  metadata {
    name = "tls-ingress"
    annotations = {
#      "kubernetes.io/ingress.global-static-ip-name" = var.global_ip
      "kubernetes.io/ingress.allow-http"            = "true"
      "kubernetes.io/ingress.class"  = "gce"
#      "ingress.kubernetes.io/force-ssl-redirect"    = "true"
    }
  }

  spec {
    backend {
      service_name = "dashboard"
      service_port = "9000"
    }

    rule {
      http {
        path {
          backend {
            service_name = "dashboard"
            service_port = "9000"
          }
          path = "/*"

        }

      }
    }

    tls {
      secret_name = "tls-secret"
    }
  }
}
