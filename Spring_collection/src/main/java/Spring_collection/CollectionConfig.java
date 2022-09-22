package Spring_collection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CollectionConfig {

    @Bean
    public NamedBean getFoo() {
        return new NamedBean("Foo");
    }

    @Bean
    public NamedBean getBar() {
        return new NamedBean("Bar");
    }

    @Bean
    public NamedBean getBaz() {
        return new NamedBean("Baz");
    }
}
