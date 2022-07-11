package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.ModificationGraph;

import java.util.Map;

import static edu.tum.sse.dirts.core.BlackboardState.DEPENDENCIES_UPDATED;
import static edu.tum.sse.dirts.core.BlackboardState.READY_TO_CALCULATE_AFFECTED_TESTS;

/**
 * Combines the graphs of the old and the new revision into a single modificationGraph
 */
public class GraphCombiner<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    public GraphCombiner(Blackboard<T> blackboard) {
        super(blackboard);
    }

    @Override
    public BlackboardState updateBlackboard() {
        DependencyGraph dependencyGraphOldRevision = blackboard.getDependencyGraphOldRevision();
        DependencyGraph dependencyGraphNewRevision = blackboard.getDependencyGraphNewRevision();
        Map<String, String> nameMapper = blackboard.getNameMapperNodes();

        ModificationGraph modificationGraph = new ModificationGraph(
                dependencyGraphOldRevision,
                dependencyGraphNewRevision,
                nameMapper);

        modificationGraph.setModificationByStatus(
                blackboard.getNodesSame().keySet(),
                blackboard.getNodesDifferent().keySet(),
                blackboard.getNodesAdded().keySet(),
                blackboard.getNodesRemoved().keySet()
        );

        blackboard.setCombinedGraph(modificationGraph);

        for (DependencyStrategy<T> dependencyStrategy : blackboard.getDependencyStrategies()) {
            dependencyStrategy.combineGraphs(blackboard);
        }

        modificationGraph.setModificationByDependencies();

        return READY_TO_CALCULATE_AFFECTED_TESTS;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == DEPENDENCIES_UPDATED;
    }
}
