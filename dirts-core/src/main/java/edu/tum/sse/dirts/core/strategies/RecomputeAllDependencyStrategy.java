package edu.tum.sse.dirts.core.strategies;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.def.finders.TypeFinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contains tasks required by a non-caching strategy for calculating dependencies
 * Recomputes all relevant dependencies
 */
public class RecomputeAllDependencyStrategy<T extends BodyDeclaration<?>> implements DependencyStrategy<T> {

    private final Set<DependencyCollector<T>> dependencyCollector;
    private final Set<EdgeType> affectedEdges;

    public RecomputeAllDependencyStrategy(Set<DependencyCollector<T>> dependencyCollector,
                                          Set<EdgeType> affectedEdges) {
        this.dependencyCollector = dependencyCollector;
        this.affectedEdges = affectedEdges;
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
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();
        dependencyGraph.removeAllEdgesByType(affectedEdges);
    }

    @Override
    public void doDependencyAnalysis(Blackboard<T> blackboard) {
        List<TypeDeclaration<?>> typeDeclarations = new ArrayList<>();
        TypeFinderVisitor typeFinderVisitor = new TypeFinderVisitor();
        blackboard.getCompilationUnits().forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));
        dependencyCollector.forEach(d -> d.calculateDependencies(typeDeclarations, blackboard.getDependencyGraphNewRevision()));
    }

    @Override
    public void combineGraphs(Blackboard<T> blackboard) {

    }
}
