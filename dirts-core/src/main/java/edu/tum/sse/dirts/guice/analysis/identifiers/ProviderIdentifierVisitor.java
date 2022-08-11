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

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.Log;

import java.util.*;

import static java.util.logging.Level.FINE;

/**
 * Identifies bindings created in classes extending Provider<T>
 * <p>
 * Rationale:
 * https://github.com/google/guice/wiki/ProviderBindings
 */
public class ProviderIdentifierVisitor extends AbstractTruncatedVisitor<
        BeanStorage<GuiceBinding>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final ProviderIdentifierVisitor singleton = new ProviderIdentifierVisitor();

    private ProviderIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations,
                                            BeanStorage<GuiceBinding> arg) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.accept(singleton, arg);
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, BeanStorage<GuiceBinding> arg) {
        super.visit(n, arg);

        /*
         * This does only account for direct inheritance
         * Accounting for indirect inheritance would involve complex resolution procedures
         * and requires the ability to resolve library code
         */
        for (ClassOrInterfaceType implementedType : n.getImplementedTypes()) {
            if (GuiceUtil.equalsProviderType(implementedType)) {

                Optional<NodeList<Type>> maybeTypeArguments = implementedType.getTypeArguments();
                if (maybeTypeArguments.isPresent()) {

                    //**************************************************************************************************
                    // Type that is provided
                    ResolvedType resolvedInjectableType = null;
                    NodeList<Type> types = maybeTypeArguments.get();
                    if (types.size() == 1) {
                        Type injectableType = types.get(0);
                        try {
                            resolvedInjectableType = injectableType.resolve();
                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": "
                                    + e.getMessage());
                        }
                    }

                    //**************************************************************************************************
                    // Method that is called on injection
                    Set<ResolvedMethodLikeDeclaration> resolvedMethodLikeDeclarations = new HashSet<>();
                    List<MethodDeclaration> getMethods = n.getMethodsBySignature("get");
                    for (MethodDeclaration getMethod : getMethods) {
                        // effectively only one method
                        try {
                            resolvedMethodLikeDeclarations.add(getMethod.resolve());
                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": "
                                    + e.getMessage());
                        }
                    }

                    //**************************************************************************************************
                    // Add binding
                    if (resolvedInjectableType != null) {
                        for (ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration :
                                resolvedMethodLikeDeclarations) {
                            arg.addBeanByType(resolvedInjectableType, new GuiceBinding(resolvedMethodLikeDeclaration));
                        }
                    }
                }
            }
        }
    }
}
