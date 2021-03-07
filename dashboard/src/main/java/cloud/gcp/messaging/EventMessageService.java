package cloud.gcp.messaging;

import cloud.gcp.service.ServiceState;
import cloud.gcp.service.StatisticsService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.pubsub.v1.PubsubMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import messaging.GCSubscriber;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
@EnableAutoConfiguration
public class EventMessageService {

    private String projectId;

    private static final Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
    private StatisticsService statisticsService;
    private GCSubscriber pubsubReadLetterSubscriber;

    @Getter
    private ServiceState state;

    private EventMessageService(ServiceState state, StatisticsService statisticsService, Environment env) throws IOException {
        this.statisticsService = statisticsService;
        projectId = env.getRequiredProperty("GOOGLE_CLOUD_PROJECT");
        pubsubReadLetterSubscriber = new GCSubscriber(projectId, "read_letters", onMessage());
        pubsubReadLetterSubscriber.start();
        log.info("creating an EventMessageService for project = {} sec", projectId);
    }

    /**
     * A simple message handler.
     */
    private Consumer<PubsubMessage> onMessage() {
        return message -> {
            Map<String, String> msg = gson.fromJson(
                    message.getData().toStringUtf8(),
                    new TypeToken<Map<String, String>>() {
                    }.getType());
            log.info("Message added to statistics: {} ", msg);
            statisticsService.addId(msg.get("id"));
            statisticsService.incTotalLettersRead(1);
        };
    }

    public void close() throws Exception {
        pubsubReadLetterSubscriber.close();
    }


}
