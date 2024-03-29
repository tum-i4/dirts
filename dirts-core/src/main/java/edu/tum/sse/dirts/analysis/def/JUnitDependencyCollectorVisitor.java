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
package edu.tum.sse.dirts.analysis.def;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.graph.DependencyGraph;

import java.util.Collection;

/**
 * Collects dependencies based on JUnit4 and JUnit5
 * <p>
 * Considers methods annotated with @Before, @BeforeClass or @BeforeEach, @BeforeAll
 * as dependencies of methods annotated with @Test
 */
public abstract class JUnitDependencyCollectorVisitor<T extends BodyDeclaration<?>>
        extends AbstractTruncatedVisitor<DependencyGraph>
        implements DependencyCollector<T> {

    public void calculateDependencies(Collection<TypeDeclaration<?>> ts, DependencyGraph dependencyGraph) {
        for (TypeDeclaration<?> t : ts) {
            t.accept(this, dependencyGraph);
        }
    }
}
