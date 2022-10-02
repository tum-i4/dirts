package Guice_autobindsingleton_collection;

import com.netflix.governator.annotations.AutoBindSingleton;

import javax.inject.Singleton;

@Singleton
// TODO: uncomment this
// @AutoBindSingleton(multiple = true, baseClass = SimpleInterface.class)
public class AdditionalImpl implements SimpleInterface {
    @Override
    public String getName() {
        return "Unexpected";
    }
}
