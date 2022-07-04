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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.analysis.TypeDependencyCollector;
import edu.tum.sse.dirts.analysis.def.finders.TypeTestFinderVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.JUnit4BeforeMethodIdentifierVisitor;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.JavaParserUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static edu.tum.sse.dirts.analysis.def.finders.TypeNameFinderVisitor.recursiveMemberTest;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;

/**
 * Collects dependencies related to JUnit4 for Type-nodes
 */
public class JUnit4TypeDependencyCollectorVisitor
        extends JUnit4DependencyCollectorVisitor
        implements TypeDependencyCollector {

    //##################################################################################################################
    // Methods inherited from JUnit4DependencyCollectorVisitor

    protected void processBeforeMethods(DependencyGraph dependencyGraph,
                                        String node,
                                        Collection<ResolvedMethodDeclaration> resolvedBeforeMethods) {
        for (ResolvedMethodLikeDeclaration resolvedMethod : resolvedBeforeMethods) {
            String toNode = lookup(resolvedMethod.declaringType());
            dependencyGraph.addEdge(node, toNode, EdgeType.JUNIT);
        }
    }

    //##################################################################################################################
    // Visirot pattern - Methods inherited from TypeDependencyCollector

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {

        // ## Account for tests in inner classes
        // if this class has members that contain tests, add edge to this class
        if (FinderVisitor.recursiveMemberTest(n)) {
            for (BodyDeclaration<?> member : n.getMembers()) {
                if (member.isClassOrInterfaceDeclaration()) {
                    ClassOrInterfaceDeclaration classOrInterfaceDeclaration = member.asClassOrInterfaceDeclaration();
                    if (TypeTestFinderVisitor.testClassDeclaration.test(classOrInterfaceDeclaration)
                            || FinderVisitor.recursiveMemberTest(classOrInterfaceDeclaration)) {
                        String toNode = lookupNode(member, dependencyGraph);
                        String fromNode = lookupNode(n, dependencyGraph);
                        dependencyGraph.addEdge(fromNode, toNode, EdgeType.JUNIT);
                    }
                }
            }
        }

        // ## Account for tests in extended class
        try {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
            List<ResolvedReferenceType> allAncestors = resolvedReferenceTypeDeclaration
                    .getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList);
            for (ResolvedReferenceType ancestor : allAncestors) {
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = ancestor.getTypeDeclaration();
                if (maybeTypeDeclaration.isPresent()) {
                    ResolvedReferenceTypeDeclaration resolvedAncestorTypeDeclaration = maybeTypeDeclaration.get();
                    if (resolvedAncestorTypeDeclaration.isClass()) {
                        ResolvedClassDeclaration resolvedClassDeclaration = resolvedAncestorTypeDeclaration.asClass();
                        Optional<Node> maybeClassDeclaration = resolvedClassDeclaration.toAst();
                        if (maybeClassDeclaration.isPresent()) {
                            Node node = maybeClassDeclaration.get();
                            if (node instanceof ClassOrInterfaceDeclaration) {
                                ClassOrInterfaceDeclaration ancestorDeclaration = (ClassOrInterfaceDeclaration) node;
                                if (TypeTestFinderVisitor.testClassDeclaration.test(ancestorDeclaration)) {
                                    // If class extends a test class, it can be executed as test as well
                                    String toNode = lookup(resolvedAncestorTypeDeclaration);
                                    String fromNode = lookupNode(n, dependencyGraph);
                                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.JUNIT);
                                }
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            if (Control.DEBUG)
                System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }

        if (TypeTestFinderVisitor.testClassDeclaration.test(n)) {

            JUnit4BeforeMethodIdentifierVisitor identifierVisitor = new JUnit4BeforeMethodIdentifierVisitor(n);

            Collection<ResolvedMethodDeclaration> resolvedBeforeMethods = identifierVisitor.getResolvedBeforeMethods();

            // Collect BeforeMethods here
            n.getChildNodes().forEach(child -> child.accept(identifierVisitor, resolvedBeforeMethods));

            String declaredNode = lookupNode(n, dependencyGraph);

            // Dependencies from methods annotated with @Before
            processBeforeMethods(dependencyGraph, declaredNode, resolvedBeforeMethods);
        }
    }

    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        // TestMethods should not exist in Enums
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        // TestMethods should not exist in Annotations
    }
}
