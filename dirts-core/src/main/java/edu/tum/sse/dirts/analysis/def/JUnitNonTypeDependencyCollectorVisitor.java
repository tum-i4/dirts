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
import edu.tum.sse.dirts.analysis.def.finders.TypeNameFinderVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.JUnit4BeforeMethodIdentifierVisitor;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.naming_scheme.Names;
import edu.tum.sse.dirts.util.tuples.Pair;
import org.apache.maven.surefire.api.testset.TestFilter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;

/**
 * Collects dependencies related to JUnit4 for NonType-nodes
 */
public class JUnitNonTypeDependencyCollectorVisitor
        extends JUnitDependencyCollectorVisitor
        implements NonTypeDependencyCollector {

    //##################################################################################################################
    // Attributes

    private final TestFilter<String, String> testFilter;
    private Collection<ResolvedMethodDeclaration> resolvedBeforeMethods = null;

    //##################################################################################################################
    // Constructors

    public JUnitNonTypeDependencyCollectorVisitor(TestFilter<String, String> testFilter) {
        this.testFilter = testFilter;
    }

    //##################################################################################################################
    // Visitor pattern - Auxiliary methods (used to set up things before)

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {

        // ## Account for tests in inner classes
        // if this class has members that contain tests,
        // put edge from first constructor to the test methods and first constructor of this class
        // Constructor of outer class is considered as test by NonTypeTestFinderVisitor
        if (FinderVisitor.recursiveMemberTest(n, testFilter)) {
            for (BodyDeclaration<?> member : n.getMembers()) {
                if (member.isClassOrInterfaceDeclaration()) {
                    ClassOrInterfaceDeclaration innerClassOrInterfaceDeclaration = member.asClassOrInterfaceDeclaration();

                    if (FinderVisitor.testClassDeclaration(innerClassOrInterfaceDeclaration, testFilter)
                            || TypeNameFinderVisitor.recursiveMemberTest(innerClassOrInterfaceDeclaration, testFilter)) {
                        try {
                            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                            ResolvedReferenceTypeDeclaration resolvedInnerClassDeclaration = innerClassOrInterfaceDeclaration.resolve();

                            String fromNode = lookup(resolvedReferenceTypeDeclaration.getConstructors().get(0));

                            // Create edges from constructor of outer class to constructor of inner class
                            {
                                List<ResolvedConstructorDeclaration> constructors = resolvedInnerClassDeclaration.getConstructors();
                                String toNode = lookup(constructors.get(0));
                                dependencyGraph.addEdge(fromNode, toNode, EdgeType.JUNIT);
                            }

                            // Create edges from constructor of outer class to test methods
                            if (FinderVisitor.testClassDeclaration(innerClassOrInterfaceDeclaration, testFilter)) {
                                for (ResolvedMethodDeclaration declaredMethod : resolvedInnerClassDeclaration.getDeclaredMethods()) {
                                    if (testFilter.shouldRun(lookup(resolvedReferenceTypeDeclaration), declaredMethod.getName())) {
                                        String toNode = lookup(declaredMethod);
                                        dependencyGraph.addEdge(fromNode, toNode, EdgeType.JUNIT);
                                    }
                                }
                            }
                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        // ## Account for test methods from extended test classes
        // If this class extends a test class, it inherits its methods as test methods
        // Method is considered as test by NonTypeTestFinderVisitor
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
                            if (testFilter.shouldRun(lookup(resolvedReferenceTypeDeclaration), declaredMethod.getName())) {
                                // If the class extends a test class, we have to add an edge to inherited test methods
                                String fromNode = Names.lookup(resolvedReferenceTypeDeclaration) + "." + declaredMethod.getSignature();
                                String toNode = Names.lookup(declaredMethod);
                                dependencyGraph.addEdge(fromNode, toNode, EdgeType.INHERITANCE);
                            }
                        }
                    }
                }
            }
        } catch (
                RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }

        if (FinderVisitor.testClassDeclaration(n, testFilter)) {
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
        if (FinderVisitor.testMethodDeclaration(n, testFilter)) {
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
