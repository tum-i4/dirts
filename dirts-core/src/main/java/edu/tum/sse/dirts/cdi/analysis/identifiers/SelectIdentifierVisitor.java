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
package edu.tum.sse.dirts.cdi.analysis.identifiers;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;

/**
 * Identifies calls to select(...) to retrieve beans
 * <p>
 * Rationale:
 * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
 * "Chapter 5. Dependency Injection and lookup" - Subchapter "5.6 Programmatic lookup"
 */
public class SelectIdentifierVisitor extends AbstractIdentifierVisitor<
        Set<Pair<Set<String>, ResolvedType>>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final SelectIdentifierVisitor singleton = new SelectIdentifierVisitor();

    private SelectIdentifierVisitor() {
    }

    public static Set<Pair<Set<String>, ResolvedType>> collectSelectMethodCalls(BodyDeclaration<?> n) {
        Set<Pair<Set<String>, ResolvedType>> namesAndTypes = new HashSet<>();

        if (n.isTypeDeclaration()) {
            TypeDeclaration<?> typeDeclaration = n.asTypeDeclaration();
            typeDeclaration.getMembers().forEach(m -> m.accept(singleton, namesAndTypes));
        } else {
            n.accept(singleton, namesAndTypes);
        }

        return namesAndTypes;
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(MethodCallExpr n, Set<Pair<Set<String>, ResolvedType>> arg) {
        super.visit(n, arg);

        String nameAsString = n.getNameAsString();
        if (nameAsString.equals("select")) {

            ResolvedType requestedType = null;
            Set<String> qualifiers = new HashSet<>();

            for (Expression argument : n.getArguments()) {
                try {
                    ResolvedType argumentType = argument.calculateResolvedType();
                    if (argumentType.isReferenceType()) {
                        ResolvedReferenceType resolvedReferenceType = argumentType.asReferenceType();

                        //**********************************************************************************************
                        // By type
                        try {
                            requestedType = JavaParserUtils.extractClassType(resolvedReferenceType,
                                    Set.of("java.lang.Class", "javax.enterprise.util.TypeLiteral"));
                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": "
                                    + e.getMessage());
                        }

                        //**********************************************************************************************
                        // By qualifiers
                        if (resolvedReferenceType.getQualifiedName().endsWith("AnnotationLiteral")) {
                            ResolvedReferenceType annotationType = resolvedReferenceType.getTypeParametersMap()
                                    .get(0).b.asReferenceType();
                            Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = annotationType
                                    .getTypeDeclaration();
                            if (maybeTypeDeclaration.isPresent()) {
                                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration =
                                        maybeTypeDeclaration.get();
                                if (resolvedReferenceTypeDeclaration.isAnnotation()) {
                                    qualifiers.add(lookup(resolvedReferenceTypeDeclaration.asAnnotation()));
                                }
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
            if (requestedType != null || !qualifiers.isEmpty()) {
                arg.add(new Pair<>(qualifiers, requestedType));
            }
        }
    }
}
