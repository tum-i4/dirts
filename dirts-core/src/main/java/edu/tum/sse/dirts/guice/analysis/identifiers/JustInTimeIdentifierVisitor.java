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
package edu.tum.sse.dirts.guice.analysis.identifiers;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;

import static java.util.logging.Level.FINE;

/**
 * Identifies just-in-time bindings
 * <p>
 * Rationale:
 * https://github.com/google/guice/wiki/JustInTimeBindings
 */
public class JustInTimeIdentifierVisitor extends AbstractIdentifierVisitor<
        BeanStorage<GuiceBinding>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final JustInTimeIdentifierVisitor singleton = new JustInTimeIdentifierVisitor();

    private JustInTimeIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations,
                                            BeanStorage<GuiceBinding> arg) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.accept(singleton, arg);
            typeDeclaration.getConstructors().forEach(c -> c.accept(singleton, arg));
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, BeanStorage<GuiceBinding> arg) {
        if (n.getConstructors().isEmpty()) {

            //**********************************************************************************************************
            // Classes without explicit constructor
            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                for (ResolvedConstructorDeclaration constructor : resolvedReferenceTypeDeclaration.getConstructors()) {
                    // Only default constructor

                    ResolvedReferenceTypeDeclaration resolvedReturnTypeDeclaration = constructor.declaringType();
                    arg.addBeanByTypeDeclaration(resolvedReturnTypeDeclaration, new GuiceBinding(constructor));
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, BeanStorage<GuiceBinding> arg) {
        if (GuiceUtil.isJustInTimeBinding(n)) {

            //**********************************************************************************************************
            // Classes with zero-arg explicit constructor or @Inject annotation
            try {
                ResolvedConstructorDeclaration resolvedConstructorDeclaration = n.resolve();
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration =
                        resolvedConstructorDeclaration.declaringType();

                arg.addBeanByTypeDeclaration(resolvedReferenceTypeDeclaration,
                        new GuiceBinding(resolvedConstructorDeclaration));
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
