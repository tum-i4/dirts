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
package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;

/**
 * Collects Dependencies induced by xml alternative entries for class level nodes
 */
public class CDIClassLevelAlternativeDependencyCollector extends CDIAlternativeDependencyCollector<TypeDeclaration<?>> {

    //##################################################################################################################
    // Visitor pattern - Methods inherited from ClassLevelDependencyCollector

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        super.visit(n, dependencyGraph);

        if (CDIUtil.isAlternativeNode(n)
                || n.getMethods().stream().anyMatch(m -> CDIUtil.isProducerNode(m) && CDIUtil.isAlternativeNode(m))
                || n.getFields().stream().anyMatch(m -> CDIUtil.isProducerNode(m) && CDIUtil.isAlternativeNode(m))
        ) {
            // Add edge to potential entry from alternatives in beans.xml
            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                String fromNode = lookup(resolvedReferenceTypeDeclaration);
                if (alternatives.contains(fromNode)) {
                    String toNode = CDIUtil.lookupXMlAlternativeName(fromNode);
                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.DI_CDI);
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        super.visit(n, dependencyGraph);
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        super.visit(n, dependencyGraph);
    }
}
