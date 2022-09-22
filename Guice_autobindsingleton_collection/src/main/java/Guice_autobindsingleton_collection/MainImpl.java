package Guice_autobindsingleton_collection;

import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.AutoBindSingleton;

import javax.inject.Singleton;

@Singleton
@AutoBindSingleton(multiple = true, baseClass = SimpleInterface.class)
public class MainImpl implements SimpleInterface {

    @Override
    public String getName() {
        return "Foo";
    }
}
