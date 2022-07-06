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
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.util.Container;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.naming_scheme.Names;
import edu.tum.sse.dirts.util.tuples.Pair;
import edu.tum.sse.dirts.util.tuples.Triple;

import java.util.*;

import static java.util.logging.Level.FINE;

/**
 * Identifies bindings created with bind(..).to(...)
 * <p>
 * Rationale:
 * https://github.com/google/guice/wiki/LinkedBindings
 * https://github.com/google/guice/wiki/InstanceBindings
 */
public class BindToIdentifier extends AbstractIdentifierVisitor<
        BeanStorage<GuiceBinding>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final BindToIdentifier singleton = new BindToIdentifier();

    private BindToIdentifier() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations,
                                            BeanStorage<GuiceBinding> arg) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.getMembers().forEach(m -> m.accept(singleton, arg));
        }
    }

    private static final BindIdentifier bindIdentifier = new BindIdentifier();
    private static final BindingsIdentifier bindingsIdentifier = new BindingsIdentifier();

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(MethodDeclaration n, BeanStorage<GuiceBinding> arg) {
        super.visit(n, arg);

        Map<Triple<ResolvedType, String, String>, Set<ResolvedReferenceTypeDeclaration>> result = new HashMap<>();
        n.accept(bindingsIdentifier, result);

        if (!result.isEmpty()) {
            ResolvedMethodDeclaration resolvedMethodDeclaration = null;
            try {
                resolvedMethodDeclaration = n.resolve();
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
            if (resolvedMethodDeclaration != null) {
                ResolvedMethodDeclaration finalResolvedMethodDeclaration = resolvedMethodDeclaration;
                result.forEach((k, v) -> {
                    GuiceBinding guiceBinding = new GuiceBinding(new Pair<>(finalResolvedMethodDeclaration, v));

                    ResolvedType bindType = k.getFirst();
                    String name = k.getSecond();
                    String annotaion = k.getThird();

                    //******************************************************************************************************
                    // Add binding

                    if (bindType != null) {
                        arg.addBeanByType(bindType, guiceBinding);
                    }
                    if (name != null) {
                        arg.addBeanByName(name, guiceBinding);
                    }
                    if (annotaion != null) {
                        arg.addBeanByQualifier(annotaion, guiceBinding);
                    }
                });
            }
        }

    }

    //##################################################################################################################
    // Auxiliary classes

    private static class BindIdentifier extends AbstractIdentifierVisitor<
            Container<Triple<ResolvedType, /*name*/String, /*annotation*/String>>
            > {
        @Override
        public void visit(MethodCallExpr n, Container<Triple<ResolvedType, String, String>> arg) {
            String nameAsString = n.getNameAsString();

            ResolvedType type = arg.content.getFirst();
            String name = arg.content.getSecond();
            String annotation = arg.content.getThird();

            if (nameAsString.equals("bind")) {
                NodeList<Expression> arguments = n.getArguments();
                if (arguments.size() == 1) {
                    Expression boundExpr = arguments.get(0);
                    try {
                        ResolvedType resolvedType = boundExpr.calculateResolvedType();
                        if (resolvedType.isReferenceType()) {
                            arg.content = new Triple<>(JavaParserUtils.extractClassType(resolvedType.asReferenceType(),
                                    Set.of("java.lang.Class", "com.google.inject.TypeLiteral", "com.google.inject.Key")), name, annotation);
                        }
                    } catch (RuntimeException e) {
                        Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                    }

                }
            } else {
                if (nameAsString.equals("annotatedWith")) {
                    NodeList<Expression> arguments = n.getArguments();
                    if (arguments.size() == 1) {
                        Expression argument = arguments.get(0);
                        ResolvedType resolvedType = argument.calculateResolvedType();
                        if (resolvedType.isReferenceType()) {
                            ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
                            if (JavaParserUtils.equalsTypeName(resolvedReferenceType, "com.google.inject.name.Named")) {
                                // Names.named("name")
                                String newName = NameIdentifierVisitor.getNameFromNamesDotNamed(n);
                                arg.content = new Triple<>(type, newName, annotation);
                            } else {
                                // Annotation.class
                                ResolvedType annotationClass = JavaParserUtils.extractClassType(resolvedReferenceType, Set.of("java.lang.Class"));
                                arg.content = new Triple<>(type, name, Names.lookup(annotationClass));
                            }
                        }
                    }
                    super.visit(n, arg);
                }
            }
        }
    }

    private static class BindingsIdentifier extends AbstractIdentifierVisitor<
            Map<Triple<ResolvedType, String, String>, Set<ResolvedReferenceTypeDeclaration>>
            > {

        @Override
        public void visit(MethodCallExpr n, Map<Triple<ResolvedType, String, String>, Set<ResolvedReferenceTypeDeclaration>> arg) {
            String nameAsString = n.getNameAsString();
            if (nameAsString.equals("to") || nameAsString.equals("toInstance")) {

                //******************************************************************************************************
                // Type in to(...)
                ResolvedReferenceTypeDeclaration toType = null;

                NodeList<Expression> arguments = n.getArguments();
                if (arguments.size() == 1) {
                    Expression toExpr = arguments.get(0);

                    ResolvedType resolvedType = null;
                    if (nameAsString.equals("to")) {
                        try {
                            ResolvedType toExprResolvedType = toExpr.calculateResolvedType();
                            if (toExprResolvedType.isReferenceType()) {
                                resolvedType = JavaParserUtils.extractClassType(toExprResolvedType.asReferenceType(),
                                        Set.of("java.lang.Class", "com.google.inject.TypeLiteral", "com.google.inject.Key"));
                            }

                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " +
                                        e.getMessage());
                        }
                    }
                    if (nameAsString.equals("toInstance")) {
                        try {
                            resolvedType = toExpr.calculateResolvedType();
                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " +
                                        e.getMessage());
                        }
                    }

                    if (resolvedType != null && resolvedType.isReferenceType()) {
                        Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = resolvedType.asReferenceType()
                                .getTypeDeclaration();
                        if (maybeTypeDeclaration.isPresent()) {
                            toType = maybeTypeDeclaration.get();
                        }
                    }
                }


                Container<Triple<ResolvedType, String, String>> container =
                        new Container<>(new Triple<>(null, null, null));

                Optional<Expression> maybeScope = n.getScope();
                if (maybeScope.isPresent()) {
                    try {
                        maybeScope.get().accept(bindIdentifier, container);
                    } catch (RuntimeException e) {
                        Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " +
                                    e.getMessage());
                    }
                }

                if (toType != null) {
                    if (!arg.containsKey(container.content))
                        arg.put(container.content, new HashSet<>());
                    arg.get(container.content).add(toType);
                }
            }
            super.visit(n, arg);
        }

    }
}
