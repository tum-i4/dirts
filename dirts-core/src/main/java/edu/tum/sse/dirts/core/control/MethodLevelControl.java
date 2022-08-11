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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.def.DefaultMethodLevelDependencyCollectorVisitor;
import edu.tum.sse.dirts.analysis.def.JUnitMethodLevelDependencyCollectorVisitor;
import edu.tum.sse.dirts.analysis.def.checksum.MethodLevelChecksumVisitor;
import edu.tum.sse.dirts.analysis.def.finders.MethodLevelNameFinderVisitor;
import edu.tum.sse.dirts.analysis.def.finders.MethodLevelTestFinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.knowledgesources.graph_cropper.MethodLevelGraphCropper;
import edu.tum.sse.dirts.core.strategies.CachingDependencyStrategy;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.Set;
import java.util.function.Predicate;

import static edu.tum.sse.dirts.graph.EdgeType.*;

/**
 * Method level RTS
 */
public class MethodLevelControl extends Control<BodyDeclaration<?>> {

    //##################################################################################################################
    // Static constants

    private static final MethodLevelChecksumVisitor METHOD_LEVEL_CHECKSUM_VISITOR = new MethodLevelChecksumVisitor();
    private static final MethodLevelNameFinderVisitor METHOD_LEVEL_NAME_FINDER_VISITOR = new MethodLevelNameFinderVisitor();

    private static final Predicate<Node> METHOD_LEVEL_NODES_IN_GRAPH =
            n -> !(n instanceof CompilationUnit || n instanceof TypeDeclaration);

    private static final DefaultMethodLevelDependencyCollectorVisitor PRIMARY_DEPENDENCY_COLLECTOR =
            new DefaultMethodLevelDependencyCollectorVisitor();
    private static final Set<EdgeType> AFFECTED_EDGES =
            Set.of(FIELD_ACCESS, INHERITANCE, DELEGATION, ANNOTATION, JUNIT);

    //##################################################################################################################
    // Constructors

    public MethodLevelControl(Blackboard<BodyDeclaration<?>> blackboard, boolean overwrite) {
        super(blackboard,
                overwrite,
                METHOD_LEVEL_CHECKSUM_VISITOR,
                METHOD_LEVEL_NAME_FINDER_VISITOR,
                new MethodLevelTestFinderVisitor(blackboard.getTestFilter()),
                METHOD_LEVEL_NODES_IN_GRAPH,
                PRIMARY_DEPENDENCY_COLLECTOR,
                AFFECTED_EDGES);
    }

    //##################################################################################################################
    // Methods

    @Override
    public void init() {
        super.init();
        blackboard.addKnowledgeSource(new MethodLevelGraphCropper(blackboard,
                nameFinderVisitor,
                affectedEdges,
                nodesInGraphFilter));

        blackboard.addDependencyStrategy(new CachingDependencyStrategy<>(
                Set.of(new JUnitMethodLevelDependencyCollectorVisitor(blackboard.getTestFilter()))
        ));
    }
}
