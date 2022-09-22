package Spring_autoconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimpleMainBeanConfig {
    @Bean
    SimpleInterface getMainBean() {
        return new SimpleMainBean();
    }

}
