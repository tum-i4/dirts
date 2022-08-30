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
package edu.tum.sse.dirts.guice.analysis;

import com.github.javaparser.ast.body.*;
import edu.tum.sse.dirts.analysis.di.InjectionPointStorage;
import edu.tum.sse.dirts.analysis.di.MethodLevelInjectionPointCollector;

import static edu.tum.sse.dirts.guice.analysis.identifiers.GetInstanceIdentifierVisitor.collectGetInstanceMethodCalls;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Collects injectionPoints induced by Guice for method level nodes
 */
public class GuiceMethodLevelInjectionPointCollectorVisitor
        extends GuiceInjectionPointCollectorVisitor<BodyDeclaration<?>>
        implements MethodLevelInjectionPointCollector {

    //##################################################################################################################
    // Visitor pattern - auxiliary visitor methods (used to set up things before)

    @Override
    public void visit(ClassOrInterfaceDeclaration n, InjectionPointStorage injectionPoints) {
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, injectionPoints));
    }

    @Override
    public void visit(EnumDeclaration n, InjectionPointStorage injectionPoints) {
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, injectionPoints));
    }

    @Override
    public void visit(AnnotationDeclaration n, InjectionPointStorage injectionPoints) {
        n.getMembers().stream().filter(m -> !m.isTypeDeclaration())
                .forEach(m -> m.accept(this, injectionPoints));
    }


    //##################################################################################################################
    // Visitor pattern -Methods inherited from MethodLevelInjectionPointCollector

    @Override
    public void visit(MethodDeclaration n, InjectionPointStorage injectionPoints) {
        visit(n.asCallableDeclaration(), injectionPoints);
    }

    @Override
    public void visit(ConstructorDeclaration n, InjectionPointStorage injectionPoints) {
        visit(n.asCallableDeclaration(), injectionPoints);
    }

    private void visit(CallableDeclaration<?> n, InjectionPointStorage injectionPoints) {

        // Constructor or method injection
        if (isInjected(n)) {
            for (Parameter parameter : n.getParameters()) {
                handleInjectionVariable(injectionPoints,
                        n, x -> lookup(x instanceof MethodDeclaration
                                ? ((MethodDeclaration) x).resolve()
                                : ((ConstructorDeclaration) x).resolve()),
                        parameter, p -> p.getType().resolve());

            }
        }

        handleGetInstance(injectionPoints,
                n, x -> lookup(x instanceof MethodDeclaration
                        ? ((MethodDeclaration) x).resolve()
                        : ((ConstructorDeclaration) x).resolve()),
                collectGetInstanceMethodCalls(n));
    }

    @Override
    public void visit(FieldDeclaration n, InjectionPointStorage injectionPoints) {
        // field injection
        if (isInjected(n)) {
            handleInjectionVariable(injectionPoints,
                    n, x -> lookup(n.resolve().declaringType(), n.resolve()),
                    n, p -> p.getCommonType().resolve());

        }

        handleGetInstance(injectionPoints,
                n, x -> lookup(n.resolve().declaringType(), n.resolve()),
                collectGetInstanceMethodCalls(n));
    }

    @Override
    public void visit(EnumConstantDeclaration n, InjectionPointStorage injectionPoints) {
        handleGetInstance(injectionPoints,
                n, x -> lookup(n.resolve().asType(), n.resolve()),
                collectGetInstanceMethodCalls(n));
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, InjectionPointStorage injectionPoints) {
        handleGetInstance(injectionPoints,
                n, x -> lookup(n.resolve().asType(), n.resolve()),
                collectGetInstanceMethodCalls(n));
    }

    @Override
    public void visit(InitializerDeclaration n, InjectionPointStorage injectionPoints) {
        handleGetInstance(injectionPoints,
                n, x -> lookup(n).getFirst(),
                collectGetInstanceMethodCalls(n));
    }
}