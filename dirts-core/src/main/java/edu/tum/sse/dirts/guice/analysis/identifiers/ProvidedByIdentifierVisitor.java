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

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static java.util.logging.Level.FINEST;

/**
 * Identifies just-in-time bindings
 * <p>
 * Rationale:
 * https://github.com/google/guice/wiki/JustInTimeBindings
 */
public class ProvidedByIdentifierVisitor extends AbstractIdentifierVisitor<BeanStorage<GuiceBinding>> {

    //##################################################################################################################
    // Singleton pattern

    private static final ProvidedByIdentifierVisitor singleton = new ProvidedByIdentifierVisitor();

    private ProvidedByIdentifierVisitor() {
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
        Set<AnnotationExpr> maybeAnnotationExpr = GuiceUtil.getProvidedByAnnotation(n);
        for (AnnotationExpr annotationExpr : maybeAnnotationExpr) {

            ResolvedReferenceTypeDeclaration implementedType = null;
            ResolvedMethodDeclaration getMethod = null;

            try {
                //******************************************************************************************************
                // Type that is implemented
                implementedType = n.resolve();

            } catch (Throwable e) {
                Log.log(FINEST, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }

            //**********************************************************************************************************
            // Provider that provides
            ResolvedType type = null;
            if (annotationExpr.isSingleMemberAnnotationExpr()) {
                SingleMemberAnnotationExpr singleMemberAnnotationExpr = annotationExpr.asSingleMemberAnnotationExpr();
                try {
                    ResolvedType resolvedType = singleMemberAnnotationExpr.getMemberValue().calculateResolvedType();
                    if (resolvedType.isReferenceType()) {
                        type = JavaParserUtils.extractClassType(resolvedType.asReferenceType(),
                                Set.of("java.lang.Class"));
                    }
                } catch (Throwable e) {
                    Log.log(FINEST, "Exception in " + JavaParserUtils.class.getSimpleName() + ": " + e.getMessage());
                }
            } else if (annotationExpr.isNormalAnnotationExpr()) {
                NormalAnnotationExpr normalAnnotationExpr = annotationExpr.asNormalAnnotationExpr();
                for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
                    if (pair.getNameAsString().equals("value")) {
                        try {
                            ResolvedType resolvedType = pair.getValue().calculateResolvedType();
                            if (resolvedType.isReferenceType()) {
                                type = JavaParserUtils.extractClassType(resolvedType.asReferenceType(),
                                        Set.of("java.lang.Class"));
                            }
                        } catch (Throwable e) {
                            Log.log(FINEST, "Exception in " + JavaParserUtils.class.getSimpleName() + ": "
                                    + e.getMessage());
                        }
                    }
                }
            }
            if (type != null && type.isReferenceType()) {
                ResolvedReferenceType resolvedReferenceType = type.asReferenceType();
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration =
                        resolvedReferenceType.getTypeDeclaration();
                if (maybeTypeDeclaration.isPresent()) {
                    ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = maybeTypeDeclaration.get();
                    for (ResolvedMethodDeclaration declaredMethod :
                            resolvedReferenceTypeDeclaration.getDeclaredMethods()) {
                        if (declaredMethod.getSignature().equals("get()")) {
                            getMethod = declaredMethod;
                            break;
                        }
                    }

                }
            }

            //**********************************************************************************************************
            // Add binding

            if (implementedType != null && getMethod != null) {
                arg.addBeanByTypeDeclaration(implementedType, new GuiceBinding(getMethod));
            }


        }
    }
}
