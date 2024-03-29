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
package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.body.*;
import edu.tum.sse.dirts.analysis.di.InjectionPointStorage;
import edu.tum.sse.dirts.analysis.di.ClassLevelInjectionPointCollector;
import edu.tum.sse.dirts.cdi.util.CDIUtil;

import java.util.List;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.cdi.analysis.identifiers.SelectIdentifierVisitor.collectSelectMethodCalls;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Collects injectionPoints induced by CDI for class level nodes
 */
public class CDIClassLevelInjectionPointCollectorVisitor
        extends CDIInjectionPointCollectorVisitor<TypeDeclaration<?>>
        implements ClassLevelInjectionPointCollector {


    //##################################################################################################################
    // Visitor pattern - Methods inherited from ClassLevelInjectionPointCollector

    @Override
    public void visit(ClassOrInterfaceDeclaration n, InjectionPointStorage injectionPointStorage) {
        collectDependencies(n, injectionPointStorage);
    }

    @Override
    public void visit(EnumDeclaration n, InjectionPointStorage injectionPointStorage) {
        collectDependencies(n, injectionPointStorage);
    }

    @Override
    public void visit(AnnotationDeclaration n, InjectionPointStorage injectionPointStorage) {
        collectDependencies(n, injectionPointStorage);
    }

    //##################################################################################################################
    // Auxiliary methods

    private void collectDependencies(TypeDeclaration<?> n, InjectionPointStorage injectionPoints) {
        List<BodyDeclaration<?>> injectedBodyDeclarations = n.getMembers().stream()
                .filter(CDIUtil::isInjected)
                .collect(Collectors.toList());

        for (BodyDeclaration<?> injectedBodyDeclaration : injectedBodyDeclarations) {

            if (injectedBodyDeclaration.isFieldDeclaration()) {
                // field injection
                FieldDeclaration injectedField = injectedBodyDeclaration.asFieldDeclaration();
                handleInjectionVariable(injectionPoints,
                        n, x -> lookup(n.resolve()),
                        injectedField, p -> p.getCommonType().resolve());

            } else if (injectedBodyDeclaration.isConstructorDeclaration() ||
                    injectedBodyDeclaration.isMethodDeclaration()) {
                // constructor or method injection
                CallableDeclaration<?> injectedCallable = (CallableDeclaration<?>) injectedBodyDeclaration;
                for (Parameter parameter : injectedCallable.getParameters()) {
                    handleInjectionVariable(injectionPoints,
                            n, x -> lookup(x.resolve()),
                            parameter, p -> p.getType().resolve());
                }
            }
        }

        // Injection through Instance
        handleSelect(injectionPoints,
                n, x -> lookup(n.resolve()),
                collectSelectMethodCalls(n));
    }
}
