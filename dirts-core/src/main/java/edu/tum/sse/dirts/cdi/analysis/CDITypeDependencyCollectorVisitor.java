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
package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.TypeDependencyCollector;
import edu.tum.sse.dirts.cdi.util.CDIBean;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.alternatives.QuadAlternative;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;
import static java.util.logging.Level.FINE;

public class CDITypeDependencyCollectorVisitor
        extends CDIDependencyCollectorVisitor<TypeDeclaration<?>>
        implements TypeDependencyCollector {

    //##################################################################################################################
    // Methods inherited from CDIDependencyCollectorVisitor

    @Override
    protected void processBeans(DependencyGraph dependencyGraph, BodyDeclaration<?> n, Collection<CDIBean> candidateBeans) {
        String node = lookupNode(n, dependencyGraph);
        for (CDIBean candidateBean : candidateBeans) {
            QuadAlternative<ResolvedReferenceTypeDeclaration,
                    Set<ResolvedMethodDeclaration>,
                    ResolvedFieldDeclaration,
                    ResolvedValueDeclaration> source = candidateBean.getSource();

            if (source.isFirstOption()) {
                ResolvedReferenceTypeDeclaration typeDeclaration = source.getAsFirstOption();
                String toNode = lookup(typeDeclaration);
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
            } else if (source.isSecondOption()) {
                Set<ResolvedMethodDeclaration> resolvedMethodDeclarations = source.getAsSecondOption();
                for (ResolvedMethodDeclaration resolvedMethodDeclaration : resolvedMethodDeclarations) {
                    String toNode = lookup(resolvedMethodDeclaration.declaringType());
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
                }
            } else if (source.isThirdOption()) {
                ResolvedFieldDeclaration resolvedFieldDeclaration = source.getAsThirdOption();
                String toNode = lookup(resolvedFieldDeclaration.declaringType());
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
            } else {
                ResolvedValueDeclaration resolvedValueDeclaration = source.getAsFourthOption();
                if (resolvedValueDeclaration.isField()) {
                    String toNode = lookup(resolvedValueDeclaration.asField().declaringType());
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
                }
            }
        }
    }

    //##################################################################################################################
    // Visitor pattern - Methods inherited from TypeDependencyCollector

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        if (CDIUtil.isAlternativeNode(n) ||
                n.getMethods().stream().anyMatch(m -> CDIUtil.isProducerNode(m) && CDIUtil.isAlternativeNode(m))) {
            // Add edge to potential entry from alternatives in beans.xml
            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                String fromNode = lookup(resolvedReferenceTypeDeclaration);
                if (alternatives.contains(fromNode)) {
                    String toNode = CDIUtil.lookupXMlAlternativeName(fromNode);
                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.DI_CDI);
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        collectDependencies(n, dependencyGraph);
    }

    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        collectDependencies(n, dependencyGraph);
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        collectDependencies(n, dependencyGraph);
    }

    //##################################################################################################################
    // Auxiliary methods

    private void collectDependencies(TypeDeclaration<?> n, DependencyGraph dependencyGraph) {
        Collection<CDIBean> candidates = new HashSet<>();

        List<BodyDeclaration<?>> injectedBodyDeclarations = n.getMembers().stream().filter(CDIUtil::isInjected).collect(Collectors.toList());
        for (BodyDeclaration<?> injectedBodyDeclaration : injectedBodyDeclarations) {

            if (injectedBodyDeclaration.isFieldDeclaration()) {
                // field injection
                FieldDeclaration injectedField = injectedBodyDeclaration.asFieldDeclaration();
                try {
                    ResolvedType resolvedType = injectedField.getCommonType().resolve();
                    getCandidates(candidates, injectedField, resolvedType);
                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }

            } else if (injectedBodyDeclaration.isConstructorDeclaration() || injectedBodyDeclaration.isMethodDeclaration()) {
                // constructor or method injection
                CallableDeclaration<?> injectedCallable = (CallableDeclaration<?>) injectedBodyDeclaration;
                for (Parameter parameter : injectedCallable.getParameters()) {
                    try {
                        ResolvedType resolvedType = parameter.getType().resolve();
                        getCandidates(candidates, parameter, resolvedType);
                    } catch (RuntimeException e) {
                        Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                }
            }
        }

        // Injection through Instance
        handleSelect(candidates, n);

        processBeans(dependencyGraph, n, candidates);
    }
}
