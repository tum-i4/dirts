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
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.def.DefaultClassLevelDependencyCollectorVisitor;
import edu.tum.sse.dirts.analysis.def.JUnitClassLevelDependencyCollectorVisitor;
import edu.tum.sse.dirts.analysis.def.checksum.ClassLevelChecksumVisitor;
import edu.tum.sse.dirts.analysis.def.finders.ClassLevelNameFinderVisitor;
import edu.tum.sse.dirts.analysis.def.finders.ClassLevelTestFinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.knowledgesources.graph_cropper.ClassLevelGraphCropper;
import edu.tum.sse.dirts.core.strategies.CachingDependencyStrategy;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.Set;
import java.util.function.Predicate;

import static edu.tum.sse.dirts.graph.EdgeType.*;

/**
 * Class level RTS
 */
public class ClassLevelControl extends Control<TypeDeclaration<?>> {

    //##################################################################################################################
    // Static constants

    private static final ClassLevelChecksumVisitor TYPE_CHECKSUM_VISITOR = new ClassLevelChecksumVisitor();
    private static final ClassLevelNameFinderVisitor TYPE_NAME_FINDER_VISITOR = new ClassLevelNameFinderVisitor();

    private static final Predicate<Node> TYPES_IN_GRAPH =
            n -> !(n instanceof CompilationUnit);

    private static final DefaultClassLevelDependencyCollectorVisitor PRIMARY_DEPENDENCY_COLLECTOR =
            new DefaultClassLevelDependencyCollectorVisitor();
    private static final Set<EdgeType> AFFECTED_EDGES =
            Set.of(EXTENDS_IMPLEMENTS, NEW, STATIC, ANNOTATION);

    //##################################################################################################################
    // Constructors

    public ClassLevelControl(Blackboard<TypeDeclaration<?>> blackboard, boolean overwrite) {
        super(blackboard,
                overwrite,
                TYPE_CHECKSUM_VISITOR,
                TYPE_NAME_FINDER_VISITOR,
                new ClassLevelTestFinderVisitor(blackboard.getTestFilter()),
                TYPES_IN_GRAPH,
                PRIMARY_DEPENDENCY_COLLECTOR,
                AFFECTED_EDGES);
    }

    //##################################################################################################################
    // Methods

    @Override
    public void init() {
        super.init();
        blackboard.addKnowledgeSource(new ClassLevelGraphCropper(blackboard,
                nameFinderVisitor,
                affectedEdges,
                nodesInGraphFilter));
        blackboard.addDependencyStrategy(new CachingDependencyStrategy<>(
                Set.of(new JUnitClassLevelDependencyCollectorVisitor(blackboard.getTestFilter()))
        ));
    }
}
