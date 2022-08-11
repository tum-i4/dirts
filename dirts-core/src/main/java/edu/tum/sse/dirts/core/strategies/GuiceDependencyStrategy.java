package edu.tum.sse.dirts.core.strategies;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.di.Bean;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.analysis.di.InjectionPointCollector;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.guice.analysis.GuiceInjectionPointCollectorVisitor;
import edu.tum.sse.dirts.guice.analysis.GuiceMapper;
import edu.tum.sse.dirts.guice.analysis.identifiers.*;
import edu.tum.sse.dirts.guice.util.GuiceBinding;

import java.util.Collection;
import java.util.Set;

/**
 * Contains tasks required by the dependency-analyzing extension for Guice
 */
public class GuiceDependencyStrategy<T extends BodyDeclaration<?>>
        extends DIDependencyStrategy<T, GuiceBinding> {

    private static final String PREFIX = "guice";

    public GuiceDependencyStrategy(GuiceInjectionPointCollectorVisitor<T> injectionPointCollector,
                                   GuiceMapper<T> nameMapper) {
        super(PREFIX, injectionPointCollector, EdgeType.DI_GUICE, nameMapper);
    }

    @Override
    protected BeanStorage<GuiceBinding> collectBeans(Collection<TypeDeclaration<?>> ts) {

        BeanStorage<GuiceBinding> bindingsStorage = new BeanStorage<>();

        ProvidesIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        BindToIdentifier.identifyDependencies(ts, bindingsStorage);
        AutoBindSingletonIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ProviderIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        JustInTimeIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ImplementedByIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ProvidedByIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        return bindingsStorage;
    }
}
