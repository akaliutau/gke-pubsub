#####################################################################
# Network resources
# address argument is omitted to allow GCP to choose a valid one
#####################################################################
resource "google_compute_global_address" "default" {
  name = "global-dashboard-ip"
}

output "global_ip" {
  value = google_compute_global_address.default.address
  sensitive = true
}