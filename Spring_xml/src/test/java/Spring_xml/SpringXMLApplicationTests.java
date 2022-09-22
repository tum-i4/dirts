package Spring_xml;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.Assert.assertEquals;


@RunWith(SpringRunner.class) // needed for JUnit4 only
public class SpringXMLApplicationTests {

    private static SimpleInterface simpleInterface;

    @BeforeClass
    public static void init() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("beans.xml");
        simpleInterface = applicationContext.getBean(SimpleInterface.class);
    }

    /**
     * Fails when the injected implementation is switched out in beans.xml
     */
    @Test
    public void shouldReturnCorrectName() {
        assertEquals("Foo", simpleInterface.getName());
    }
}
