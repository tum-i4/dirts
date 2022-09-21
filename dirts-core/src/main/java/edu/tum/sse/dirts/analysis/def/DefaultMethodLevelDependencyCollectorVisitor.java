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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.*;
import edu.tum.sse.dirts.analysis.MethodLevelDependencyCollector;
import edu.tum.sse.dirts.analysis.def.identifiers.AnnotationIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.methodlevel.DelegationIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.methodlevel.FieldAccessIdentifierVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.methodlevel.InheritanceIdentifierVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.*;

import static edu.tum.sse.dirts.graph.EdgeType.*;
import static edu.tum.sse.dirts.graph.EdgeType.ANNOTATION;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;

/**
 * Collects dependencies based on Delegation, MemberAccess, Inheritance and Annotations (if specified)
 * for method level nodes
 */
public class DefaultMethodLevelDependencyCollectorVisitor
        extends DefaultDependencyCollectorVisitor<BodyDeclaration<?>>
        implements MethodLevelDependencyCollector {

    private IdentityHashMap<TypeDeclaration<?>, InheritanceIdentifierVisitor> inheritanceIdentifierVisitorMap;

    public void init(Blackboard<?> blackboard) {
        this.inheritanceIdentifierVisitorMap = blackboard.getInheritanceIdentifierVisitorMap();
    }

    //##################################################################################################################
    // Visitor pattern - Auxiliary methods (used to set up things before)

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));
    }


    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        inheritanceIdentifierVisitorMap.put(n, new InheritanceIdentifierVisitor(n));
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));
    }


    //##################################################################################################################
    // Methods that add edges

    private void processFieldAccess(DependencyGraph dependencyGraph,
                                    String node,
                                    Pair<Collection<ResolvedValueDeclaration>,
                                            Collection<ResolvedValueDeclaration>> resolvedFields) {
        Collection<ResolvedValueDeclaration> resolvedAccessedFields = resolvedFields.getFirst();
        Collection<ResolvedValueDeclaration> resolvedAssignedFields = resolvedFields.getSecond();

        for (ResolvedValueDeclaration resolvedAccessedField : resolvedAccessedFields) {
            if (resolvedAccessedField.isField()) {
                // access of Fields
                String toNode = lookup(resolvedAccessedField.asField().declaringType(), resolvedAccessedField.asField());
                dependencyGraph.addEdge(node, toNode, FIELD_ACCESS);
            } else if (resolvedAccessedField.isEnumConstant()) {
                // usage of EnumConstants
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = resolvedAccessedField.getType()
                        .asReferenceType().getTypeDeclaration();
                if (maybeTypeDeclaration.isPresent()) {
                    String toNode = lookup(maybeTypeDeclaration.get(), resolvedAccessedField.asEnumConstant());
                    dependencyGraph.addEdge(node, toNode, FIELD_ACCESS);
                }
            }
        }
        for (ResolvedValueDeclaration resolvedAssignedField : resolvedAssignedFields) {
            if (resolvedAssignedField.isField()) {
                // assignment of Fields
                String fromNode = lookup(resolvedAssignedField.asField().declaringType(),
                        resolvedAssignedField.asField());
                dependencyGraph.addEdge(fromNode, node, FIELD_ASSIGNMENT);
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
                dependencyGraph.addEdge(node, toNode, DELEGATION);
            }
        }
    }

    private void processInheritance(DependencyGraph dependencyGraph,
                                    String node,
                                    Collection<ResolvedMethodLikeDeclaration> resolvedOverriddenMethods) {
        for (ResolvedMethodLikeDeclaration resolvedOverriddenMethod : resolvedOverriddenMethods) {
            if (!resolvedOverriddenMethod.declaringType().isAnonymousClass()) {
                String fromNode = lookup(resolvedOverriddenMethod);
                dependencyGraph.addEdge(fromNode, node, INHERITANCE);
            }
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
    // Visitor pattern - Methods inherited from MethodLevelDependencyCollector

    @Override
    public void visit(MethodDeclaration n, DependencyGraph dependencyGraph) {
        String node = lookupNode(n, dependencyGraph);

        // Dependencies from Delegation and Accessed Fields
        handleDelegationFieldsAndAnnotations(n, dependencyGraph);

        // Dependencies from Inheritance
        TypeDeclaration<?> containingTypeDeclaration = getContainingTypeDeclaration(n);
        if (containingTypeDeclaration != null) {
            InheritanceIdentifierVisitor inheritanceIdentifierVisitor =
                    this.inheritanceIdentifierVisitorMap.get(containingTypeDeclaration);
            if (inheritanceIdentifierVisitor != null) {
                Collection<ResolvedMethodLikeDeclaration> resolvedOverriddenMethods = new ArrayList<>();
                inheritanceIdentifierVisitor.visit(n, resolvedOverriddenMethods);
                processInheritance(dependencyGraph, node, resolvedOverriddenMethods);
            }
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, DependencyGraph dependencyGraph) {
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
                dependencyGraph.addEdge(node, declaratorNode, MULTIPLE_FIELD_DECL);
                dependencyGraph.addEdge(declaratorNode, node, MULTIPLE_FIELD_DECL);

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

        dependencyGraph.removeAllEdgesFrom(node, Set.of(DELEGATION, FIELD_ACCESS, ANNOTATION));

        // Dependencies from delegation
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
