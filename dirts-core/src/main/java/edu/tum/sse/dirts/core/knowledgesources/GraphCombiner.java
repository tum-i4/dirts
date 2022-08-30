/*
 * Copyright 2022. The dirts authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.ModificationGraph;
import edu.tum.sse.dirts.graph.ModificationType;
import edu.tum.sse.dirts.util.DirtsUtil;
import edu.tum.sse.dirts.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static edu.tum.sse.dirts.core.BlackboardState.DEPENDENCIES_UPDATED;
import static edu.tum.sse.dirts.core.BlackboardState.READY_TO_CALCULATE_AFFECTED_TESTS;
import static java.util.logging.Level.WARNING;

/**
 * Combines the graphs of the old and the new revision into a single ModificationGraph
 */
public class GraphCombiner<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    public GraphCombiner(Blackboard<T> blackboard) {
        super(blackboard);
    }

    @Override
    public BlackboardState updateBlackboard() {
        DependencyGraph dependencyGraphOldRevision = blackboard.getDependencyGraphOldRevision();
        DependencyGraph dependencyGraphNewRevision = blackboard.getDependencyGraphNewRevision();

        ModificationGraph modificationGraph = new ModificationGraph(
                dependencyGraphOldRevision,
                dependencyGraphNewRevision);


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

    private void importChangesFromOtherModules(ModificationGraph graph) {
        Path changedNodesPath = DirtsUtil.getChangedNodesPath(blackboard.getRootPath());

        if (Files.exists(changedNodesPath)) {
            try {
                Set<String> changedNodes = Set.of(Files.readString(changedNodesPath).strip().split(", "));

                for (String changedNode : changedNodes) {
                    graph.setModificationTypeIfPresent(changedNode, ModificationType.EXTERNALLY_MODIFIED);
                }

            } catch (IOException e) {
                Log.errLog(WARNING, "Failed to read file containing changed nodes");
            }
        }

    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == DEPENDENCIES_UPDATED;
    }
}
