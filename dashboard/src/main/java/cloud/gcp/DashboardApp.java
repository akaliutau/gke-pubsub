package cloud.gcp;

import cloud.gcp.service.StatisticsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        StatisticsService.class
})
public class DashboardApp {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(DashboardApp.class, args);
    }

}
