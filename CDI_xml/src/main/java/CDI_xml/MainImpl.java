package CDI_xml;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

@Alternative
public class MainImpl implements SimpleInterface {

    @Override
    public String getName() {
        return "Foo";
    }
}
