package cloud.gcp.config;

import lombok.Getter;

public class ShutdownHandler {
    public enum ServiceStatus {STARTING, WORKING, STOPPING, STOPPED}

    @Getter
    private ServiceStatus status;

    synchronized public void setStatus(ServiceStatus status) {
        this.status = status;
    }

}
