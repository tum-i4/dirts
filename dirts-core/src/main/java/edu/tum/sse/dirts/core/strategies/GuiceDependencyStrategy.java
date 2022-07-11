package edu.tum.sse.dirts.core.strategies;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.Set;

public class GuiceDependencyStrategy<T extends BodyDeclaration<?>> extends RecomputeAllDependencyStrategy<T> {
    public GuiceDependencyStrategy(Set<DependencyCollector<T>> dependencyCollector) {
        super(dependencyCollector, Set.of(EdgeType.DI_GUICE));
    }
}
