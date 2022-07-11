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
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.TypeDependencyCollector;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.guice.analysis.identifiers.GetInstanceIdentifierVisitor.collectGetInstanceMethodCalls;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;

public class GuiceTypeDependencyCollectorVisitor
        extends GuiceDependencyCollectorVisitor<TypeDeclaration<?>>
        implements TypeDependencyCollector {

    public GuiceTypeDependencyCollectorVisitor(BeanStorage<GuiceBinding> bindingsStorage) {
        super(bindingsStorage);
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
                toNode = lookup(resolvedMethodLikeDeclaration.declaringType());
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_GUICE);
            } else if (source.isSecondOption()) {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = source.getAsSecondOption();
                toNode = lookup(resolvedReferenceTypeDeclaration);
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_GUICE);
            } else {
                Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>> pair = source.getAsThirdOption();
                ResolvedMethodDeclaration methodWhereBound = pair.getFirst();
                Set<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations = pair.getSecond();

                // Add edge to the class containing the method where the binding was created
                if (methodWhereBound != null) {
                    String toMethodNode;
                    toMethodNode = lookup(methodWhereBound.declaringType());
                    dependencyGraph.addEdge(node, toMethodNode, EdgeType.DI_GUICE);
                }
                for (ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypeDeclarations) {
                    toNode = lookup(resolvedReferenceTypeDeclaration);
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_GUICE);
                }
            }
        }
    }

    //##################################################################################################################
    // Visitor pattern - Methods inherited from TypeDependencyCollector

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        visit(n.asTypeDeclaration(), dependencyGraph);
    }

    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        visit(n.asTypeDeclaration(), dependencyGraph);
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        visit(n.asTypeDeclaration(), dependencyGraph);
    }

    private void visit(TypeDeclaration<?> n, DependencyGraph dependencyGraph) {
        Set<GuiceBinding> candidateTypes = new HashSet<>();

        List<BodyDeclaration<?>> injectedBodyDeclarations = n.getMembers().stream().filter(this::isInjected).collect(Collectors.toList());
        for (BodyDeclaration<?> injectedBodyDeclaration : injectedBodyDeclarations) {

            if (injectedBodyDeclaration.isFieldDeclaration()) {
                // field injection
                FieldDeclaration injectedField = injectedBodyDeclaration.asFieldDeclaration();
                try {
                    ResolvedType injectedType = injectedField.getElementType().resolve();
                    getCandidates(injectedType, GuiceUtil.findName(injectedField).orElse(null), GuiceUtil.findQualifiers(injectedField), candidateTypes);
                } catch (UnsolvedSymbolException ignored) {
                }
            } else if (injectedBodyDeclaration.isConstructorDeclaration() || injectedBodyDeclaration.isMethodDeclaration()) {
                // constructor or method injection
                CallableDeclaration<?> injectedCallable = (CallableDeclaration<?>) injectedBodyDeclaration;
                for (Parameter parameter : injectedCallable.getParameters()) {
                    try {
                        ResolvedType injectedType = parameter.getType().resolve();
                        getCandidates(injectedType,GuiceUtil.findName(parameter).orElse(null), GuiceUtil.findQualifiers(parameter), candidateTypes);
                    } catch (UnsolvedSymbolException ignored) {
                    }
                }
            }
        }

        handleGetInstance(candidateTypes, collectGetInstanceMethodCalls(n));

        if (!candidateTypes.isEmpty())
            processBindings(dependencyGraph, n, candidateTypes);
    }
}
