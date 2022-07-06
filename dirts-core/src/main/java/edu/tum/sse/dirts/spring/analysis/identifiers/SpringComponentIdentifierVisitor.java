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

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.util.SpringUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.Set;

import static java.util.logging.Level.FINE;

/**
 * Identifies classes annotated with @Component
 * <p>
 * Rationale:
 * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
 * "Part II. Core Technologies" -  Chapter "3 The IoC Container" - Subchapter "3.12 Java-based container configuration"
 */
public class SpringComponentIdentifierVisitor extends AbstractTruncatedVisitor<
        BeanStorage<SpringBean>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final SpringComponentIdentifierVisitor singleton = new SpringComponentIdentifierVisitor();

    private SpringComponentIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations, BeanStorage<SpringBean> beanStorage) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.accept(singleton, beanStorage);
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, BeanStorage<SpringBean> arg) {
        Set<AnnotationExpr> maybeComponentAnnotation = SpringUtil.getComponentAnnotation(n);
        if (!maybeComponentAnnotation.isEmpty()) {
            // should be exactly one annotation
            for (AnnotationExpr componentAnnotation : maybeComponentAnnotation) {
                try {
                    ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                    SpringBean newBean = new SpringBean(resolvedReferenceTypeDeclaration);

                    //******************************************************************************************************
                    // By type

                    // add entry to this class
                    arg.addBeanByTypeDeclaration(resolvedReferenceTypeDeclaration, newBean);

                    // Add entries for all resolvable ancestors
                    for (ResolvedReferenceType ancestor : resolvedReferenceTypeDeclaration
                            .getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList)) {
                        arg.addBeanByType(ancestor, newBean);
                    }

                    //******************************************************************************************************
                    // By name

                    Set<String> names = SpringUtil.findNames(n, componentAnnotation);
                    for (String name : names) {
                        arg.addBeanByName(name, newBean);
                    }

                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void visit(EnumDeclaration n, BeanStorage arg) {
        // Spring does not support @Component on enums
    }

    @Override
    public void visit(AnnotationDeclaration n, BeanStorage arg) {
        // We do not support @Component on annotations
    }
}
