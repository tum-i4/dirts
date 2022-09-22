package Spring_autoconfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.Assert.assertEquals;


@RunWith(SpringRunner.class) // needed for JUnit4 only
@SpringBootTest
public class SpringAutoconfigApplicationTests {

    @Autowired
    private SimpleInterface simpleInterface;

    /**
     * Fails when a new Bean implementing SimpleInterface, annotated with @Primary is added
     */
    @Test
    public void shouldReturnCorrectName() {
        assertEquals("Foo", simpleInterface.getName());
    }
}
