package CDI_xml;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class CdiXmlApplicationTests {

    private static WeldContainer container;

    @BeforeClass
    public static void init() {
        container = new Weld().initialize();
    }

    /**
     * Fails when the injected implementation is switched out in beans.xml
     */
    @Test
    public void test_name() {
        SimpleInterface simpleInterface = container.select(SimpleInterface.class).get();
        assertEquals("Foo", simpleInterface.getName());
    }
}
