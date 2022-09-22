package Spring_changed;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimpleBeanConfig {
    @Bean
    SimpleInterface getBean() {
        return new SimpleBean();
    }

}
