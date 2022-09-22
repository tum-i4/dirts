package Spring_component_scan;

import org.springframework.stereotype.Component;

@Component
public class SimpleMainComponent implements SimpleInterface {
    @Override
    public String getName() {
        return "Foo";
    }
}
