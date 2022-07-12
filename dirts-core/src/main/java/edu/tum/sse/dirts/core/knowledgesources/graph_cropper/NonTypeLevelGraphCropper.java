package edu.tum.sse.dirts.core.knowledgesources.graph_cropper;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.tum.sse.dirts.core.BlackboardState.NODES_CHANGES_SET;

/**
 * Prepares the graph before analyzing dependencies
 * Calculates which dependencies need to be reanalyzed
 * <p>
 * The concept used in this class can be found in J. Öqvist et al., Extraction-Based Regression Test Selection, 2016,
 * but has been adapted and extended towards nontypes
 */
public class NonTypeLevelGraphCropper extends AbstractGraphCropper<BodyDeclaration<?>> {

    public NonTypeLevelGraphCropper(Blackboard<BodyDeclaration<?>> blackboard,
                                    FinderVisitor<Map<String, Node>, BodyDeclaration<?>> finderVisitor,
                                    Set<EdgeType> affectedEdges,
                                    Predicate<Node> nodesFilter) {
        super(blackboard, finderVisitor, affectedEdges, nodesFilter);
    }

    @Override
    public Set<CompilationUnit> calculateImpactedCompilationUnits(
            DependencyGraph dependencyGraph,
            Map<String, String> nameMapperNodes,
            Collection<CompilationUnit> compilationUnits,
            Map<String, Node> nodesAdded,
            Map<String, Integer> nodesRemoved,
            Map<String, Node> nodesDifferent,
            Map<String, Node> nodesSame
    ) {

        Map<String, Node> allNodes = blackboard.getAllNodes();
        Map<String, String> compilationUnitMapping = blackboard.getCompilationUnitMapping();

        // Compute the set of CompilationUnits that require a recalculation of dependencies
        HashSet<CompilationUnit> impactedCompilationUnits = new HashSet<>();

        // In nontypeL it may be that removed or renamed nodes are pointed to by other nodes that need not change
        // However, the dependencies of these nodes may change and need to be recalculated
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

        // Add all CompilationUnits that contained removed nodes (if they are not removed entirely)
        impactedCompilationUnits.addAll(nodesRemoved.keySet()
                .stream().map(compilationUnitMapping::get)
                .filter(Objects::nonNull)
                .map(allNodes::get)
                .filter(Objects::nonNull)
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

        // Add all CompilationUnits from added nodes
        impactedCompilationUnits.addAll(nodesAdded.values().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        // Add all CompilationUnits from packages that have at least one added or renamed CompilationUnit
        // theory by Öqvist et al.
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

        return impactedCompilationUnits;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == NODES_CHANGES_SET;
    }
}
