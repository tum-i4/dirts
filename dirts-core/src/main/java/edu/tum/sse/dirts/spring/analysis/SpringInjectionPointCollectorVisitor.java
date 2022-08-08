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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.di.InjectionPointCollector;
import edu.tum.sse.dirts.analysis.di.InjectionPointStorage;
import edu.tum.sse.dirts.spring.util.SpringUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor.getNameFromQualifier;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;

/**
 * Collects dependencies from beans/code entities to code entities based on @Bean, @Configuration, xml beans @Autowire and getBean(...)
 */
public abstract class SpringInjectionPointCollectorVisitor<P extends BodyDeclaration<?>>
        extends AbstractTruncatedVisitor<InjectionPointStorage>
        implements InjectionPointCollector<P> {

    //##################################################################################################################
    // Methods specified by DependencyCollector

    public void collectInjectionPoints(Collection<TypeDeclaration<?>> ts, InjectionPointStorage injectionPointStorage) {
        for (TypeDeclaration<?> t : ts) {
            t.accept(this, injectionPointStorage);
        }
    }

    //##################################################################################################################
    // Methods used by subclasses

    protected boolean isInjected(BodyDeclaration<?> bodyDeclaration) {
        return SpringUtil.isInjectNode(bodyDeclaration);
    }

    protected <T extends Node & NodeWithAnnotations<?>, R> void handleInjectionVariable(
            InjectionPointStorage injectionPoints,
            R node,
            Function<R, String> getterNodeName,
            T injectionVariable,
            Function<T, ResolvedType> getterType) {

        String nodeName = null;
        ResolvedType type = null;

        // Name
        String name = getNameFromQualifier(injectionVariable, "Qualifier");

        // No Qualifiers for Spring

        try {
            // Type
            type = getterType.apply(injectionVariable);

            nodeName = getterNodeName.apply(node);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }

        if (nodeName != null) {
            // add injectionPoints
            injectionPoints.addInjectionPoint(nodeName, type, name, Set.of());

            if (type != null && type.isReferenceType()) {
                checkCollectionInjection(injectionPoints, nodeName, type.asReferenceType(), name, Set.of());
            }
        }
    }

    protected <R> void handleGetBean(
            InjectionPointStorage injectionPointStorage,
            R node,
            Function<R, String> getterNodeName,
            Set<Pair<String, ResolvedType>> pairs) {

        if (!pairs.isEmpty()) {

            try {
                String nodeName = getterNodeName.apply(node);

                // Injection through BeanFactory
                for (Pair<String, ResolvedType> nameAndType : pairs) {
                    String name = nameAndType.getFirst();
                    ResolvedType type = nameAndType.getSecond();

                    injectionPointStorage.addInjectionPoint(nodeName, type, name, Set.of());

                    if (type.isReferenceType()) {
                        checkCollectionInjection(injectionPointStorage, nodeName, type.asReferenceType(), null, Set.of());
                    }
                }

            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    protected void checkCollectionInjection(InjectionPointStorage injectionPointStorage,
                                            String outerName,
                                            ResolvedReferenceType injectedReferenceType,
                                            String name,
                                            Set<String> qualifiers) {

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

                injectionPointStorage.addInjectionPoint(outerName, containedType, name, qualifiers);
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
                    injectionPointStorage.addInjectionPoint(outerName, valueType, name, qualifiers);
                }
            }
        }
    }
}