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
package edu.tum.sse.dirts.spring.analysis.identifiers;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.HashSet;
import java.util.Set;

import static java.util.logging.Level.FINE;

/**
 * Identifies method calls to .getBean() supposedly invoked on a BeanFactory or ApplicationContext
 * <p>
 * Rationale:
 * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
 * "Part II. Core Technologies" -  Chapter "3 The IoC Container" - Subchapter "3.2 Container overview"
 */
public class SpringGetBeanIdentifierVisitor extends AbstractIdentifierVisitor<
        Set<Pair<String, ResolvedType>>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final SpringGetBeanIdentifierVisitor singleton = new SpringGetBeanIdentifierVisitor();

    private SpringGetBeanIdentifierVisitor() {
    }

    public static Set<Pair<String, ResolvedType>> collectGetBeanMethodCalls(BodyDeclaration<?> n) {
        Set<Pair<String, ResolvedType>> namesAndTypes = new HashSet<>();

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
    public void visit(MethodCallExpr n, Set<Pair<String, ResolvedType>> arg) {
        super.visit(n, arg);

        String nameAsString = n.getNameAsString();
        if (nameAsString.equals("getBean") || nameAsString.equals("getBeansOfType")) {
            ResolvedType resolvedType = null;
            String name = null;

            for (Expression argument : n.getArguments()) {

                if (argument.isStringLiteralExpr()) {
                    //**************************************************************************************************
                    // By name
                    name = argument.asStringLiteralExpr().getValue();
                } else {
                    //**************************************************************************************************
                    // By type
                    try {
                        ResolvedType argumentResolvedType = argument.calculateResolvedType();
                        if (argumentResolvedType.isReferenceType()) {
                            resolvedType = JavaParserUtils.extractClassType(argumentResolvedType.asReferenceType(), Set.of("java.lang.Class"));
                        }
                    } catch (RuntimeException e) {
                        Log.log(FINE, "Exception in " + JavaParserUtils.class.getSimpleName() + ": " + e.getMessage());
                    }
                }
            }

            //**********************************************************************************************************
            // Add entry
            if (name != null || resolvedType != null)
                arg.add(new Pair<>(name, resolvedType));
        }
    }
}
