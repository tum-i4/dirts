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
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.resolution.declarations.*;
import edu.tum.sse.dirts.analysis.NonTypeDependencyCollector;
import edu.tum.sse.dirts.analysis.def.identifiers.AnnotationIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.nontype.DelegationIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.nontype.FieldAccessIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.nontype.InheritanceIdentifierVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.*;
import java.util.function.Function;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;
import static java.util.logging.Level.FINE;

/**
 * Collects dependencies based on Delegation, MemberAccess and Inheritance for NonType-nodes
 */
public class DefaultNonTypeDependencyCollectorVisitor
        extends DefaultDependencyCollectorVisitor<BodyDeclaration<?>>
        implements NonTypeDependencyCollector {

    private Map<TypeDeclaration<?>, InheritanceIdentifierVisitor> inheritanceIdentifierVisitorMap;

    public void init(Blackboard<?> blackboard) {
        this.inheritanceIdentifierVisitorMap = blackboard.getInheritanceIdentifierVisitorMap();
    }

    //##################################################################################################################
    // Visitor pattern - Auxiliary methods (used to set up things before)

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        //InheritanceIdentifierVisitor inheritanceIdentifierVisitor = new InheritanceIdentifierVisitor(n);
        //inheritanceIdentifierVisitorMap.put(n, inheritanceIdentifierVisitor);
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));

        InheritanceIdentifierVisitor inheritanceIdentifierVisitor = inheritanceIdentifierVisitorMap.get(n);

        // Dependencies from implicitly invoked constructor
        if (n.getConstructors().isEmpty()) {
            /*
                If a class has no constructors, it has an implicit default constructor
                When this constructor is invoked, the zero-argument constructor of the parent class is invoked implicitly
             */

            try {
                Set<ResolvedConstructorDeclaration> parentConstructors = inheritanceIdentifierVisitor.getParentZeroArgConstructors();
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                List<ResolvedConstructorDeclaration> constructors = resolvedReferenceTypeDeclaration.getConstructors();
                for (ResolvedConstructorDeclaration constructor : constructors) { // Will only contain a single default constructor
                    String node = lookup(constructor);
                    dependencyGraph.addNode(node);
                    processDelegation(dependencyGraph, node, new HashSet<>(parentConstructors));
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }


    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        inheritanceIdentifierVisitorMap.put(n, new InheritanceIdentifierVisitor(n));
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        // Annotations are not subject to inheritance

        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));
    }


    //##################################################################################################################
    // Methods that add edges

    private void processFieldAccess(DependencyGraph dependencyGraph,
                                    String node,
                                    Pair<Collection<ResolvedValueDeclaration>, Collection<ResolvedValueDeclaration>> resolvedFields) {
        Collection<ResolvedValueDeclaration> resolvedAccessedFields = resolvedFields.getFirst();
        Collection<ResolvedValueDeclaration> resolvedAssignedFields = resolvedFields.getSecond();

        for (ResolvedValueDeclaration resolvedAccessedField : resolvedAccessedFields) {
            if (resolvedAccessedField.isField()) {
                String toNode = lookup(resolvedAccessedField.asField().declaringType(), resolvedAccessedField.asField());
                dependencyGraph.addEdge(node, toNode, EdgeType.FIELD_ACCESS);
            } else if (resolvedAccessedField.isEnumConstant()) {
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = resolvedAccessedField.getType().asReferenceType().getTypeDeclaration();
                if (maybeTypeDeclaration.isPresent()) {
                    String toNode = lookup(maybeTypeDeclaration.get(), resolvedAccessedField.asEnumConstant());
                    dependencyGraph.addEdge(node, toNode, EdgeType.FIELD_ACCESS);
                }
            }
        }
        for (ResolvedValueDeclaration resolvedAssignedField : resolvedAssignedFields) {
            if (resolvedAssignedField.isField()) {
                String fromNode = lookup(resolvedAssignedField.asField().declaringType(), resolvedAssignedField.asField());
                dependencyGraph.addEdge(fromNode, node, EdgeType.FIELD_ACCESS);
            }
            // Assignment to EnumConstants is not possible
        }
    }

    private void processDelegation(DependencyGraph dependencyGraph,
                                   String node,
                                   Collection<ResolvedMethodLikeDeclaration> resolvedMethods) {
        for (ResolvedMethodLikeDeclaration resolvedMethod : resolvedMethods) {
            if (!resolvedMethod.declaringType().isAnonymousClass()) {
                String toNode = lookup(resolvedMethod);
                dependencyGraph.addEdge(node, toNode, EdgeType.DELEGATION);
            }
        }
    }

    private void processInheritance(DependencyGraph dependencyGraph,
                                    String node,
                                    Collection<ResolvedMethodLikeDeclaration> resolvedOverriddenMethods) {
        for (ResolvedMethodLikeDeclaration resolvedOverriddenMethod : resolvedOverriddenMethods) {
            if (!resolvedOverriddenMethod.declaringType().isAnonymousClass()) {
                String fromNode = lookup(resolvedOverriddenMethod);
                dependencyGraph.addEdge(fromNode, node, EdgeType.INHERITANCE);
            }
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
    // Visitor pattern - Methods inherited from NonTypeDependencyCollector

    @Override
    public void visit(MethodDeclaration n, DependencyGraph dependencyGraph) {
        String node = lookupNode(n, dependencyGraph);

        // Dependencies from Delegation and Accessed Fields
        handleDelegationFieldsAndAnnotations(n, dependencyGraph);

        // Dependencies from Inheritance
        TypeDeclaration<?> containingTypeDeclaration = getContainingTypeDeclaration(n);
        if (containingTypeDeclaration != null) {
            InheritanceIdentifierVisitor inheritanceIdentifierVisitor = this.inheritanceIdentifierVisitorMap.get(containingTypeDeclaration);
            if (inheritanceIdentifierVisitor != null) {
                Collection<ResolvedMethodLikeDeclaration> resolvedOverriddenMethods = new ArrayList<>();
                inheritanceIdentifierVisitor.visit(n, resolvedOverriddenMethods);
                processInheritance(dependencyGraph, node, resolvedOverriddenMethods);
            }
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, DependencyGraph dependencyGraph) {
        String node = lookupNode(n, dependencyGraph);

        // Dependencies from implicitly invoked constructor
        if (n.getBody().findFirst(SuperExpr.class).isEmpty()) {
            /*
                When no explicit call to super(...) is present, the zero-argument constructor of the parent class is invoked implicitly
             */

            TypeDeclaration<?> containingTypeDeclaration = getContainingTypeDeclaration(n);
            if (containingTypeDeclaration != null) {
                InheritanceIdentifierVisitor inheritanceIdentifierVisitor = this.inheritanceIdentifierVisitorMap.get(containingTypeDeclaration);
                if (inheritanceIdentifierVisitor != null) {
                    Set<ResolvedConstructorDeclaration> parentConstructors = inheritanceIdentifierVisitor.getParentZeroArgConstructors();
                    processDelegation(dependencyGraph, node, new HashSet<>(parentConstructors));
                }
            }
        }

        // Dependencies from Delegation and Accessed Fields
        handleDelegationFieldsAndAnnotations(n, dependencyGraph);
    }

    @Override
    public void visit(FieldDeclaration n, DependencyGraph dependencyGraph) {
        if (n.getVariables().size() == 1) {
            // Dependencies from Delegation and Accessed Fields
            handleDelegationFieldsAndAnnotations(n, dependencyGraph);
        } else {
            for (VariableDeclarator variableDeclarator : n.getVariables()) {
                String node = lookupNode(n, dependencyGraph);
                String declaratorNode = lookupNode(variableDeclarator, dependencyGraph);
                dependencyGraph.addNode(declaratorNode);
                dependencyGraph.addEdge(node, declaratorNode, EdgeType.MULTIPLE_FIELD_DECL);
                dependencyGraph.addEdge(declaratorNode, node, EdgeType.MULTIPLE_FIELD_DECL);

                // Dependencies from Delegation and Accessed Fields
                handleDelegationFieldsAndAnnotations(variableDeclarator, dependencyGraph);
            }
        }
    }

    @Override
    public void visit(EnumConstantDeclaration n, DependencyGraph dependencyGraph) {
        // Dependencies from Delegation and Accessed Fields
        handleDelegationFieldsAndAnnotations(n, dependencyGraph);
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, DependencyGraph dependencyGraph) {
        // Dependencies from Delegation and Accessed Fields
        handleDelegationFieldsAndAnnotations(n, dependencyGraph);
    }

    @Override
    public void visit(InitializerDeclaration n, DependencyGraph dependencyGraph) {
        // Dependencies from Delegation and Accessed Fields
        handleDelegationFieldsAndAnnotations(n, dependencyGraph);
    }

    //##################################################################################################################
    // Auxiliary methods

    private void handleDelegationFieldsAndAnnotations(Node n, DependencyGraph dependencyGraph) {
        String node = lookupNode(n, dependencyGraph);

        // Dependencies from Delegation
        processDelegation(dependencyGraph, node, DelegationIdentifierVisitor.identifyDependencies(n));

        // Dependencies from accessed fields
        processFieldAccess(dependencyGraph, node, FieldAccessIdentifierVisitor.identifyDependencies(n));

        // Dependencies from annotations
        processAnnotations(dependencyGraph, node, AnnotationIdentifierVisitor.identifyDependencies(n));
    }

    private TypeDeclaration<?> getContainingTypeDeclaration(BodyDeclaration<?> bodyDeclaration) {
        Optional<Node> maybeParentNode = bodyDeclaration.getParentNode();
        while (maybeParentNode.isPresent()) {
            Node node = maybeParentNode.get();
            // RecordDeclaration is currently subject to change and not supported
            if (node instanceof TypeDeclaration<?> && !(node instanceof RecordDeclaration)) {
                return ((TypeDeclaration<?>) node);
            }
            maybeParentNode = node.getParentNode();
        }
        return null;
    }
}
