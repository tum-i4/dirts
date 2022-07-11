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
package edu.tum.sse.dirts.analysis.def;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import edu.tum.sse.dirts.analysis.TypeDependencyCollector;
import edu.tum.sse.dirts.analysis.def.identifiers.AnnotationIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.type.ExtendsImplementsIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.type.NewIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.type.StaticIdentifierVisitor;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.lang.reflect.Type;
import java.util.Collection;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;

/**
 * Collects dependencies based on Delegation, MemberAccess, Inheritance and Annotations (not critical) for Type-nodes
 */
public class DefaultTypeDependencyCollectorVisitor
        extends DefaultDependencyCollectorVisitor<TypeDeclaration<?>>
        implements TypeDependencyCollector {

    /*
     * This class implements the algorithm to compute dependencies from AutoRTS
     * (J. Öqvist, G. Hedin, and B. Magnusson, “Extraction-based regression test selection,”
     * ACM Int. Conf. Proceeding Ser., vol. Part F1284, 2016, doi: 10.1145/2972206.2972224)
     */

    //##################################################################################################################
    // Methods that add edges

    private void processNew(DependencyGraph dependencyGraph,
                            String node,
                            Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations) {
        for (ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypeDeclarations) {
            String toNode = lookup(resolvedReferenceTypeDeclaration);
            dependencyGraph.addEdge(node, toNode, EdgeType.NEW);
        }
    }

    private void processStatic(DependencyGraph dependencyGraph,
                               String node,
                               Collection<ResolvedTypeDeclaration> resolvedReferenceTypeDeclarations) {
        for (ResolvedTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypeDeclarations) {
            String toNode = lookup(resolvedReferenceTypeDeclaration);
            dependencyGraph.addEdge(node, toNode, EdgeType.STATIC);
        }
    }

    private void processExtendsImplements(DependencyGraph dependencyGraph,
                                          String node,
                                          Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypes) {
        for (ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypes) {
            String toNode = lookup(resolvedReferenceTypeDeclaration);
            dependencyGraph.addEdge(node, toNode, EdgeType.EXTENDS_IMPLEMENTS);
        }
    }

    private void processAnnotations(DependencyGraph dependencyGraph,
                                    String node,
                                    Collection<ResolvedAnnotationDeclaration> resolvedAnnotations) {
        for (ResolvedAnnotationDeclaration resolvedAnnotation : resolvedAnnotations) {
            String toNode = lookup(resolvedAnnotation);
            dependencyGraph.addEdge(node, toNode, EdgeType.ANNOTATION);
        }
    }

    //##################################################################################################################
    // Visitor pattern - Methods inherited from TypeDependencyCollector

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        handleExtendsImplementsNewStatic(n, dependencyGraph);
    }

    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        handleExtendsImplementsNewStatic(n, dependencyGraph);
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        handleExtendsImplementsNewStatic(n, dependencyGraph);
    }

    //##################################################################################################################
    // Auxiliary methods

    private void handleExtendsImplementsNewStatic(TypeDeclaration<?> n, DependencyGraph dependencyGraph) {
        String node = lookupNode(n, dependencyGraph);
        processNew(dependencyGraph, node, NewIdentifierVisitor.identifyDependencies(n));
        processExtendsImplements(dependencyGraph, node, ExtendsImplementsIdentifierVisitor.identifyDependencies(n));
        processStatic(dependencyGraph, node, StaticIdentifierVisitor.identifyDependencies(n));
        processAnnotations(dependencyGraph, node, AnnotationIdentifierVisitor.identifyDependencies(n));
    }
}
