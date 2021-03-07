package messaging;

import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GCPublisher implements AutoCloseable {

    final TopicName topicName;
    final Publisher publisher;

    public GCPublisher(String projectId, String topicId) throws IOException {
        topicName = TopicName.of(projectId, topicId);
        log.info("topicName = {}", topicName);
        publisher = Publisher.newBuilder(topicName).build();
    }

    public void publish(String message) {
        ByteString data = ByteString.copyFromUtf8(message);
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

        // Once published, returns a server-assigned message id (unique within the topic)
        // Add an asynchronous callback to handle success / failure
        ApiFutures.addCallback(
                publisher.publish(pubsubMessage),
                new ApiFutureCallback<>() {

                    @Override
                    public void onFailure(Throwable throwable) {
                        if (throwable instanceof ApiException) {
                            ApiException apiException = ((ApiException) throwable);
                            log.error("{}", apiException.getStatusCode().getCode());
                            log.error("{}", apiException.isRetryable());
                        }
                        log.error("Error publishing message {}", message);
                    }

                    @Override
                    public void onSuccess(String messageId) {
                        log.info("Published message ID={}", messageId);
                    }
                },
                MoreExecutors.directExecutor()
        );
    }

    @Override
    public void close() throws Exception {
        if (publisher != null) {
            // When finished with the publisher, shutdown to free up resources.
            publisher.shutdown();
            publisher.awaitTermination(1, TimeUnit.MINUTES);
        }
    }
}
