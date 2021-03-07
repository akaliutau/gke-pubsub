package cloud.gcp.messaging;

import cloud.gcp.config.ServiceState;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.pubsub.v1.PubsubMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import messaging.GCPublisher;
import messaging.GCSubscriber;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
//@EnableAutoConfiguration
public class EventMessageService {

    private static int timeToReadSec = 30;

    private String projectId;

    private static final Gson gson = new Gson().newBuilder().setPrettyPrinting().create();

    private GCPublisher pubsubReadLetterPublisher;
    private GCSubscriber pubsubPostboxSubscriber;

    @Getter
    private ServiceState state;

    public EventMessageService(ServiceState state, Environment env) throws IOException {
        projectId = env.getRequiredProperty("GOOGLE_CLOUD_PROJECT");
        pubsubReadLetterPublisher = new GCPublisher(projectId, "read_letters");
        pubsubPostboxSubscriber = new GCSubscriber(projectId, "postbox", onMessage());
        pubsubPostboxSubscriber.start();
        this.state = state;
        log.info("creating an EventMessageService for project = {} sec", projectId);
    }

    private Consumer<PubsubMessage> onMessage() {
        return message -> {
            Map<String, String> msg = gson.fromJson(
                    message.getData().toStringUtf8(),
                    new TypeToken<Map<String, String>>() {
                    }.getType());
            log.info("Message: {} ", msg);
            log.info("Start reading");
            try {
                Thread.sleep(timeToReadSec * 1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
            log.info("Finish reading");
            this.pubsubReadLetterPublisher.publish(message.getData().toStringUtf8());
        };
    }

    public void close() throws Exception {
        pubsubReadLetterPublisher.close();
        pubsubPostboxSubscriber.close();
    }

}
