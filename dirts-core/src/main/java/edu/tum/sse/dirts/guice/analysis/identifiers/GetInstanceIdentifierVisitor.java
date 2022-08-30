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
package edu.tum.sse.dirts.guice.analysis.identifiers;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.util.HashSet;
import java.util.Set;

import static java.util.logging.Level.FINE;

/**
 * Identifies method calls to getInstance(...)
 * <p>
 * Rationale:
 * https://github.com/google/guice/wiki/GettingStarted
 * Section "Guice injectors"
 */
public class GetInstanceIdentifierVisitor extends AbstractIdentifierVisitor<
        Set<ResolvedType>
        > {


    //##################################################################################################################
    // Singleton pattern

    private static final GetInstanceIdentifierVisitor singleton = new GetInstanceIdentifierVisitor();

    private GetInstanceIdentifierVisitor() {
    }

    public static Set<ResolvedType> collectGetInstanceMethodCalls(BodyDeclaration<?> n) {
        Set<ResolvedType> arg = new HashSet<>();

        if (n.isTypeDeclaration()) {
            TypeDeclaration<?> typeDeclaration = n.asTypeDeclaration();
            typeDeclaration.getMembers().forEach(m -> m.accept(singleton, arg));
        } else {
            n.accept(singleton, arg);
        }

        return arg;
    }


    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(MethodCallExpr n, Set<ResolvedType> arg) {
        super.visit(n, arg);

        ResolvedType resolvedType = null;

        String nameAsString = n.getNameAsString();
        if (nameAsString.equals("getInstance") || nameAsString.equals("getProvider")) {
            NodeList<Expression> arguments = n.getArguments();
            if (arguments.size() == 1) {
                try {
                    ResolvedType argumentResolvedType = arguments.get(0).calculateResolvedType();
                    if (argumentResolvedType.isReferenceType()) {
                        resolvedType = JavaParserUtils.extractClassType(argumentResolvedType.asReferenceType(),
                                Set.of("java.lang.Class", "com.google.inject.Key"));
                    }
                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + JavaParserUtils.class.getSimpleName() + ": " + e.getMessage());
                }
                // Unfortunately, it is not easy to extract annotations or Names.named(...) from Key<T>
                if (resolvedType != null)
                    arg.add(resolvedType);
            }
        }
    }
}