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
package edu.tum.sse.dirts.spring.analysis;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.NonTypeDependencyCollector;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;
import edu.tum.sse.dirts.spring.util.SpringNames;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;

import java.util.Collection;
import java.util.HashSet;

import static edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor.getNameFromQualifier;
import static edu.tum.sse.dirts.util.naming_scheme.Names.*;
import static java.util.logging.Level.FINE;

public class SpringNonTypeDependencyCollectorVisitor
        extends SpringDependencyCollectorVisitor<BodyDeclaration<?>>
        implements NonTypeDependencyCollector {

    //##################################################################################################################
    // auxiliary visitor methods (used to set up things before)

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
    // Methods inherited from SpringDependencyCollectorVisitor

    @Override
    protected void processBeanMethods(DependencyGraph dependencyGraph,
                                      BodyDeclaration<?> n,
                                      Collection<SpringBean> candidateBeanMethods) {
        String node = lookupNode(n, dependencyGraph);
        for (SpringBean bean : candidateBeanMethods) {
            TriAlternative<XMLBeanDefinition, ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration> dependsOn =
                    bean.getDefinition();

            if (dependsOn.isFirstOption()) {
                XMLBeanDefinition referencedBean = dependsOn.getAsFirstOption();
                String toNode = SpringNames.lookup(referencedBean);
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_SPRING);
            } else if (dependsOn.isSecondOption()) {
                ResolvedMethodDeclaration method = dependsOn.getAsSecondOption();
                String toNode = lookup(method);
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_SPRING);
            } else {
                ResolvedReferenceTypeDeclaration typeDeclaration = dependsOn.getAsThirdOption();

                // Add edge to node of the annotations of this class to account for changes in annotations of the class
                String toDefinitionNode = lookupAnnotationsNode(lookup(typeDeclaration));
                dependencyGraph.addEdge(node, toDefinitionNode, EdgeType.DI_SPRING);

                for (ResolvedConstructorDeclaration constructor : typeDeclaration.getConstructors()) {
                    String toNode = lookup(constructor);
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_SPRING);
                }
            }
        }
    }

    //##################################################################################################################
    // Methods inherited from NonTypeDependencyCollector

    @Override
    public void visit(MethodDeclaration n, DependencyGraph dependencyGraph) {
        Collection<SpringBean> candidates = new HashSet<>();

        // Setter injection
        if (isInjected(n)) {
            for (Parameter parameter : n.getParameters()) {
                String name = getNameFromQualifier(parameter, "Qualifier");
                try {
                    ResolvedType elementType = parameter.getType().resolve();
                    getBeanCandidates(elementType, name, candidates);
                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        // Injection through BeanFactory
        handleGetBean(candidates, n);

        addEdges(dependencyGraph, candidates, n);
    }

    @Override
    public void visit(ConstructorDeclaration n, DependencyGraph dependencyGraph) {
        Collection<SpringBean> candidates = new HashSet<>();

        // Constructor injection
        if (isInjected(n)) {
            for (Parameter parameter : n.getParameters()) {
                String name = getNameFromQualifier(parameter, "Qualifier");
                try {
                    ResolvedType elementType = parameter.getType().resolve();
                    getBeanCandidates(elementType, name, candidates);
                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        // Injection through BeanFactory
        handleGetBean(candidates, n);

        addEdges(dependencyGraph, candidates, n);
    }

    @Override
    public void visit(FieldDeclaration n, DependencyGraph dependencyGraph) {
        Collection<SpringBean> candidates = new HashSet<>();

        // Field injection
        if (isInjected(n)) {
            String name = getNameFromQualifier(n, "Qualifier");
            try {
                ResolvedType elementType = n.getCommonType().resolve();
                getBeanCandidates(elementType, name, candidates);
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // Injection through BeanFactory
        handleGetBean(candidates, n);

        addEdges(dependencyGraph, candidates, n);
    }

    @Override
    public void visit(EnumConstantDeclaration n, DependencyGraph dependencyGraph) {
        Collection<SpringBean> candidates = new HashSet<>();

        // Injection through BeanFactory
        handleGetBean(candidates, n);

        addEdges(dependencyGraph, candidates, n);
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, DependencyGraph dependencyGraph) {
        Collection<SpringBean> candidates = new HashSet<>();

        // Injection through BeanFactory
        handleGetBean(candidates, n);

        addEdges(dependencyGraph, candidates, n);
    }

    @Override
    public void visit(InitializerDeclaration n, DependencyGraph dependencyGraph) {
        Collection<SpringBean> candidates = new HashSet<>();

        // Injection through BeanFactory
        handleGetBean(candidates, n);

        addEdges(dependencyGraph, candidates, n);
    }
}
