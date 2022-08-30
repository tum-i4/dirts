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
package edu.tum.sse.dirts.spring.analysis;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import edu.tum.sse.dirts.analysis.di.InjectionPointStorage;
import edu.tum.sse.dirts.analysis.di.MethodLevelInjectionPointCollector;
import edu.tum.sse.dirts.spring.analysis.identifiers.SpringGetBeanIdentifierVisitor;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Collects dependencies induced by Spring for method level nodes
 */
public class SpringMethodLevelInjectionPointCollectorVisitor
        extends SpringInjectionPointCollectorVisitor<BodyDeclaration<?>>
        implements MethodLevelInjectionPointCollector {

    //##################################################################################################################
    // auxiliary visitor methods (used to set up things before)

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
    // Methods inherited from MethodLevelInjectionPointCollector

    @Override
    public void visit(MethodDeclaration n, InjectionPointStorage injectionPoints) {
        // Setter injection
        if (isInjected(n)) {
            for (Parameter parameter : n.getParameters()) {
                handleInjectionVariable(injectionPoints,
                        n, x -> lookup(x.resolve()),
                        parameter, p -> p.getType().resolve());
            }
        }

        // Injection through BeanFactory
        handleGetBean(injectionPoints,
                n, x -> lookup(x.resolve()),
                SpringGetBeanIdentifierVisitor.collectGetBeanMethodCalls(n));
    }

    @Override
    public void visit(ConstructorDeclaration n, InjectionPointStorage injectionPoints) {
        // Constructor injection
        if (isInjected(n)) {
            for (Parameter parameter : n.getParameters()) {
                handleInjectionVariable(injectionPoints,
                        n, x -> lookup(x.resolve()),
                        parameter, p -> p.getType().resolve());
            }
        }

        // Injection through BeanFactory
        handleGetBean(injectionPoints,
                n, x -> lookup(x.resolve()),
                SpringGetBeanIdentifierVisitor.collectGetBeanMethodCalls(n));
    }

    @Override
    public void visit(FieldDeclaration n, InjectionPointStorage injectionPoints) {
        // Field injection
        if (isInjected(n)) {
            handleInjectionVariable(injectionPoints,
                    n, x -> lookup(x.resolve().declaringType(), x.resolve()),
                    n, p -> p.getCommonType().resolve());
        }

        // Injection through BeanFactory
        handleGetBean(injectionPoints,
                n, x -> lookup(x.resolve().declaringType(), x.resolve()),
                SpringGetBeanIdentifierVisitor.collectGetBeanMethodCalls(n));
    }

    @Override
    public void visit(EnumConstantDeclaration n, InjectionPointStorage injectionPoints) {
        // Injection through BeanFactory
        handleGetBean(injectionPoints,
                n, x -> lookup(x.resolve().asType(), x.resolve()),
                SpringGetBeanIdentifierVisitor.collectGetBeanMethodCalls(n));
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, InjectionPointStorage injectionPoints) {
        // Injection through BeanFactory
        handleGetBean(injectionPoints,
                n, x -> lookup(x.resolve().asType(), x.resolve()),
                SpringGetBeanIdentifierVisitor.collectGetBeanMethodCalls(n));
    }

    @Override
    public void visit(InitializerDeclaration n, InjectionPointStorage injectionPoints) {
        // Injection through BeanFactory
        handleGetBean(injectionPoints,
                n, x -> lookup((Node) x).getFirst(),
                SpringGetBeanIdentifierVisitor.collectGetBeanMethodCalls(n));
    }
}
