package CDI_higher_prio;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

@Alternative
@Priority(1)
public class MainImpl implements SimpleInterface {
    @Override
    public String getName() {
        return "Foo";
    }
}
