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

import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.*;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINEST;

/**
 * Collects Dependencies induced by xml alternative entries for method level nodes
 */
public class CDIMethodLevelAlternativeDependencyCollector extends CDIAlternativeDependencyCollector<BodyDeclaration<?>> {

    //##################################################################################################################
    // Visitor pattern - auxiliary visitor methods (used to set up things before)

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        super.visit(n, dependencyGraph);

        if (CDIUtil.isAlternativeNode(n)) {
            // Add edge to potential entry from alternatives in beans.xml
            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                String name = lookup(resolvedReferenceTypeDeclaration);
                if (alternatives.contains(name)) {
                    for (ResolvedConstructorDeclaration constructor :
                            resolvedReferenceTypeDeclaration.getConstructors()) {
                        String toNode = CDIUtil.lookupXMlAlternativeName(name);
                        dependencyGraph.addEdge(lookup(constructor), toNode, EdgeType.DI_CDI);
                    }
                }
            } catch (Throwable e) {
                Log.log(FINEST, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    //##################################################################################################################
    // Visitor pattern - Methods inherited from MethodLevelDependencyCollector

    @Override
    public void visit(MethodDeclaration n, DependencyGraph dependencyGraph) {
        if (CDIUtil.isAlternativeNode(n)
                && CDIUtil.isProducerNode(n)) {
            // Add edge to potential entry from alternatives in beans.xml
            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();
                String declaringType = lookup(resolvedMethodDeclaration.declaringType());
                if (alternatives.contains(declaringType)) {
                    String toNode = CDIUtil.lookupXMlAlternativeName(declaringType);
                    String fromNode = lookup(resolvedMethodDeclaration);
                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.DI_CDI);
                }
            } catch (Throwable e) {
                Log.log(FINEST, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, DependencyGraph dependencyGraph) {
    }


    @Override
    public void visit(FieldDeclaration n, DependencyGraph dependencyGraph) {
        if (n.getVariables().size() == 1
                && CDIUtil.isAlternativeNode(n)
                && CDIUtil.isProducerNode(n)) {
            // Add edge to potential entry from alternatives in beans.xml
            try {
                ResolvedFieldDeclaration resolvedFieldDeclaration = n.resolve();
                ResolvedTypeDeclaration declaringType = resolvedFieldDeclaration.declaringType();
                String declaringTypeName = lookup(declaringType);
                if (alternatives.contains(declaringTypeName)) {
                    String toNode = CDIUtil.lookupXMlAlternativeName(declaringTypeName);
                    String fromNode = lookup(declaringType, resolvedFieldDeclaration);
                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.DI_CDI);
                }
            } catch (Throwable e) {
                Log.log(FINEST, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void visit(EnumConstantDeclaration n, DependencyGraph dependencyGraph) {

    }

    @Override
    public void visit(AnnotationMemberDeclaration n, DependencyGraph dependencyGraph) {

    }

    @Override
    public void visit(InitializerDeclaration n, DependencyGraph dependencyGraph) {

    }
}
