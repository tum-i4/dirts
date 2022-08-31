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
package edu.tum.sse.dirts.spring.analysis.identifiers;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.util.SpringUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINEST;

/**
 * Identifies methods annotated with @Bean
 * <p>
 * Rationale:
 * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
 * "Part II. Core Technologies" -  Chapter "3 The IoC Container" - Subchapter "3.12 Java-based container configuration"
 */
public class SpringBeanMethodIdentifierVisitor extends AbstractIdentifierVisitor<
        BeanStorage<SpringBean>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final SpringBeanMethodIdentifierVisitor singleton = new SpringBeanMethodIdentifierVisitor();

    private SpringBeanMethodIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations,
                                            BeanStorage<SpringBean> beanStorage) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.getMembers().forEach(m -> m.accept(singleton, beanStorage));
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(MethodDeclaration n, BeanStorage<SpringBean> arg) {
        Set<AnnotationExpr> maybeBeanAnnotation = SpringUtil.getBeanAnnotation(n);
        if (!maybeBeanAnnotation.isEmpty()) {
            // should be exactly one annotation
            for (AnnotationExpr beanAnnotation : maybeBeanAnnotation) {
                try {
                    ResolvedMethodDeclaration methodDecl = n.resolve();
                    ResolvedType returnType = n.getType().resolve();

                    SpringBean newBean = new SpringBean(methodDecl);

                    //**************************************************************************************************
                    // By type

                    // return Type may be primitive
                    arg.addBeanByType(returnType, newBean);

                    // if returnType is ReferenceType add entries for all resolvable ancestors
                    if (returnType.isReferenceType()) {
                        ResolvedReferenceType resolvedReferenceType = returnType.asReferenceType();
                        Optional<ResolvedReferenceTypeDeclaration> typeDeclaration = resolvedReferenceType
                                .getTypeDeclaration();
                        if (typeDeclaration.isPresent()) {
                            for (ResolvedReferenceType ancestor : typeDeclaration.get()
                                    .getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList)) {
                                arg.addBeanByType(ancestor, newBean);
                            }
                        }
                    }

                    //**************************************************************************************************
                    // By name
                    Set<String> names = SpringUtil.findNames(n, beanAnnotation);
                    for (String name : names) {
                        arg.addBeanByName(name, newBean);
                    }

                } catch (Throwable e) {
                    Log.log(FINEST, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }
}
