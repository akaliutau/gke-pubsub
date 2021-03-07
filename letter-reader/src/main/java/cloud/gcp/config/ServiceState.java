package cloud.gcp.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Service;

@ToString
@Service
public class ServiceState {
    public enum ServiceStatus {STARTING, WORKING, STOPPING, STOPPED}

    @Getter
    private ServiceStatus status = ServiceStatus.STARTING;

    @Getter
    @Setter
    private String message;

    synchronized public void setStatus(ServiceStatus status) {
        this.status = status;
    }

}
