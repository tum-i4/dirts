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

import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.analysis.NonTypeDependencyCollector;
import edu.tum.sse.dirts.analysis.def.finders.NonTypeTestFinderVisitor;
import edu.tum.sse.dirts.analysis.def.finders.TypeNameFinderVisitor;
import edu.tum.sse.dirts.analysis.def.finders.TypeTestFinderVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.JUnit4BeforeMethodIdentifierVisitor;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.naming_scheme.Names;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static edu.tum.sse.dirts.analysis.def.finders.TypeNameFinderVisitor.recursiveMemberTest;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;

/**
 * Collects dependencies related to JUnit4 for NonType-nodes
 */
public class JUnit4NonTypeDependencyCollectorVisitor
        extends JUnit4DependencyCollectorVisitor
        implements NonTypeDependencyCollector {

    private Collection<ResolvedMethodDeclaration> resolvedBeforeMethods = null;

    //##################################################################################################################
    // Visitor pattern - Auxiliary methods (used to set up things before)

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {

        // ## Account for tests in inner classes
        // if this class has members that contain tests,
        // put node from all constructors to the test methods and constructors of this class
        if (FinderVisitor.recursiveMemberTest(n)) {
            for (BodyDeclaration<?> member : n.getMembers()) {
                if (member.isClassOrInterfaceDeclaration()) {
                    ClassOrInterfaceDeclaration innerClassOrInterfaceDeclaration = member.asClassOrInterfaceDeclaration();

                    // This has to be a method call on TypeTestFinderVisitor
                    if (TypeTestFinderVisitor.testClassDeclaration.test(innerClassOrInterfaceDeclaration)
                            || TypeNameFinderVisitor.recursiveMemberTest(innerClassOrInterfaceDeclaration)) {
                        try {
                            ResolvedReferenceTypeDeclaration resolvedInnerClassDeclaration = innerClassOrInterfaceDeclaration.resolve();
                            List<ResolvedConstructorDeclaration> constructors = resolvedInnerClassDeclaration.getConstructors();
                            for (ResolvedConstructorDeclaration constructor : constructors) {
                                String toNode = lookup(constructor);
                                String fromNode = lookupNode(n, dependencyGraph);
                                dependencyGraph.addEdge(fromNode, toNode, EdgeType.JUNIT);
                            }

                            // This has to be a method call on TypeTestFinderVisitor
                            if (TypeTestFinderVisitor.testClassDeclaration.test(innerClassOrInterfaceDeclaration)) {
                                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                                for (ResolvedMethodDeclaration declaredMethod : resolvedInnerClassDeclaration.getDeclaredMethods()) {
                                    Optional<MethodDeclaration> maybeMethodDeclaration = declaredMethod.toAst();
                                    if (maybeMethodDeclaration.isPresent()) {
                                        MethodDeclaration methodDeclaration = maybeMethodDeclaration.get();
                                        if (!methodDeclaration.getNameAsString().equals("setUp") &&
                                                !methodDeclaration.getNameAsString().equals("tearDown")) {
                                            String toNode = lookup(declaredMethod);
                                            String fromNode = lookup(resolvedReferenceTypeDeclaration.getConstructors().get(0));
                                            dependencyGraph.addEdge(fromNode, toNode, EdgeType.JUNIT);
                                        }
                                    }
                                }
                            } else {
                                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                                for (ResolvedMethodDeclaration declaredMethod : resolvedInnerClassDeclaration.getDeclaredMethods()) {
                                    Optional<MethodDeclaration> maybeMethodDeclaration = declaredMethod.toAst();
                                    if (maybeMethodDeclaration.isPresent()) {
                                        MethodDeclaration methodDeclaration = maybeMethodDeclaration.get();
                                        if (NonTypeTestFinderVisitor.testMethodDeclaration.test(methodDeclaration)) {
                                            String toNode = lookup(declaredMethod);
                                            String fromNode = lookup(resolvedReferenceTypeDeclaration.getConstructors().get(0));
                                            dependencyGraph.addEdge(fromNode, toNode, EdgeType.JUNIT);
                                        }
                                    }
                                }
                            }
                        } catch (RuntimeException e) {
                            if (Control.DEBUG)
                                System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        // ## Account for tests from extended test classes
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
                        for (ResolvedMethodDeclaration declaredMethod : resolvedClassDeclaration.getDeclaredMethods()) {
                            Optional<MethodDeclaration> maybeMethodDeclaration = declaredMethod.toAst();
                            if (maybeMethodDeclaration.isPresent()) {
                                MethodDeclaration methodDeclaration = maybeMethodDeclaration.get();
                                if (NonTypeTestFinderVisitor.testMethodDeclaration.test(methodDeclaration)) {
                                    // If the class extends a test class, we have to add an edge to inherited Methods
                                    String fromNode = Names.lookup(resolvedReferenceTypeDeclaration) + "." + declaredMethod.getSignature();
                                    String toNode = Names.lookup(declaredMethod);
                                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.INHERITANCE);
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

        // This has to be a method call on TypeTestFinderVisitor
        if (TypeTestFinderVisitor.testClassDeclaration.test(n)) {
            JUnit4BeforeMethodIdentifierVisitor identifierVisitor = new JUnit4BeforeMethodIdentifierVisitor(n);

            this.resolvedBeforeMethods = identifierVisitor.getResolvedBeforeMethods();

            // Collect BeforeMethods here
            n.getChildNodes().forEach(child -> child.accept(identifierVisitor, this.resolvedBeforeMethods));

            n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                    .forEach(m -> m.accept(this, dependencyGraph));
        }
    }

    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        // TestMethods should not exist in Enums

        // No need to proceed
        // no n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
        //        .forEach(m -> m.accept(this, arg));
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        // TestMethods should not exist in Annotations

        // No need to proceed
        // no n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
        //        .forEach(m -> m.accept(this, arg));
    }


    //##################################################################################################################
    // Methods that add edges
    private void processBeforeMethods(DependencyGraph dependencyGraph,
                                      String node,
                                      Collection<ResolvedMethodDeclaration> resolvedBeforeMethods) {
        for (ResolvedMethodLikeDeclaration resolvedBeforeMethod : resolvedBeforeMethods) {
            String toNode = lookup(resolvedBeforeMethod);
            dependencyGraph.addEdge(node, toNode, EdgeType.JUNIT);
        }
    }

    //##################################################################################################################
    // Visitor pattern - Methods inherited from NonTypeDependencyCollector

    @Override
    public void visit(MethodDeclaration n, DependencyGraph dependencyGraph) {
        if (NonTypeTestFinderVisitor.testMethodDeclaration.test(n)) {
            Pair<String, Optional<String>> lookup = lookup(n);
            String node = lookup.getFirst();
            lookup.getSecond().ifPresent(e -> dependencyGraph.addMessage(node, e));

            // Dependencies from methods annotated with @Before
            processBeforeMethods(dependencyGraph, node, this.resolvedBeforeMethods);
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, DependencyGraph dependencyGraph) {
        // @Test should not be present on a Constructor
    }

    @Override
    public void visit(FieldDeclaration n, DependencyGraph dependencyGraph) {
        // @Test should not be present on a Field
    }

    @Override
    public void visit(EnumConstantDeclaration n, DependencyGraph dependencyGraph) {
        // @Test should not be present on an EnumConstantDeclaration
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, DependencyGraph dependencyGraph) {
        // @Test should not be present on an AnnotationMemberDeclaration
    }

    @Override
    public void visit(InitializerDeclaration n, DependencyGraph dependencyGraph) {
        // @Test should not be present on an InitializerDeclaration
    }
}
