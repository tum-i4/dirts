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
package edu.tum.sse.dirts.cdi.analysis.identifiers;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.cdi.util.CDIBean;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.*;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;

/**
 * Identifies methods annotated with @Produces and @Disposes
 * <p>
 * Rationale:
 * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
 * "Chapter 3. Programming model" - Subchapter "3.2 Producer methods" and Subchapter "3.4 Disposer methods"
 */
public class ProducerMethodIdentifierVisitor extends AbstractIdentifierVisitor<
        Pair<BeanStorage<CDIBean>, Map<String, Set<ResolvedMethodDeclaration>>>
        > {

    //##################################################################################################################
    // Constants

    private static final DisposerMethodIdentifierVisitor disposerMethodIdentifierVisitor = new DisposerMethodIdentifierVisitor();

    //##################################################################################################################
    // Singleton pattern

    private static final ProducerMethodIdentifierVisitor singleton = new ProducerMethodIdentifierVisitor();

    private ProducerMethodIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations, BeanStorage<CDIBean> arg) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            Map<String, Set<ResolvedMethodDeclaration>> disposerMethodsMap = new HashMap<>();
            Pair<BeanStorage<CDIBean>, Map<String, Set<ResolvedMethodDeclaration>>> beanStorageMapPair =
                    new Pair<>(arg, disposerMethodsMap);
            typeDeclaration.getMembers().forEach(m -> m.accept(disposerMethodIdentifierVisitor, disposerMethodsMap));
            typeDeclaration.getMembers().forEach(m -> m.accept(singleton, beanStorageMapPair));
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(MethodDeclaration n,
                      Pair<BeanStorage<CDIBean>, Map<String, Set<ResolvedMethodDeclaration>>> arg) {
        if (CDIUtil.isProducerNode(n)) {
            BeanStorage<CDIBean> beanStorage = arg.getFirst();
            Map<String, Set<ResolvedMethodDeclaration>> disposerMethodsMap = arg.getSecond();

            try {
                ResolvedType returnType = n.getType().resolve();
                Set<ResolvedMethodDeclaration> disposerMethods = new HashSet<>();

                String key = lookup(returnType);
                if (disposerMethodsMap.containsKey(key))
                    disposerMethods.addAll(disposerMethodsMap.get(key));

                CDIBean newBean = new CDIBean(n.resolve(), disposerMethods);

                try {
                    //**************************************************************************************************
                    // By type
                    Set<ResolvedType> beanTypes = CDIUtil.findBeanTypes(returnType);
                    for (ResolvedType beanType : beanTypes) {
                        beanStorage.addBeanByType(beanType, newBean);
                    }

                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }

                //******************************************************************************************************
                // By name
                String name = CDIUtil.findName(n);
                if (name != null)
                    beanStorage.addBeanByName(name, newBean);

                //******************************************************************************************************
                // By qualifiers
                Set<String> qualifiers = CDIUtil.findQualifiers(n);
                for (String qualifier : qualifiers) {
                    beanStorage.addBeanByQualifier(qualifier, newBean);
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    //##################################################################################################################
    // Auxiliary classes

    private static class DisposerMethodIdentifierVisitor extends AbstractIdentifierVisitor<
            Map<String, Set<ResolvedMethodDeclaration>>
            > {

        @Override
        public void visit(MethodDeclaration n, Map<String, Set<ResolvedMethodDeclaration>> arg) {
            if (CDIUtil.isDisposerMethod(n)) {
                for (Parameter p : n.getParameters()) {
                    if (p.getAnnotationByName("Dispose").isPresent()) {
                        try {
                            ResolvedType disposedType = p.getType().resolve();
                            String disposedTypeName = lookup(disposedType);
                            if (!arg.containsKey(disposedTypeName))
                                arg.put(disposedTypeName, new HashSet<>());
                            arg.get(disposedTypeName).add(n.resolve());
                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + this.getClass().getSimpleName()
                                    + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
