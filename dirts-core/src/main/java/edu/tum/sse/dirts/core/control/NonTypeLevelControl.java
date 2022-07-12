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
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.def.DefaultNonTypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.analysis.def.JUnitNonTypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.analysis.def.checksum.NonTypeChecksumVisitor;
import edu.tum.sse.dirts.analysis.def.finders.NonTypeNameFinderVisitor;
import edu.tum.sse.dirts.analysis.def.finders.NonTypeTestFinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.knowledgesources.graph_cropper.NonTypeLevelGraphCropper;
import edu.tum.sse.dirts.core.strategies.CachingDependencyStrategy;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.Set;
import java.util.function.Predicate;

import static edu.tum.sse.dirts.graph.EdgeType.*;

/**
 * NonType-level RTS
 */
public class NonTypeLevelControl extends Control<BodyDeclaration<?>> {

    //##################################################################################################################
    // Static constants

    private static final String SUFFIX = "nontypeL";

    private static final NonTypeChecksumVisitor NONTYPE_CHECKSUM_VISITOR = new NonTypeChecksumVisitor();
    private static final NonTypeNameFinderVisitor NONTYPE_NAME_FINDER_VISITOR = new NonTypeNameFinderVisitor();

    private static final Predicate<Node> NONTYPES_IN_GRAPH =
            n -> !(n instanceof CompilationUnit || n instanceof TypeDeclaration);

    private static final DependencyCollector<BodyDeclaration<?>> PRIMARY_DEPENDENCY_COLLECTOR =
            new DefaultNonTypeDependencyCollectorVisitor();
    private static final Set<EdgeType> AFFECTED_EDGES =
            Set.of(FIELD_ACCESS, INHERITANCE, DELEGATION, ANNOTATION, JUNIT);

    //##################################################################################################################
    // Constructors

    public NonTypeLevelControl(Blackboard<BodyDeclaration<?>> blackboard, boolean overwrite) {
        super(blackboard,
                overwrite,
                SUFFIX,
                NONTYPE_CHECKSUM_VISITOR,
                NONTYPE_NAME_FINDER_VISITOR,
                new NonTypeTestFinderVisitor(blackboard.getTestFilter()),
                NONTYPES_IN_GRAPH,
                PRIMARY_DEPENDENCY_COLLECTOR,
                AFFECTED_EDGES);
    }

    //##################################################################################################################
    // Methods

    @Override
    public void init() {
        super.init();
        blackboard.addKnowledgeSource(new NonTypeLevelGraphCropper(blackboard,
                nameFinderVisitor,
                affectedEdges,
                nodesInGraphFilter));

        blackboard.addDependencyStrategy(new CachingDependencyStrategy<>(
                Set.of(new JUnitNonTypeDependencyCollectorVisitor(blackboard.getTestFilter())),
                Set.of(JUNIT)));
    }
}
