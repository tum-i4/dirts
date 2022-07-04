package edu.tum.sse.dirts.core.strategies;

import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.Set;

public class GuiceDependencyStrategy extends RecomputeAllDependencyStrategy {
    public GuiceDependencyStrategy(Set<DependencyCollector> dependencyCollector) {
        super(dependencyCollector, Set.of(EdgeType.DI_GUICE));
    }
}
