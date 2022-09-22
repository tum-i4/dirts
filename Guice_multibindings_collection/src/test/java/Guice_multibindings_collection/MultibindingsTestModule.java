package Guice_multibindings_collection;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.ProvidesIntoMap;
import com.google.inject.multibindings.ProvidesIntoSet;

public class MultibindingsTestModule extends AbstractModule {

    /* Other possibility to declare the exact same
    @Override
    public void configure() {
        Multibinder<SimpleInterface> interfaceMultibinder = Multibinder.newSetBinder(binder(), SimpleInterface.class);
        interfaceMultibinder.addBinding().to(MainImpl.class);
        //interfaceMultibinder.addBinding().to(AdditionalImpl.class);   // TODO: uncommented
    }*/

    @ProvidesIntoSet
    SimpleInterface provideMainImpl() {
        return new MainImpl();
    }

    // TODO: uncomment this
    /*@ProvidesIntoSet
    SimpleInterface provideAdditionalImpl() {
        return new AdditionalImpl();
    }*/
}
