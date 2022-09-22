package CDI_higher_prio;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class CDIApplicationTests {

    private static WeldContainer container;

    @BeforeClass
    public static void init() {
        container = new Weld().initialize();
    }

    /**
     * Fails when an @Alternative implementation is provided with higher @Priority(int)
     */
    @Test
    public void test_name() {
        SimpleInterface simpleInterface = container.select(SimpleInterface.class).get();
        assertEquals("Foo", simpleInterface.getName());
    }
}
