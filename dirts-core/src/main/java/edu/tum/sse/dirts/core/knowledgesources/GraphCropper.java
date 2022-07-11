package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import edu.tum.sse.dirts.analysis.FinderVisitor;
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
import java.util.stream.Stream;

import static edu.tum.sse.dirts.core.BlackboardState.NEW_GRAPH_SET;
import static edu.tum.sse.dirts.core.BlackboardState.NODES_CHANGES_SET;

/**
 * Prepares the graph before analyzing dependencies
 *
 * @param <T>
 */
public class GraphCropper<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    private final Blackboard<T> blackboard;
    private final FinderVisitor<Map<String, Node>, T> finderVisitor;
    private final Set<EdgeType> affectedEdges;
    private final Predicate<Node> nodesFilter;

    private final boolean considerIngoingDependencies;

    public GraphCropper(Blackboard<T> blackboard,
                        FinderVisitor<Map<String, Node>, T> finderVisitor,
                        Set<EdgeType> affectedEdges,
                        Predicate<Node> nodesFilter,
                        boolean considerIngoingDependencies) {
        super(blackboard);
        this.blackboard = blackboard;
        this.finderVisitor = finderVisitor;
        this.affectedEdges = affectedEdges;
        this.nodesFilter = nodesFilter;
        this.considerIngoingDependencies = considerIngoingDependencies;
    }

    @Override
    public BlackboardState updateBlackboard() {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        Map<String, String> nameMapperNodes = blackboard.getNameMapperNodes();
        Map<String, String> compilationUnitMapping = blackboard.getCompilationUnitMapping();

        Collection<CompilationUnit> compilationUnits = blackboard.getCompilationUnits();

        Map<String, Node> allNodes = blackboard.getAllNodes();
        Map<String, Node> nodesAdded = blackboard.getNodesAdded();
        Map<String, Integer> nodesRemoved = blackboard.getNodesRemoved();
        Map<String, Node> nodesDifferent = blackboard.getNodesDifferent();
        Map<String, Node> nodesSame = blackboard.getNodesSame();

        // rename nodes that have been renamed
        nameMapperNodes.forEach(dependencyGraph::renameNode);

        // add new nodes
        nodesAdded.entrySet()
                .stream()
                .filter(e -> nodesFilter.test(e.getValue()))
                .map(Map.Entry::getKey)
                .forEach(dependencyGraph::addNode);

        // Compute the set of CompilationUnits that require a recalculation of dependencies
        HashSet<CompilationUnit> impactedCompilationUnits = new HashSet<>();

        // In nontypeL it may be that removed or renamed nodes are pointed to by other nodes that need not change
        // However, the dependencies of these nodes may change and need to be recalculated
        if (considerIngoingDependencies) {
            Stream.concat(nodesRemoved.keySet().stream(), nameMapperNodes.values().stream())
                    .forEach(name -> {
                        Set<String> affectedNodesNames = dependencyGraph.removeAllEdgesTo(name, affectedEdges);
                        Set<Node> affectedNodes = allNodes.entrySet().stream()
                                .filter(e -> affectedNodesNames.contains(e.getKey()))
                                .map(Map.Entry::getValue)
                                .collect(Collectors.toSet());
                        impactedCompilationUnits.addAll(affectedNodes.stream()
                                .map(Node::findCompilationUnit)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet()));
                    });
        }

        // remove removed nodes
        nodesRemoved.keySet().forEach(dependencyGraph::removeNode);

        // Add all CompilationUnits that contained removed nodes (if they are not removed entirely)
        impactedCompilationUnits.addAll(nodesRemoved.keySet()
                .stream().map(compilationUnitMapping::get)
                .map(allNodes::get)
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        // Add all CompilationUnits from modified nodes
        impactedCompilationUnits.addAll(nodesDifferent.values().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        // Add all CompilationUnits from packages that have at least one added or renamed CompilationUnit
        // theory from Öqvist et al.
        {
            // Add all added nodes that correspond to a CompilationUnit
            Set<String> affectedPackagesNodes = new HashSet<>();
            affectedPackagesNodes.addAll(nodesAdded.values().stream()
                    .filter(n -> n instanceof CompilationUnit)
                    .map(n -> ((CompilationUnit) n).getPackageDeclaration())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(NodeWithName::getNameAsString)
                    .collect(Collectors.toSet()));

            // Add all nodes that have been renamed and correspond to a CompilationUnit
            affectedPackagesNodes.addAll(nodesSame.entrySet().stream()
                    .filter(n -> n.getValue() instanceof CompilationUnit)
                    .filter(n -> nameMapperNodes.containsValue(n.getKey()))
                    .map(n -> ((CompilationUnit) n.getValue()).getPackageDeclaration())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(NodeWithName::getNameAsString)
                    .collect(Collectors.toSet()));

            impactedCompilationUnits.addAll(compilationUnits.stream()
                    .filter(cu -> cu.getPackageDeclaration()
                            .filter(declaration -> affectedPackagesNodes.contains(declaration.getNameAsString()))
                            .isPresent())
                    .collect(Collectors.toSet()));
        }

        // remove all edges from nodes resulting from these compilationUnits, those will be recalculated
        Map<String, Node> nodeMap = new HashMap<>();
        impactedCompilationUnits.forEach(cu -> cu.accept(finderVisitor, nodeMap));
        nodesRemoved.forEach((removed, checksum) -> nodeMap.put(removed, null));
        nodeMap.keySet().forEach(from -> {
            dependencyGraph.removeAllEdgesFrom(from, affectedEdges);
        });

        // Add all CompilationUnits from added nodes (these nodes cannot be present and need not be considered before)
        impactedCompilationUnits.addAll(nodesAdded.values().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        List<TypeDeclaration<?>> typeDeclarations = new ArrayList<>();
        TypeFinderVisitor typeFinderVisitor = new TypeFinderVisitor();
        impactedCompilationUnits.forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));

        blackboard.setImpactedTypes(typeDeclarations);

        for (DependencyStrategy<T> dependencyStrategy : blackboard.getDependencyStrategies()) {
            dependencyStrategy.doGraphCropping(blackboard);
        }

        return NEW_GRAPH_SET;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == NODES_CHANGES_SET;
    }
}
