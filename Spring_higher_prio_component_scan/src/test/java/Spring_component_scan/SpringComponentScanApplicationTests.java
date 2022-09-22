package Spring_component_scan;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class) // needed for JUnit4 only
@SpringBootTest
public class SpringComponentScanApplicationTests {

    private SimpleInterface simpleInterface;

    @Before
    public void init() {
        ApplicationContext context = new AnnotationConfigApplicationContext(ComponentScanConfiguration.class);
        simpleInterface = context.getBean(SimpleInterface.class);
    }

    /**
     * Fails when a new Component implementing SimpleInterface, annotated with @Primary is added
     */
    @Test
    public void shouldReturnCorrectName() {
        assertEquals("Foo", simpleInterface.getName());
    }

}
