#####################################################################
# Global variables
#####################################################################
variable "project" {
  default = ""
}
variable "region" {
  default = "europe-west2"
}
variable "username" {
  default = "c_username"
}
variable "password" {
  default = "c_password"
}
variable "google_app_creds" {
  default = ""
}
#####################################################################
# Modules
#####################################################################
module "gke" {
  source = "./gke"

  project = var.project
  region = var.region
  username = var.username
  password = var.password
}

module "k8s" {
  source = "./k8s"

  project = var.project
  google_app_creds = var.google_app_creds
  username = var.username
  password = var.password

  host = module.gke.host
  global_ip = module.gke.global_ip
  client_certificate = module.gke.client_certificate
  client_key = module.gke.client_key
  cluster_ca_certificate = module.gke.cluster_ca_certificate
}