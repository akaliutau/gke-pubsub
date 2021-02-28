resource "google_pubsub_topic" "postbox" {
  count = 1
  name = "postbox"
}

resource "google_pubsub_subscription" "postbox" {
  count = 1
  name = "postbox"
  topic = google_pubsub_topic.postbox[0].name
  ack_deadline_seconds = 600

  expiration_policy {
    ttl = "" # never
  }
}

resource "google_pubsub_topic" "read_letters" {
  count = 1
  name = "read_letters"
}

resource "google_pubsub_subscription" "read_letters" {
  count = 1
  name = "read_letters"
  topic = google_pubsub_topic.read_letters[0].name
  ack_deadline_seconds = 600

  expiration_policy {
    ttl = "" # never
  }
}
