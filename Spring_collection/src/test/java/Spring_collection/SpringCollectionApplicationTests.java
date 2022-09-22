package Spring_collection;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class) // needed for JUnit4 only
@SpringBootTest
public class SpringCollectionApplicationTests {

    @Autowired
    private List<NamedBean> beanList;

    /**
     * Fails wehen an additional Implementation is provided in a new @Configuration
     */
    @Test
    public void list_should_contain_3_elements() {
        assertEquals(beanList.get(0).getName(), "Foo");
        assertEquals(beanList.get(1).getName(), "Bar");
        assertEquals(beanList.get(2).getName(), "Baz");
        assertEquals(3, beanList.size());
    }

    @Autowired
    private Set<NamedBean> beanSet;

    /**
     * Fails wehen an additional Implementation is provided in a new @Configuration
     */
    @Test
    public void set_should_contain_3_elements() {
        assertTrue(beanSet.containsAll(beanList));
        assertEquals(3, beanSet.size());
    }
}
