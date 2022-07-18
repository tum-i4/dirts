package edu.tum.sse.dirts.core.knowledgesources.graph_cropper;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static edu.tum.sse.dirts.core.BlackboardState.NEW_GRAPH_SET;

/**
 * Prepares the graph before analyzing dependencies
 * Calculates which dependencies need to be reanalyzed
 * <p>
 * The concept for updating the graph used in this class can be found in
 * J. Öqvist et al., Extraction-Based Regression Test Selection, 2016,
 *
 * It is implemented in
 *  - TypeLevelGraphCropper for types (by Öqvist et al.)
 *  - NonTypeLevelGraphCropper for nontypes (extended and adapted to nontypes)
 */
public abstract class AbstractGraphCropper<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    protected final FinderVisitor<Map<String, Node>, T> finderVisitor;
    protected final Set<EdgeType> affectedEdges;
    protected final Predicate<Node> nodesFilter;

    public AbstractGraphCropper(Blackboard<T> blackboard,
                                FinderVisitor<Map<String, Node>, T> finderVisitor,
                                Set<EdgeType> affectedEdges,
                                Predicate<Node> nodesFilter) {
        super(blackboard);
        this.finderVisitor = finderVisitor;
        this.affectedEdges = affectedEdges;
        this.nodesFilter = nodesFilter;
    }

    public BlackboardState updateBlackboard() {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        Map<String, String> nameMapperNodes = blackboard.getNameMapperNodes();

        Collection<CompilationUnit> compilationUnits = blackboard.getCompilationUnits();

        Map<String, Node> nodesAdded = blackboard.getNodesAdded();
        Map<String, Integer> nodesRemoved = blackboard.getNodesRemoved();
        Map<String, Node> nodesDifferent = blackboard.getNodesDifferent();
        Map<String, Node> nodesSame = blackboard.getNodesSame();

        // add new nodes
        nodesAdded.entrySet()
                .stream()
                .filter(e -> nodesFilter.test(e.getValue()))
                .map(Map.Entry::getKey)
                .forEach(dependencyGraph::addNode);

        // remove removed nodes
        nodesRemoved.keySet().forEach(dependencyGraph::removeNode);

        // rename nodes that have been renamed
        nameMapperNodes.forEach(dependencyGraph::renameNode);


        Collection<TypeDeclaration<?>> impactedTypes = calculateImpactedTypeDeclarations(
                dependencyGraph,
                nameMapperNodes,
                compilationUnits,
                nodesAdded,
                nodesRemoved,
                nodesDifferent,
                nodesSame
        );

        // remove all edges from nodes resulting from these compilationUnits, those will be recalculated
        Map<String, Node> nodeMap = new HashMap<>();
        impactedTypes.forEach(cu -> cu.accept(finderVisitor, nodeMap));
        nodesRemoved.forEach((removed, node) -> nodeMap.put(removed, null));
        nodeMap.keySet().forEach(from -> dependencyGraph.removeAllEdgesFrom(from, affectedEdges));


        blackboard.setImpactedTypes(impactedTypes);

        for (DependencyStrategy<T> dependencyStrategy : blackboard.getDependencyStrategies()) {
            dependencyStrategy.doGraphCropping(blackboard);
        }

        return NEW_GRAPH_SET;
    }

    protected abstract Collection<TypeDeclaration<?>> calculateImpactedTypeDeclarations(DependencyGraph dependencyGraph,
                                                                              Map<String, String> nameMapperNodes,
                                                                              Collection<CompilationUnit> compilationUnits,
                                                                              Map<String, Node> nodesAdded,
                                                                              Map<String, Integer> nodesRemoved,
                                                                              Map<String, Node> nodesDifferent,
                                                                              Map<String, Node> nodesSame);


}
