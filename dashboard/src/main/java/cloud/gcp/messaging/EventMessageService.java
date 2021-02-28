package cloud.gcp.messaging;

import cloud.gcp.service.StatisticsService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EventMessageService {

    private final Gson gson;
    private StatisticsService statisticsService;

    private EventMessageService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        this.gson = new Gson().newBuilder().setPrettyPrinting().create();
    }

    /**
     * Creates an inbound channel adapter to listen to the subscription  `read_letters`
     */
    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("pubsubReadLetters") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, "read_letters");
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    /**
     * Creates a message channel for messages from read_letters
     */
    @Bean
    @Qualifier("pubsubReadLetters")
    public MessageChannel pubsubReadLetters() {
        return new DirectChannel();
    }

    /**
     * A simple message handler.
     */
    @Bean
    @ServiceActivator(inputChannel = "pubsubReadLetters")
    public MessageHandler messageReceiver() {
        return message -> {
            Map<String, String> msg = gson.fromJson(
                    new String((byte[]) message.getPayload()),
                    new TypeToken<Map<String, String>>() {
                    }.getType());
            log.info("Message: {} ", msg);
            statisticsService.addId(msg.get("id"));
            statisticsService.incTotalLettersRead(1);
            BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
                    .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            originalMessage.ack();
        };
    }
}
