package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import edu.tum.sse.dirts.analysis.def.finders.TypeFinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.core.BlackboardState.NEW_GRAPH_SET;
import static edu.tum.sse.dirts.core.BlackboardState.NODES_CHANGES_SET;

/**
 * Prepares the graph before analyzing dependencies
 * @param <T>
 */
public class GraphCropper<T extends BodyDeclaration<?>> extends KnowledgeSource {

    private final Blackboard blackboard;
    private final Set<EdgeType> affectedEdges;
    private final Predicate<Node> nodesFilter;

    public GraphCropper(Blackboard blackboard, Set<EdgeType> affectedEdges, Predicate<Node> nodesFilter) {
        super(blackboard);
        this.blackboard = blackboard;
        this.affectedEdges = affectedEdges;
        this.nodesFilter = nodesFilter;
    }

    @Override
    public BlackboardState updateBlackboard() {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        // remove removed nodes
        blackboard.getNodesRemoved().keySet().forEach(dependencyGraph::removeNode);

        // rename nodes that have been renamed
        blackboard.getNameMapperNodes().forEach(dependencyGraph::renameNode);

        // add new nodes
        blackboard.getNodesAdded().entrySet()
                .stream()
                .filter(e -> nodesFilter.test(e.getValue()))
                .map(Map.Entry::getKey)
                .forEach(dependencyGraph::addNode);

        // Compute the set of CompilationUnits that require a recalculation of dependencies
        HashSet<CompilationUnit> impactedCompilationUnits = new HashSet<>();

        // remove all edges from changed nodes
        blackboard.getNodesDifferent().keySet()
                .forEach(from -> dependencyGraph.removeAllEdgesFrom(from, affectedEdges));

        // Add all CompilationUnits from modified nodes
        impactedCompilationUnits.addAll(blackboard.getNodesImpacted().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));


        // Add all CompilationUnits from packages that have at least one added or renamed CompilationUnit
        // theory from Öqvist et al.

        // Add all added nodes that correspond to a CompilationUnit
        Set<String> affectedPackagesNodes = new HashSet<>();
        affectedPackagesNodes.addAll(blackboard.getNodesAdded().values().stream()
                .filter(n -> n instanceof CompilationUnit)
                .map(n -> ((CompilationUnit) n).getPackageDeclaration())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(NodeWithName::getNameAsString)
                .collect(Collectors.toSet()));

        // Add all nodes that have been renamed and correspond to a CompilationUnit
        affectedPackagesNodes.addAll(blackboard.getNodesSame().entrySet().stream()
                .filter(n -> n.getValue() instanceof CompilationUnit)
                .filter(n -> blackboard.getNameMapperNodes().containsValue(n.getKey()))
                .map(n -> ((CompilationUnit) n.getValue()).getPackageDeclaration())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(NodeWithName::getNameAsString)
                .collect(Collectors.toSet()));


        impactedCompilationUnits.addAll(blackboard.getAllNodes().values().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(cu -> cu.getPackageDeclaration()
                        .filter(declaration ->
                                affectedPackagesNodes.contains(declaration.getNameAsString())).isPresent())
                .collect(Collectors.toSet()));

        List<TypeDeclaration<?>> typeDeclarations = new ArrayList<>();
        TypeFinderVisitor typeFinderVisitor = new TypeFinderVisitor();
        impactedCompilationUnits.forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));

        blackboard.setImpactedTypes(typeDeclarations);

        for (DependencyStrategy dependencyStrategy : blackboard.getDependencyStrategies()) {
            dependencyStrategy.doGraphCropping(blackboard);
        }

        return NEW_GRAPH_SET;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == NODES_CHANGES_SET;
    }
}
