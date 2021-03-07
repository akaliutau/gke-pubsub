resource "kubernetes_cluster_role_binding" "delegator" {
  metadata {
    name = "custom-metrics:system:auth-delegator"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "system:auth-delegator"
  }
  subject {
    kind      = "ServiceAccount"
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
  }
}

resource "kubernetes_cluster_role_binding" "resource-reader" {
  metadata {
    name = "custom-metrics-resource-reader"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "view"
  }
  subject {
    kind      = "ServiceAccount"
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
  }
}

resource "kubernetes_role_binding" "reader" {
  metadata {
    name = "custom-metrics-auth-reader"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "Role"
    name      = "extension-apiserver-authentication-reader"
  }
  subject {
    kind      = "ServiceAccount"
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
  }
}

