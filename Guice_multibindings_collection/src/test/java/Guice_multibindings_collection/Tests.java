package Guice_multibindings_collection;

import com.google.inject.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;


public class Tests {

    private static Injector injector;

    @BeforeClass
    public static void init() {
        injector = Guice.createInjector(new MultibindingsTestModule());
    }

    /**
     * Fails when an additional implementation is added in MultibindingsTestModule,
     * in a method that delegates to the constructor and is annotated with @ProvidesIntoSet
     */
    @Test
    public void should_contain_one_element() {
        Set<SimpleInterface> simpleInterfaces = injector.getInstance(Key.get(new TypeLiteral<Set<SimpleInterface>>() {}));
        assertEquals(1, simpleInterfaces.size());
    }

}
