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
package edu.tum.sse.dirts.guice.analysis;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.NonTypeDependencyCollector;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.guice.analysis.identifiers.GetInstanceIdentifierVisitor.collectGetInstanceMethodCalls;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;

public class GuiceNonTypeDependencyCollectorVisitor
        extends GuiceDependencyCollectorVisitor<ResolvedMethodLikeDeclaration>
        implements NonTypeDependencyCollector {

    public GuiceNonTypeDependencyCollectorVisitor(BeanStorage<GuiceBinding> bindingsStorage) {
        super(bindingsStorage);
    }

    //##################################################################################################################
    // Visitor pattern - auxiliary visitor methods (used to set up things before)

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));
    }

    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, dependencyGraph));
    }

    //##################################################################################################################
    // Methods inherited from GuiceDependencyCollectorVisitor

    @Override
    protected void processBindings(DependencyGraph dependencyGraph, BodyDeclaration<?> n, Set<GuiceBinding> bindings) {
        String node = lookupNode(n, dependencyGraph);
        for (GuiceBinding binding : bindings) {
            TriAlternative<ResolvedMethodLikeDeclaration,
                    ResolvedReferenceTypeDeclaration,
                    Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>>> source = binding.getSource();

            String toNode;
            if (source.isFirstOption()) {
                ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration = source.getAsFirstOption();
                toNode = lookup(resolvedMethodLikeDeclaration);
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_GUICE);
            } else if (source.isSecondOption()) {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = source.getAsSecondOption();

                for (ResolvedConstructorDeclaration constructor : resolvedReferenceTypeDeclaration.getConstructors()) {
                    toNode = lookup(constructor);
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_GUICE);
                }
            } else {
                Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>> pair = source.getAsThirdOption();
                ResolvedMethodDeclaration methodWhereBound = pair.getFirst();
                Set<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations = pair.getSecond();

                // Add edge to the method where the binding was created
                String toMethodNode = lookup(methodWhereBound);
                dependencyGraph.addEdge(node, toMethodNode, EdgeType.DI_GUICE);

                for (ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypeDeclarations) {
                    for (ResolvedConstructorDeclaration constructor : resolvedReferenceTypeDeclaration.getConstructors()) {
                        toNode = lookup(constructor);
                        dependencyGraph.addEdge(node, toNode, EdgeType.DI_GUICE);
                    }
                }
            }
        }
    }

    //##################################################################################################################
    // Visitor pattern -Methods inherited from NonTypeDependencyCollector

    @Override
    public void visit(MethodDeclaration n, DependencyGraph dependencyGraph) {
        visit(n.asCallableDeclaration(), dependencyGraph);
    }

    @Override
    public void visit(ConstructorDeclaration n, DependencyGraph dependencyGraph) {
        visit(n.asCallableDeclaration(), dependencyGraph);
    }

    private void visit(CallableDeclaration<?> n, DependencyGraph dependencyGraph) {
        Set<GuiceBinding> candidateMethods = new HashSet<>();

        // Constructor or method injection
        if (isInjected(n)) {
            for (Parameter parameter : n.getParameters()) {
                try {
                    ResolvedType injectedType = parameter.getType().resolve();
                    getCandidates(injectedType, GuiceUtil.findName(parameter).orElse(null), GuiceUtil.findQualifiers(n), candidateMethods);
                } catch (RuntimeException ignored) {
                }
            }
        }

        handleGetInstance(candidateMethods, collectGetInstanceMethodCalls(n));

        if (!candidateMethods.isEmpty())
            processBindings(dependencyGraph, n, candidateMethods);
    }

    @Override
    public void visit(FieldDeclaration n, DependencyGraph dependencyGraph) {
        Set<GuiceBinding> candidateMethods = new HashSet<>();

        // field injection
        if (isInjected(n)) {
            try {
                ResolvedType injectedType = n.getElementType().resolve();
                getCandidates(injectedType, GuiceUtil.findName(n).orElse(null), GuiceUtil.findQualifiers(n), candidateMethods);
            } catch (RuntimeException ignored) {
            }
        }

        handleGetInstance(candidateMethods, collectGetInstanceMethodCalls(n));

        if (!candidateMethods.isEmpty())
            processBindings(dependencyGraph, n, candidateMethods);
    }

    @Override
    public void visit(EnumConstantDeclaration n, DependencyGraph dependencyGraph) {
        Set<GuiceBinding> candidateMethods = new HashSet<>();

        handleGetInstance(candidateMethods, collectGetInstanceMethodCalls(n));

        if (!candidateMethods.isEmpty())
            processBindings(dependencyGraph, n, candidateMethods);
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, DependencyGraph dependencyGraph) {
        Set<GuiceBinding> candidateMethods = new HashSet<>();

        handleGetInstance(candidateMethods, collectGetInstanceMethodCalls(n));

        if (!candidateMethods.isEmpty())
            processBindings(dependencyGraph, n, candidateMethods);
    }

    @Override
    public void visit(InitializerDeclaration n, DependencyGraph dependencyGraph) {
        Set<GuiceBinding> candidateMethods = new HashSet<>();
        handleGetInstance(candidateMethods, collectGetInstanceMethodCalls(n));

        if (!candidateMethods.isEmpty())
            processBindings(dependencyGraph, n, candidateMethods);
    }
}