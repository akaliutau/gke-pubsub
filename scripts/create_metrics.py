import argparse
import os
import pprint
import time
import uuid

from google.cloud import monitoring_v3



client = monitoring_v3.MetricServiceClient()
project_name = "projects/message-multi-processor"

series = monitoring_v3.TimeSeries()
series.metric.type = "pubsub.googleapis.com/subscription/num_undelivered_messages"
series.resource.type = "gce_instance"
series.resource.labels["instance_id"] = "1234567890123456789"
series.resource.labels["zone"] = "us-central1-f"
series.metric.labels["TestLabel"] = "Label1"
now = time.time()
seconds = int(now)
nanos = int((now - seconds) * 10 ** 9)
interval = monitoring_v3.TimeInterval(
    {"end_time": {"seconds": seconds, "nanos": nanos}}
)
point = monitoring_v3.Point({"interval": interval, "value": {"double_value": 3.14}})
series.points = [point]
client.create_time_series(name=project_name, time_series=[series])
