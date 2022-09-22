package Guice_autobindsingleton_collection;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.netflix.governator.guice.LifecycleInjector;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

import static junit.framework.Assert.assertEquals;


public class Tests {

    private static Injector injector;

    @BeforeClass
    public static void init() {
        injector = LifecycleInjector
                .builder()
                .usingBasePackages("")
                .build()
                .createInjector();
    }

    /**
     * Fails when an additional implementation is added,
     * annotated with @AutoBindSingleton(multiple = true, baseClass = SimpleInterface.class)
     */
    @Test
    public void should_contain_one_element() {
        Set<SimpleInterface> simpleInterfaces = injector.getInstance(Key.get(new TypeLiteral<Set<SimpleInterface>>() {}));
        assertEquals(1, simpleInterfaces.size());
    }

}
