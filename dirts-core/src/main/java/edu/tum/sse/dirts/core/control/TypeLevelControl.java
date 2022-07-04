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
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.def.DefaultTypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.analysis.def.checksum.TypeChecksumVisitor;
import edu.tum.sse.dirts.analysis.def.finders.TypeNameFinderVisitor;
import edu.tum.sse.dirts.analysis.def.finders.TypeTestFinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.knowledgesources.*;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.Set;
import java.util.function.Predicate;

import static edu.tum.sse.dirts.graph.EdgeType.*;

/**
 * Class-level RTS
 */
public class TypeLevelControl extends Control {

    //##################################################################################################################
    // Attributes

    private static final String SUFFIX = "typeL";

    private static final TypeChecksumVisitor TYPE_CHECKSUM_VISITOR = new TypeChecksumVisitor();
    private static final TypeNameFinderVisitor TYPE_NAME_FINDER_VISITOR = new TypeNameFinderVisitor();
    private static final TypeTestFinderVisitor TYPE_TEST_FINDER_VISITOR = new TypeTestFinderVisitor();

    private static final Predicate<Node> TYPES_IN_GRAPH =
            n -> !(n instanceof CompilationUnit);

    private static final DependencyCollector PRIMARY_DEPENDENCY_COLLECTOR =
            new DefaultTypeDependencyCollectorVisitor();
    private static final Set<EdgeType> AFFECTED_EDGES =
            Set.of(EXTENDS_IMPLEMENTS, NEW, STATIC, ANNOTATION);

    //##################################################################################################################
    // Constructors

    public TypeLevelControl(Blackboard blackboard, boolean overwrite) {
        super(blackboard,
                overwrite,
                SUFFIX,
                TYPE_CHECKSUM_VISITOR,
                TYPE_NAME_FINDER_VISITOR,
                TYPE_TEST_FINDER_VISITOR,
                TYPES_IN_GRAPH,
                PRIMARY_DEPENDENCY_COLLECTOR,
                AFFECTED_EDGES);
    }
}
