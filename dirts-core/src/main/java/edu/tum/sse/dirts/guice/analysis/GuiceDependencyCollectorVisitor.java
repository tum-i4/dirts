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

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.guice.analysis.identifiers.*;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.logging.Level.FINER;

public abstract class GuiceDependencyCollectorVisitor<P extends BodyDeclaration<?>> extends AbstractTruncatedVisitor<DependencyGraph>
        implements DependencyCollector<P> {

    //##################################################################################################################
    // Attributes

    private final BeanStorage<GuiceBinding> bindingsStorage;

    public GuiceDependencyCollectorVisitor(BeanStorage<GuiceBinding> bindingsStorage) {
        this.bindingsStorage = bindingsStorage;
    }

    //##################################################################################################################
    // Methods specified by DependencyCollector

    public void calculateDependencies(Collection<TypeDeclaration<?>> ts, DependencyGraph dependencyGraph) {
        // collect all bindings
        ProvidesIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        BindToIdentifier.identifyDependencies(ts, bindingsStorage);
        AutoBindSingletonIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ProviderIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        JustInTimeIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ImplementedByIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ProvidedByIdentifierVisitor.identifyDependencies(ts, bindingsStorage);

        for (TypeDeclaration<?> t : ts) {
            t.accept(this, dependencyGraph);
        }

        Log.log(FINER, "BEANS", "\n" + bindingsStorage.toString());
    }

    //##################################################################################################################
    // Methods required in subclasses

    protected abstract void processBindings(DependencyGraph arg, BodyDeclaration<?> n, Set<GuiceBinding> bindings);

    //##################################################################################################################
    // Methods used by subclasses

    protected boolean isInjected(BodyDeclaration<?> bodyDeclaration) {
        return GuiceUtil.isInjectNode(bodyDeclaration);
    }

    protected void getCandidates(ResolvedType injectedType,
                                 String name,
                                 Set<String> bindingAnnotations,
                                 Collection<GuiceBinding> candidates) {
        candidates.addAll(bindingsStorage.getBeansForTypeAndNameAndQualifiers(injectedType, name, bindingAnnotations));
        if (injectedType.isReferenceType()) {
            checkCollectionInjection(injectedType.asReferenceType(), candidates);
        }
    }

    protected void handleGetInstance(Set<GuiceBinding> candidateMethods, Set<ResolvedType> retrievedTypes) {
        // injection through getInstance()
        for (ResolvedType retrievedType : retrievedTypes) {
            getCandidates(retrievedType, null, null, candidateMethods);
        }
    }

    //##################################################################################################################
    // Auxiliary methods

    private void checkCollectionInjection(ResolvedReferenceType injectedReferenceType,
                                          Collection<GuiceBinding> candidates) {
        Optional<ResolvedReferenceTypeDeclaration> mayBeTypeDeclaration = injectedReferenceType.getTypeDeclaration();
        if (mayBeTypeDeclaration.isPresent()) {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = mayBeTypeDeclaration.get();
            List<ResolvedReferenceType> interfacesAncestors = resolvedReferenceTypeDeclaration
                    .getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList)
                    .stream().filter(a ->
                            a.getTypeDeclaration().map(ResolvedTypeDeclaration::isInterface).orElse(false))
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
                getCandidates(containedType, null, null, candidates);
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
                if (typeParameters.size() == 2) {
                    ResolvedType keyType = typeParameters.get(0);
                    ResolvedType valueType = typeParameters.get(1);
                    if (keyType.isReferenceType() &&
                            (keyType.asReferenceType().getQualifiedName().equals("java.lang.String")
                                    || keyType.asReferenceType().getQualifiedName().equals("java.lang.Class")
                                    || keyType.asReferenceType().getQualifiedName().equals("java.lang.Enum"))) {
                        getCandidates(valueType, null, null, candidates);
                    }
                }
            }
        }
    }
}
