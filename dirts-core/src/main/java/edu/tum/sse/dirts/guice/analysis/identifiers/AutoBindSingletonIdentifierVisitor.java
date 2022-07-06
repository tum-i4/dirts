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
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.Set;

import static java.util.logging.Level.FINE;


/**
 * Identifies classes annotated with @AutoBindSingleton
 * <p>
 * Rationale:
 * https://github.com/Netflix/governator/wiki/AutoBindSingleton
 */
public class AutoBindSingletonIdentifierVisitor extends AbstractTruncatedVisitor<
        BeanStorage<GuiceBinding>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final AutoBindSingletonIdentifierVisitor singleton = new AutoBindSingletonIdentifierVisitor();

    private AutoBindSingletonIdentifierVisitor() {
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
        Set<AnnotationExpr> maybeAutoBindSingletonAnnotation = GuiceUtil.getAutoBindSingletonAnnotation(n);
        if (!maybeAutoBindSingletonAnnotation.isEmpty()) {
            AnnotationExpr annotationExpr = maybeAutoBindSingletonAnnotation.stream().findFirst().get();

            ResolvedType bindType = null;

            // We try to extract the type that is being bound to
            if (annotationExpr.isSingleMemberAnnotationExpr()) {
                // single member annotation must contain a classExpr
                Expression memberValue = annotationExpr.asSingleMemberAnnotationExpr().getMemberValue();
                try {
                    ResolvedType resolvedType = memberValue.calculateResolvedType();
                    if (resolvedType.isReferenceType()) {
                        bindType = JavaParserUtils.extractClassType(resolvedType.asReferenceType(), Set.of("java.lang.Class"));
                    }
                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            } else if (annotationExpr.isNormalAnnotationExpr()) {
                // the member baseClass contains the type
                NodeList<MemberValuePair> pairs = annotationExpr.asNormalAnnotationExpr().getPairs();
                for (MemberValuePair pair : pairs) {
                    if (pair.getNameAsString().equals("baseClass")) {
                        Expression value = pair.getValue();
                        try {
                            ResolvedType resolvedType = value.calculateResolvedType();
                            if (resolvedType.isReferenceType()) {
                                bindType = JavaParserUtils.extractClassType(resolvedType.asReferenceType(), Set.of("java.lang.Class"));
                            }
                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                        }
                    }
                }
            }

            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                if (bindType != null) {
                    arg.addBeanByType(bindType, new GuiceBinding(resolvedReferenceTypeDeclaration));
                } else {
                    // in case of MarkerAnnotation the type that is bound is the type of this class
                    arg.addBeanByTypeDeclaration(resolvedReferenceTypeDeclaration,
                            new GuiceBinding(resolvedReferenceTypeDeclaration));
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

    }

    @Override
    public void visit(AnnotationDeclaration n, BeanStorage<GuiceBinding> arg) {
        // Not possible on annotations
    }

    @Override
    public void visit(EnumDeclaration n, BeanStorage<GuiceBinding> arg) {
        // Not possible on enums
    }
}