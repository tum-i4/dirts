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

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.identifiers.SpringBeanMethodIdentifierVisitor;
import edu.tum.sse.dirts.spring.analysis.identifiers.SpringComponentIdentifierVisitor;
import edu.tum.sse.dirts.spring.analysis.identifiers.SpringGetBeanIdentifierVisitor;
import edu.tum.sse.dirts.spring.util.SpringUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Collects dependencies from beans/code entities to code entities based on @Bean, @Configuration, xml beans @Autowire and getBean(...)
 */
public abstract class SpringDependencyCollectorVisitor
        extends AbstractTruncatedVisitor<DependencyGraph>
        implements DependencyCollector {

    //##################################################################################################################
    // Attributes

    protected BeanStorage<SpringBean> beanStorage;

    //##################################################################################################################
    // Setters

    public void setBeanStorage(BeanStorage<SpringBean> beanStorage) {
        this.beanStorage = beanStorage;
    }

    //##################################################################################################################
    // Methods specified by DependencyCollector

    public void calculateDependencies(Collection<TypeDeclaration<?>> ts, DependencyGraph dependencyGraph) {
        // Collect all types annotated with @Component
        SpringComponentIdentifierVisitor.identifyDependencies(ts, beanStorage);

        // Collect all methods annotated with @Bean
        // Because of "lite mode" beans can be declared in all classes, not only in those annotated with @Configuration
        SpringBeanMethodIdentifierVisitor.identifyDependencies(ts, beanStorage);

        for (TypeDeclaration<?> t : ts) {
            t.accept(this, dependencyGraph);
        }

        if (Control.PRINT_BEANS) {
            System.out.println("[BEANS]");
            System.out.println(beanStorage.toString());
        }
    }

    //##################################################################################################################
    // Methods required in subclasses

    protected abstract void processBeanMethods(DependencyGraph arg,
                                               BodyDeclaration<?> n,
                                               Collection<SpringBean> candidateBeanMethods);

    //##################################################################################################################
    // Methods used by subclasses

    protected boolean isInjected(BodyDeclaration<?> bodyDeclaration) {
        return SpringUtil.isInjectNode(bodyDeclaration);
    }

    protected void getBeanCandidates(ResolvedType type, String name, Collection<SpringBean> candidates) {

        candidates.addAll(beanStorage.getBeansForTypeAndName(type, name));
        if (type != null && type.isReferenceType()) {
            ResolvedReferenceType resolvedReferenceType = type.asReferenceType();
            checkCollectionInjection(resolvedReferenceType, candidates);
        }
    }

    protected void handleGetBean(Collection<SpringBean> candidates, BodyDeclaration<?> n) {
        Set<Pair<String, ResolvedType>> pairs = SpringGetBeanIdentifierVisitor.collectGetBeanMethodCalls(n);
        // Injection through BeanFactory
        for (Pair<String, ResolvedType> nameAndType : pairs) {
            String name = nameAndType.getFirst();
            ResolvedType type = nameAndType.getSecond();

            getBeanCandidates(type, name, candidates);
        }
    }

    protected void checkCollectionInjection(ResolvedReferenceType injectedReferenceType, Collection<SpringBean> candidates) {

        Optional<ResolvedReferenceTypeDeclaration> mayBeTypeDeclaration = injectedReferenceType.getTypeDeclaration();
        if (mayBeTypeDeclaration.isPresent()) {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = mayBeTypeDeclaration.get();
            List<ResolvedReferenceType> interfacesAncestors = resolvedReferenceTypeDeclaration
                    .getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList)
                    .stream().filter(a -> a.getTypeDeclaration().map(ResolvedTypeDeclaration::isInterface)
                            .orElse(false))
                    // We have to replace the generic type parameters by the actual type parameters
                    // Otherwise, comparing the type will fail
                    .map(a -> injectedReferenceType.typeParametersMap().replaceAll(a).asReferenceType())
                    .collect(Collectors.toList());

            // Collection
            Optional<ResolvedReferenceType> collectionTypeAncestor = interfacesAncestors.stream()
                    .filter(a -> a.getQualifiedName().equals("java.util.Collection"))
                    .findAny();
            if (collectionTypeAncestor.isPresent()) {
                ResolvedReferenceType collectionType = collectionTypeAncestor.get();
                List<ResolvedType> typeParameters = collectionType
                        .getTypeParametersMap()
                        .stream().map(p -> p.b)
                        .collect(Collectors.toList());
                ResolvedType containedType = typeParameters.get(0);
                candidates.addAll(beanStorage.getBeansForType(containedType));
            }

            // Map
            Optional<ResolvedReferenceType> mapTypeAncestor = interfacesAncestors.stream()
                    .filter(a -> a.getQualifiedName().equals("java.util.Map"))
                    .findAny();
            if (mapTypeAncestor.isPresent()) {
                ResolvedReferenceType mapType = mapTypeAncestor.get();
                List<ResolvedType> typeParameters = mapType
                        .getTypeParametersMap()
                        .stream().map(p -> p.b)
                        .collect(Collectors.toList());
                ResolvedType keyType = typeParameters.get(0);
                ResolvedType valueType = typeParameters.get(1);
                if (keyType.isReferenceType()
                        && keyType.asReferenceType().getQualifiedName().equals("java.lang.String")) {
                    candidates.addAll(beanStorage.getBeansForType(valueType));
                }
            }
        }
    }

    protected void addEdges(DependencyGraph arg, Collection<SpringBean> candidates, BodyDeclaration<?> n) {
        if (!candidates.isEmpty()) {
            processBeanMethods(arg, n, candidates);
        }
    }

}
