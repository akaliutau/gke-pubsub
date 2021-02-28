#####################################################################
# GKE Cluster
#####################################################################
resource "google_container_cluster" "letter-processing-cluster" {
  name = "letter-processing-cluster"
  location = var.region
  initial_node_count = 1

#  master_auth {
#    username = var.username
#    password = var.password
#  }

  node_config {
    machine_type = "e2-small"
    oauth_scopes = [
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/service.management.readonly",
      "https://www.googleapis.com/auth/servicecontrol",
      "https://www.googleapis.com/auth/trace.append",
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/cloud-platform"
    ]
  }
}

#####################################################################
# Dynamic output to be used by K8S module
#####################################################################
output "client_certificate" {
  value = google_container_cluster.letter-processing-cluster.master_auth[0].client_certificate
  sensitive = true
}
output "client_key" {
  value = google_container_cluster.letter-processing-cluster.master_auth[0].client_key
  sensitive = true
}
output "cluster_ca_certificate" {
  value = google_container_cluster.letter-processing-cluster.master_auth[0].cluster_ca_certificate
  sensitive = true
}
output "host" {
  value = "https://${google_container_cluster.letter-processing-cluster.endpoint}"
  sensitive = true
}