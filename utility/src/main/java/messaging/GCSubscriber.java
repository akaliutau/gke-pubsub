package messaging;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
public class GCSubscriber implements AutoCloseable {

    private final ProjectSubscriptionName subsName;
    private final Subscriber subscriber;

    public GCSubscriber(String projectId, String subscriptionName, Consumer<PubsubMessage> onMessage) throws IOException {
        // process only 1 message in any time
        ExecutorProvider executorProvider =
                InstantiatingExecutorProvider.newBuilder()
                        .setExecutorThreadCount(1)
                        .build();

        // Instantiate an asynchronous message receiver.
        MessageReceiver receiver =
                (PubsubMessage message, AckReplyConsumer consumer) -> {
                    // Handle incoming message, then ack the received message.
                    onMessage.accept(message);
                    consumer.ack();
                };

        this.subsName = ProjectSubscriptionName.of(projectId, subscriptionName);
        log.info("subsName = {}", subsName);
        this.subscriber = Subscriber.newBuilder(subsName, receiver)
                .setParallelPullCount(1)
                .setSystemExecutorProvider(executorProvider)
                .build();
    }

    public void start() {
        // Start the subscriber.
        subscriber.startAsync().awaitRunning();
        log.info("Listening for messages on {}", subsName.getSubscription());
    }


    @Override
    public void close() {
        if (subscriber != null)
            // Shut down the subscriber after 30s. Stop receiving messages.
            subscriber.stopAsync();
    }
}
