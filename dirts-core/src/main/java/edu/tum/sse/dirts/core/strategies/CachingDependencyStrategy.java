package edu.tum.sse.dirts.core.strategies;

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
public class CachingDependencyStrategy implements DependencyStrategy {

    private final Set<DependencyCollector> dependencyCollector;
    private final Set<EdgeType> affectedEdges;

    public CachingDependencyStrategy(Set<DependencyCollector> dependencyCollector,
                                     Set<EdgeType> affectedEdges) {
        this.dependencyCollector = dependencyCollector;
        this.affectedEdges = affectedEdges;
    }


    @Override
    public void doImport(Path tmpPath, Blackboard blackboard, String suffix) {
    }

    @Override
    public void doExport(Path tmpPath, Blackboard blackboard, String suffix) {
    }

    @Override
    public void doChangeAnalysis(Blackboard blackboard) {
    }

    @Override
    public void doGraphCropping(Blackboard blackboard) {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        // remove all edges from changed nodes
        blackboard.getNodesDifferent().keySet()
                .forEach(from -> dependencyGraph.removeAllEdgesFrom(from, affectedEdges));
    }

    @Override
    public void doDependencyAnalysis(Blackboard blackboard) {
        Collection<TypeDeclaration<?>> impactedTypes = blackboard.getImpactedTypes();
        dependencyCollector.forEach(d -> d.calculateDependencies(
                impactedTypes,
                blackboard.getDependencyGraphNewRevision()));
    }

    @Override
    public void combineGraphs(Blackboard blackboard) {

    }
}
