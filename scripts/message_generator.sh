#!/bin/bash

if [ "$#" -ne 1 ]; then
  echo "usage: message_generator.sh num_of_messages"
  exit 1
fi

for (( i=1; i<=$1; i++ ))
do
  gcloud pubsub topics publish "postbox" --message="{\"id\":\"letter_$i\"}"
done

echo "sent $1 messages"