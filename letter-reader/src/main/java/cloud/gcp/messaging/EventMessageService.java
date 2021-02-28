package cloud.gcp.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
@Slf4j
@EnableAutoConfiguration
public class EventMessageService {

    @Value("${time-to-read-sec}")
    private int timeToReadSec;

    @Autowired
    private PubsubOutboundGateway messagingGateway;


    /**
     * Creates an inbound channel adapter to listen to the subscription  `postbox`
     */
    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("pubsubPostBox") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, "postbox");
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    /**
     * Creates a message channel for messages from postbox
     */
    @Bean
    @Qualifier("pubsubPostBox")
    public MessageChannel pubsubPostBox() {
        return new DirectChannel();
    }

    @MessagingGateway(defaultRequestChannel = "pubsubReadLetters")
    public interface PubsubOutboundGateway {
        void sendToPubsub(String msg) throws MessagingException;
    }

    /**
     * A simple message publisher.
     */
    @Bean
    @ServiceActivator(inputChannel = "pubsubReadLetters")
    public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
        PubSubMessageHandler adapter = new PubSubMessageHandler(pubsubTemplate, "read_letters");

        ListenableFutureCallback<String> callback = new ListenableFutureCallback<>() {

            @Override
            public void onSuccess(String result) {
                log.info("Message was sent to read_letters topic, result: {}", result);
            }

            @Override
            public void onFailure(Throwable ex) {
                log.info("Error sending a message due to {}", ex.getMessage());
            }
        };

        adapter.setPublishCallback(callback);
        return adapter;
    }


    /**
     * A simple message handler.
     */
    @Bean
    @ServiceActivator(inputChannel = "pubsubPostBox")
    public MessageHandler messageReceiver() {
        return message -> {
            String msg = new String((byte[]) message.getPayload());
            log.info("Message: {} ", msg);
            log.info("Start reading");
            try {
                log.info("sleeping for {} sec", timeToReadSec);
                Thread.sleep(timeToReadSec);
            } catch (InterruptedException e) {
                log.warn("Interrupted with message {}", e.getMessage());
            }
            log.info("Finish reading");
            BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
                    .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            if (originalMessage != null) {
                log.info("acknowledged");
                originalMessage.ack();
            } else {
                log.warn("cannot extract original message");
            }
            messagingGateway.sendToPubsub(msg);
        };
    }
}
