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

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static java.util.logging.Level.FINE;

/**
 * Identifies bindings created in methods annotated with @Provides
 * <p>
 * Rationale:
 * https://github.com/google/guice/wiki/ProvidesMethods
 */
public class ProvidesIdentifierVisitor extends AbstractIdentifierVisitor<
        BeanStorage<GuiceBinding>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final ProvidesIdentifierVisitor singleton = new ProvidesIdentifierVisitor();

    private ProvidesIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations,
                                            BeanStorage<GuiceBinding> arg) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.getMembers().forEach(m -> m.accept(singleton, arg));
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(MethodDeclaration n, BeanStorage<GuiceBinding> arg) {
        if (GuiceUtil.isProvidesMethod(n)) {
            try {

                //******************************************************************************************************
                // Method that is called on injection
                ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();

                //******************************************************************************************************
                // Type that is provided
                ResolvedType resolvedReturnType = resolvedMethodDeclaration.getReturnType();

                //******************************************************************************************************
                // BindingAnnotations
                Set<String> bindingAnnotations = GuiceUtil.findQualifiers(n);

                //******************************************************************************************************
                // Names
                Optional<String> name = GuiceUtil.findName(n);


                //******************************************************************************************************
                // Add binding

                GuiceBinding guiceBinding = new GuiceBinding(resolvedMethodDeclaration);

                arg.addBeanByType(resolvedReturnType, guiceBinding);
                for (String bindingAnnotation : bindingAnnotations) {
                    arg.addBeanByQualifier(bindingAnnotation, guiceBinding);
                }
                name.ifPresent(s -> arg.addBeanByName(s, guiceBinding));

            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
