resource "kubernetes_config_map" "adc" {
  metadata {
    name = "adc"
  }
  data = {
    "adc.json" = file(var.google_app_creds)
  }
}