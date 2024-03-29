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

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import edu.tum.sse.dirts.analysis.ClassLevelDependencyCollector;
import edu.tum.sse.dirts.analysis.def.identifiers.AnnotationIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.classlevel.ExtendsImplementsIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.classlevel.NewIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.classlevel.StaticIdentifierVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.DependencyGraph;

import java.util.Collection;
import java.util.Set;

import static edu.tum.sse.dirts.graph.EdgeType.*;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;

/**
 * Collects dependencies based on Extends, Implements, New, Static and Annotations (if specified)
 * for class level nodes
 */
public class DefaultClassLevelDependencyCollectorVisitor
        extends DefaultDependencyCollectorVisitor<TypeDeclaration<?>>
        implements ClassLevelDependencyCollector {

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
            dependencyGraph.addEdge(node, toNode, NEW);
        }
    }

    private void processStatic(DependencyGraph dependencyGraph,
                               String node,
                               Collection<ResolvedTypeDeclaration> resolvedReferenceTypeDeclarations) {
        for (ResolvedTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypeDeclarations) {
            String toNode = lookup(resolvedReferenceTypeDeclaration);
            dependencyGraph.addEdge(node, toNode, STATIC);
        }
    }

    private void processExtendsImplements(DependencyGraph dependencyGraph,
                                          String node,
                                          Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypes) {
        for (ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypes) {
            String toNode = lookup(resolvedReferenceTypeDeclaration);
            dependencyGraph.addEdge(node, toNode, EXTENDS_IMPLEMENTS);
        }
    }

    private void processAnnotations(DependencyGraph dependencyGraph,
                                    String node,
                                    Collection<ResolvedAnnotationDeclaration> resolvedAnnotations) {
        if (Blackboard.considerAnnotationsAsDependencies) {
            for (ResolvedAnnotationDeclaration resolvedAnnotation : resolvedAnnotations) {
                String toNode = lookup(resolvedAnnotation);
                dependencyGraph.addEdge(node, toNode, ANNOTATION);
            }
        }
    }

    //##################################################################################################################
    // Visitor pattern - Methods inherited from ClassLevelDependencyCollector

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
        dependencyGraph.removeAllEdgesFrom(node, Set.of(NEW, EXTENDS_IMPLEMENTS, STATIC, ANNOTATION));
        processNew(dependencyGraph, node, NewIdentifierVisitor.identifyDependencies(n));
        processExtendsImplements(dependencyGraph, node, ExtendsImplementsIdentifierVisitor.identifyDependencies(n));
        processStatic(dependencyGraph, node, StaticIdentifierVisitor.identifyDependencies(n));
        processAnnotations(dependencyGraph, node, AnnotationIdentifierVisitor.identifyDependencies(n));
    }
}
