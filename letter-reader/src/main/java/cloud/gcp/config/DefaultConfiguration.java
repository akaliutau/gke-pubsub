package cloud.gcp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DefaultConfiguration {

    @Bean
    public ShutdownHandler shutdownHandler() {
        return new ShutdownHandler();
    }

}
