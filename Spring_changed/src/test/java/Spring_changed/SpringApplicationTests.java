package Spring_changed;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.Assert.assertEquals;


@RunWith(SpringRunner.class) // needed for JUnit4 only
@SpringBootTest
public class SpringApplicationTests {

    @Autowired
    private SimpleInterface simpleInterface;

    /**
     * Fails when the returned value is changed to "Unexpected" in SimpleBean
     */
    @Test
    public void shouldReturnCorrectName() {
        assertEquals("Foo", simpleInterface.getName());
    }
}
