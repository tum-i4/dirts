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
package edu.tum.sse.dirts.core.control;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.analysis.def.DefaultDependencyCollectorVisitor;
import edu.tum.sse.dirts.analysis.def.checksum.ChecksumVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.knowledgesources.*;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;

import java.util.*;
import java.util.function.Predicate;

import static java.util.logging.Level.INFO;

/**
 * Abstract base class for either type-level or nontype-level RTS
 */
public abstract class Control<T extends BodyDeclaration<?>> {

    //##################################################################################################################
    // Attributes

    protected final boolean overwrite;

    /**
     * Central class of the blackboard pattern
     */
    protected final Blackboard<T> blackboard;

    /**
     * Suffix for distinguishing cache directories of different levels of RTS
     */
    private final String suffix;

    /**
     * Visitor used to calculate checksums of the most important nodes
     */
    private final ChecksumVisitor<T> checksumVisitor;

    /**
     * Visitor used to find the names of the most important nodes
     */
    protected final FinderVisitor<Map<String, Node>, T> nameFinderVisitor;

    /**
     * Visitor used to find tests
     */
    private final FinderVisitor<Collection<String>, T> testFinderVisitor;


    /**
     * Filter used to restrict the nodes that are added to the graph
     */
    protected final Predicate<Node> nodesInGraphFilter;

    /**
     * Dependency collector that creates the most important edges
     * (Extensions can be added using DependencyStrategies)
     */
    private final DefaultDependencyCollectorVisitor<T> primaryDependencyCollector;

    /**
     * Most important edges
     */
    protected final Set<EdgeType> affectedEdges;

    //##################################################################################################################
    // Constructors

    public Control(Blackboard<T> blackboard,
                   boolean overwrite,
                   String suffix,
                   ChecksumVisitor<T> checksumVisitor,
                   FinderVisitor<Map<String, Node>, T> nameFinderVisitor,
                   FinderVisitor<Collection<String>, T> testFinderVisitor,
                   Predicate<Node> nodesInGraphFilter,
                   DefaultDependencyCollectorVisitor<T> primaryDependencyCollector,
                   Set<EdgeType> affectedEdges) {
        this.blackboard = blackboard;
        this.overwrite = overwrite;

        this.suffix = suffix;
        this.checksumVisitor = checksumVisitor;
        this.nameFinderVisitor = nameFinderVisitor;
        this.testFinderVisitor = testFinderVisitor;
        this.nodesInGraphFilter = nodesInGraphFilter;
        this.primaryDependencyCollector = primaryDependencyCollector;
        this.affectedEdges = affectedEdges;
    }

    //##################################################################################################################
    // Methods

    protected void init() {
        List<KnowledgeSource<T>> knowledgeSources = List.of(
                new ProjectImporter<>(blackboard, suffix),

                new TypeSolverInitializer<>(blackboard),

                new Parser<>(blackboard),

                new CodeChangeAnalyzer<>(blackboard,
                        nameFinderVisitor,
                        checksumVisitor),

                new TestFinder<>(blackboard,
                        testFinderVisitor),

                new DependencyAnalyzer<>(blackboard,
                        primaryDependencyCollector),

                new GraphCombiner<>(blackboard),

                new ProjectExporter<>(blackboard,
                        suffix,
                        checksumVisitor,
                        overwrite)
        );

        knowledgeSources.forEach(blackboard::addKnowledgeSource);
    }

    /**
     * Apply knowledgeSources as long as possible
     */
    public void applyKnowledgeSources() {
        init();

        while (!blackboard.getState().isTerminalState()) {

            // query candidate ready to run
            KnowledgeSource<T> candidate = blackboard.getKnowledgeSources().stream()
                    .filter(KnowledgeSource::executeCondition)
                    .findFirst().orElse(null);

            if (candidate != null) {
                // run candidate
                long unitTime = System.currentTimeMillis();

                BlackboardState newState = candidate.updateBlackboard();
                blackboard.setState(newState);

                Log.log(INFO, "TIME",
                        String.format(Locale.US,
                                "%s took %.3f seconds",
                                newState,
                                ((System.currentTimeMillis() - unitTime) * 0.001)));

            } else {
                throw new RuntimeException("Terminated unexpectedly at state " + blackboard.getState().name());
            }
        }

        // check if failed
        if (blackboard.getState().isFailedState()) {
            throw new RuntimeException("Failed to compute affected tests.");
        }
    }

    /**
     * Calculates affected tests
     *
     * @param filterByEdgeType
     * @return (changedNode, affectedTest)
     */
    public Map<String, Set<String>> getSelectedTests(Set<EdgeType> filterByEdgeType) {
        long startTime = System.currentTimeMillis();

        if (!blackboard.getState().isTerminalState())
            applyKnowledgeSources();

        if (blackboard.getState().isDoneState()) {
            // calculated impacted nodes
            Map<String, Set<String>> impactedNodes;
            if (!filterByEdgeType.isEmpty()) {
                impactedNodes = blackboard.getCombinedGraph().affectedByEdgeType(filterByEdgeType);
            } else {
                impactedNodes = blackboard.getCombinedGraph().affected();
            }

            Collection<String> tests = blackboard.getTests();

            // Check if tests are in set of impacted nodes
            impactedNodes.forEach((key, value) -> {
                value.retainAll(tests);
                tests.removeAll(value);
            });
            impactedNodes.put(null, new HashSet<>(tests));


            Log.log(INFO, "TIME",
                    String.format(Locale.US,
                            "Selecting tests altogether took %.3f seconds",
                            ((System.currentTimeMillis() - startTime) * 0.001)));

            return impactedNodes;
        } else {
            return null;
        }
    }

    /**
     * Visualizes the combined dependencyGraph
     *
     * @return
     */
    public String visualizeDependencyGraph() {
        if (!blackboard.getState().isTerminalState())
            applyKnowledgeSources();

        if (blackboard.getState().isDoneState())
            return blackboard.getCombinedGraph().toString();
        else
            return "Not able to visualize dependency graph";
    }
}
