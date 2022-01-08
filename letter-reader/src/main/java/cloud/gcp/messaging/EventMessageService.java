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
import java.util.stream.Collectors;

@Service
@Slf4j
public class EventMessageService {

    private int timeToReadSec = 30;

    private static final Gson gson = new Gson().newBuilder().setPrettyPrinting().create();

    private final String projectId;
    private GCPublisher pubsubReadLetterPublisher;
    private GCPublisher pubsubPostboxPublisher;// used for recovery only
    private GCSubscriber pubsubPostboxSubscriber;

    @Getter
    private ServiceState state;

    public EventMessageService(ServiceState state, Environment env) throws IOException {
        projectId = env.getRequiredProperty("GOOGLE_CLOUD_PROJECT");
        pubsubReadLetterPublisher = new GCPublisher(projectId, "read_letters");
        pubsubPostboxPublisher = new GCPublisher(projectId, "postbox");// used for recovery only
        pubsubPostboxSubscriber = new GCSubscriber(projectId, "postbox", onMessage());
        pubsubPostboxSubscriber.start();
        this.state = state;
        this.state.setStatus(ServiceState.ServiceStatus.WORKING);
        log.info("creating an EventMessageService for project = {}", projectId);
        log.info("timeToReadSec= {}", this.timeToReadSec);

    }

    private Consumer<PubsubMessage> onMessage() {
        return message -> {
            Map<String, String> msg = gson.fromJson(
                    message.getData().toStringUtf8(),
                    new TypeToken<Map<String, String>>() {
                    }.getType());
            log.info("Message: {} ", msg);
            log.info("Updating state");
            state.setMessage(message);
            log.info("Start reading");
            try {
                Thread.sleep(timeToReadSec * 1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                state.removeMessage(message);
                return;
            }
            log.info("Finish reading");
            this.pubsubReadLetterPublisher.publish(message.getData().toStringUtf8());
            state.removeMessage(message);
        };
    }

    public void close() {
        this.state.setStatus(ServiceState.ServiceStatus.STOPPING);
        pubsubReadLetterPublisher.close();
        pubsubPostboxSubscriber.close();
        if (state.hasUnfinishedWork()){
            log.warn("Re-publish unfinished work");
            pubsubPostboxPublisher.publish(state.getMessageMap().values().stream().map(m -> m.getData().toStringUtf8()).collect(Collectors.toList()));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
            pubsubPostboxPublisher.close();
        }
    }

}
