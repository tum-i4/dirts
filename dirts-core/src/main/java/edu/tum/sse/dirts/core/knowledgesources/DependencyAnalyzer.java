/*
 * Copyright 2022. The ttrace authors.
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

import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.util.naming_scheme.Names;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Collection;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.core.BlackboardState.NEW_GRAPH_SET;

/**
 * Analyzes the dependencies of code objects and adds corresponding edges in the DependencyGraph
 */
public class DependencyAnalyzer extends KnowledgeSource {

    private final DependencyCollector primaryDependencyCollector;

    //##################################################################################################################
    // Constructors

    public DependencyAnalyzer(Blackboard blackboard, DependencyCollector primaryDependencyCollector) {
        super(blackboard);
        this.primaryDependencyCollector = primaryDependencyCollector;
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {
        Collection<TypeDeclaration<?>> impactedTypes = blackboard.getImpactedTypes();

        primaryDependencyCollector.calculateDependencies(
                impactedTypes,
                blackboard.getDependencyGraphNewRevision());

        System.out.println("[DEBUG] Recalculated primary dependencies of "
                + impactedTypes.stream().map(Names::lookup).map(Pair::getFirst).collect(Collectors.toList()));

        for (DependencyStrategy dependencyStrategy : blackboard.getDependencyStrategies()) {
            dependencyStrategy.doDependencyAnalysis(blackboard);
        }
        return BlackboardState.DEPENDENCIES_UPDATED;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == NEW_GRAPH_SET;
    }
}
