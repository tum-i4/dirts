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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.cdi.util.CDIBean;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.Set;

import static java.util.logging.Level.FINE;

/**
 * Identifies methods annotated with @Bean
 * <p>
 * Rationale:
 * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
 * "Chapter 3. Programming model" - Subchapter "3.1. Managed beans"
 */
public class ManagedBeanIdentifierVisitor extends AbstractIdentifierVisitor<
        BeanStorage<CDIBean>
        > {

    //##################################################################################################################
    // Singleton pattern

    private static final ManagedBeanIdentifierVisitor singleton = new ManagedBeanIdentifierVisitor();

    private ManagedBeanIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations, BeanStorage<CDIBean> arg) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.accept(singleton, arg);
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, BeanStorage<CDIBean> arg) {
        if (CDIUtil.isManagedBean(n)) {
            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                CDIBean newBean = new CDIBean(resolvedReferenceTypeDeclaration);

                //******************************************************************************************************
                // By type
                arg.addBeanByTypeDeclaration(resolvedReferenceTypeDeclaration, newBean);
                Set<ResolvedType> beanTypes = CDIUtil.findBeanTypes(resolvedReferenceTypeDeclaration);
                for (ResolvedType beanType : beanTypes) {
                    arg.addBeanByType(beanType, newBean);
                }

                //******************************************************************************************************
                // By name
                String name = CDIUtil.findName(n);
                arg.addBeanByName(name, newBean);

                //******************************************************************************************************
                // By qualifiers
                Set<String> qualifiers = CDIUtil.findQualifiers(n);
                for (String qualifier : qualifiers) {
                    arg.addBeanByQualifier(qualifier, newBean);
                }

            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
