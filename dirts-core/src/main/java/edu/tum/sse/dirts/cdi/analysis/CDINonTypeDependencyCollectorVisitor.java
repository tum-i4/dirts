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
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.NonTypeDependencyCollector;
import edu.tum.sse.dirts.cdi.util.CDIBean;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.alternatives.QuadAlternative;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.*;
import static java.util.logging.Level.FINE;

public class CDINonTypeDependencyCollectorVisitor
        extends CDIDependencyCollectorVisitor<BodyDeclaration<?>>
        implements NonTypeDependencyCollector {


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

                String typeName = lookup(typeDeclaration);
                if (alternatives.contains(typeName)) {
                    String toNode = CDIUtil.lookupXMlAlternativeName(lookup(typeDeclaration));
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
                }

                for (ResolvedConstructorDeclaration constructor : typeDeclaration.getConstructors()) {
                    String toNode = lookup(constructor);
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
                }
            } else if (source.isSecondOption()) {
                Set<ResolvedMethodDeclaration> resolvedMethodDeclarations = source.getAsSecondOption();
                for (ResolvedMethodDeclaration resolvedMethodDeclaration : resolvedMethodDeclarations) {
                    String toNode = lookup(resolvedMethodDeclaration);
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
                }
            } else if (source.isThirdOption()) {
                ResolvedFieldDeclaration resolvedFieldDeclaration = source.getAsThirdOption();
                String toNode = lookup(resolvedFieldDeclaration.declaringType(), resolvedFieldDeclaration);
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
            } else {
                ResolvedValueDeclaration resolvedValueDeclaration = source.getAsFourthOption();
                if (resolvedValueDeclaration.isField()) {
                    String toNode = lookup(resolvedValueDeclaration.asField().declaringType(), resolvedValueDeclaration.asField());
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
                }
            }
        }
    }

    //##################################################################################################################
    // Visitor pattern - Methods inherited from NonTypeDependencyCollector

    @Override
    public void visit(MethodDeclaration n, DependencyGraph dependencyGraph) {
        if (CDIUtil.isAlternativeNode(n)
                && CDIUtil.isProducerNode(n)) {
            // Add edge to potential entry from alternatives in beans.xml
            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();
                String declaringType = lookup(resolvedMethodDeclaration.declaringType());
                if (alternatives.contains(declaringType)) {
                    String toNode = CDIUtil.lookupXMlAlternativeName(declaringType);
                    String fromNode = lookupAnnotationsNode(lookup(resolvedMethodDeclaration));
                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.DI_CDI);
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        visit(((CallableDeclaration<?>) n), dependencyGraph);
    }

    @Override
    public void visit(ConstructorDeclaration n, DependencyGraph dependencyGraph) {
        visit(((CallableDeclaration<?>) n), dependencyGraph);
    }

    private void visit(CallableDeclaration<?> n, DependencyGraph dependencyGraph) {
        Collection<CDIBean> candidates = new HashSet<>();

        // Constructor or setter injection
        if (CDIUtil.isInjected(n)) {
            for (Parameter parameter : n.getParameters()) {
                try {
                    ResolvedType resolvedType = parameter.getType().resolve();
                    getCandidates(candidates, n, resolvedType);
                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        // Injection through Instance
        handleSelect(candidates, n);

        processBeans(dependencyGraph, n, candidates);
    }

    @Override
    public void visit(FieldDeclaration n, DependencyGraph dependencyGraph) {
        if (n.getVariables().size() == 1
                && CDIUtil.isAlternativeNode(n)
                && CDIUtil.isProducerNode(n)) {
            // Add edge to potential entry from alternatives in beans.xml
            try {
                ResolvedFieldDeclaration resolvedFieldDeclaration = n.resolve();
                ResolvedTypeDeclaration declaringType = resolvedFieldDeclaration.declaringType();
                String declaringTypeName = lookup(declaringType);
                if (alternatives.contains(declaringTypeName)) {
                    String toNode = CDIUtil.lookupXMlAlternativeName(declaringTypeName);
                    String fromNode = lookupAnnotationsNode(lookup(declaringType, resolvedFieldDeclaration));
                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.DI_CDI);
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        Collection<CDIBean> candidates = new HashSet<>();

        // Field injection
        if (CDIUtil.isInjected(n)) {
            try {
                ResolvedType resolvedType = n.getCommonType().resolve();
                getCandidates(candidates, n, resolvedType);
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // Injection through Instance
        handleSelect(candidates, n);

        processBeans(dependencyGraph, n, candidates);
    }

    @Override
    public void visit(EnumConstantDeclaration n, DependencyGraph dependencyGraph) {
        Collection<CDIBean> candidates = new HashSet<>();

        // Injection through Instance
        handleSelect(candidates, n);

        processBeans(dependencyGraph, n, candidates);
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, DependencyGraph dependencyGraph) {
        Collection<CDIBean> candidates = new HashSet<>();

        // Injection through Instance
        handleSelect(candidates, n);

        processBeans(dependencyGraph, n, candidates);
    }

    @Override
    public void visit(InitializerDeclaration n, DependencyGraph dependencyGraph) {
        Collection<CDIBean> candidates = new HashSet<>();

        // Injection through Instance
        handleSelect(candidates, n);

        processBeans(dependencyGraph, n, candidates);
    }
}
