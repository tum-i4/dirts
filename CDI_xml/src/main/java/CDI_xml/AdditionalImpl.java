package CDI_xml;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

@Alternative
public class AdditionalImpl implements SimpleInterface {

    @Override
    public String getName() {
        return "Unexpected";
    }
}
