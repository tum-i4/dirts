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
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.TypeDependencyCollector;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;
import edu.tum.sse.dirts.spring.util.SpringNames;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor.getNameFromQualifier;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupNode;

public class SpringTypeDependencyCollectorVisitor
        extends SpringDependencyCollectorVisitor
        implements TypeDependencyCollector {

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

            String toNode;
            if (dependsOn.isFirstOption()) {
                XMLBeanDefinition referencedBean = dependsOn.getAsFirstOption();
                toNode = SpringNames.lookup(referencedBean);
            } else if (dependsOn.isSecondOption()) {
                ResolvedMethodDeclaration method = dependsOn.getAsSecondOption();
                toNode = lookup(method.declaringType());
            } else {
                ResolvedReferenceTypeDeclaration typeDeclaration = dependsOn.getAsThirdOption();
                toNode = lookup(typeDeclaration);
            }
            dependencyGraph.addEdge(node, toNode, EdgeType.DI_SPRING);
        }
    }

    //##################################################################################################################
    // Methods inherited from TypeDependencyCollector

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
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
        Collection<SpringBean> candidates = new HashSet<>();

        List<BodyDeclaration<?>> injectedBodyDeclarations = n.getMembers().stream()
                .filter(this::isInjected)
                .collect(Collectors.toList());
        for (BodyDeclaration<?> injectedBodyDeclaration : injectedBodyDeclarations) {

            if (injectedBodyDeclaration.isFieldDeclaration()) {
                // field injection
                FieldDeclaration injectedField = injectedBodyDeclaration.asFieldDeclaration();
                String name = getNameFromQualifier(injectedBodyDeclaration, "Qualifier");
                try {
                    ResolvedType elementType = injectedField.getCommonType().resolve();
                    getBeanCandidates(elementType, name, candidates);
                } catch (RuntimeException e) {
                    if (Control.DEBUG)
                        System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            } else if (injectedBodyDeclaration.isConstructorDeclaration() || injectedBodyDeclaration.isMethodDeclaration()) {
                // constructor or method injection
                CallableDeclaration<?> injectedCallable = (CallableDeclaration<?>) injectedBodyDeclaration;
                for (Parameter parameter : injectedCallable.getParameters()) {
                    String name = getNameFromQualifier(parameter, "Qualifier");
                    try {
                        ResolvedType elementType = parameter.getType().resolve();
                        getBeanCandidates(elementType, name, candidates);
                    } catch (RuntimeException e) {
                        if (Control.DEBUG)
                            System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                }
            }
        }

        // Injection through BeanFactory
        handleGetBean(candidates, n);

        addEdges(dependencyGraph, candidates, n);
    }
}
