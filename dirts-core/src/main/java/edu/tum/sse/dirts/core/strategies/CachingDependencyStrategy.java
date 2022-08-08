package edu.tum.sse.dirts.core.strategies;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * Contains tasks required by a caching strategy for calculating dependencies
 * Incrementally updates the graph of the old revision based on a concept by
 * (J. Öqvist, G. Hedin, and B. Magnusson, “Extraction-based regression test selection,”
 * ACM Int. Conf. Proceeding Ser., vol. Part F1284, 2016, doi: 10.1145/2972206.2972224)
 */
public class CachingDependencyStrategy<T extends BodyDeclaration<?>> implements DependencyStrategy<T> {

    private final Set<DependencyCollector<T>> dependencyCollector;

    public CachingDependencyStrategy(Set<DependencyCollector<T>> dependencyCollector) {
        this.dependencyCollector = dependencyCollector;
    }


    @Override
    public void doImport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
    }

    @Override
    public void doExport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
    }

    @Override
    public void doChangeAnalysis(Blackboard<T> blackboard) {
    }

    @Override
    public void doGraphCropping(Blackboard<T> blackboard) {
        // Done in the individual DependencyCollectors
    }

    @Override
    public void doDependencyAnalysis(Blackboard<T> blackboard) {
        Collection<TypeDeclaration<?>> impactedTypes = blackboard.getImpactedTypes();
        dependencyCollector.forEach(d -> d.calculateDependencies(
                impactedTypes,
                blackboard.getDependencyGraphNewRevision()));
    }

    @Override
    public void combineGraphs(Blackboard<T> blackboard) {

    }
}
