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

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.cdi.util.CDIBean;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.util.tuples.Triple;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Identifies fields annotated with @Produces
 * <p>
 * Rationale:
 * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
 * "Chapter 3. Programming model" - Subchapter "3.3 Producer fields"
 */
public class ProducerFieldIdentifierVisitor extends AbstractIdentifierVisitor<
        BeanStorage<CDIBean>
        > {

    //##################################################################################################################
    // Constants

    private static final ProducerFieldDeclaratorIdentifierVisitor producerFieldDeclaratorIdentifierVisitor =
            new ProducerFieldDeclaratorIdentifierVisitor();

    //##################################################################################################################
    // Singleton pattern

    private static final ProducerFieldIdentifierVisitor singleton = new ProducerFieldIdentifierVisitor();

    private ProducerFieldIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations, BeanStorage<CDIBean> arg) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.getMembers().forEach(m -> m.accept(singleton, arg));
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(FieldDeclaration n, BeanStorage<CDIBean> arg) {
        if (CDIUtil.isProducerNode(n)) {
            if (n.getVariables().size() == 1) {
                Set<ResolvedType> beanTypes = new HashSet<>();

                try {
                    ResolvedFieldDeclaration resolvedFieldDeclaration = n.resolve();
                    CDIBean newBean = new CDIBean(resolvedFieldDeclaration);

                    try {
                        //**********************************************************************************************
                        // By type
                        ResolvedType variableType = n.getCommonType().resolve();
                        beanTypes.addAll(CDIUtil.findBeanTypes(variableType));
                        for (ResolvedType beanType : beanTypes) {
                            arg.addBeanByType(beanType, newBean);
                        }
                    } catch (RuntimeException e) {
                        if (Control.DEBUG)
                            System.out.println("Exception in " + this.getClass().getSimpleName() + ": " +
                                    e.getMessage());
                    }

                    //**************************************************************************************************
                    // By name
                    String name = CDIUtil.findName(n);
                    if (name != null)
                        arg.addBeanByName(name, newBean);

                    //**************************************************************************************************
                    // By qualifiers
                    Set<String> qualifiers = CDIUtil.findQualifiers(n);
                    for (String qualifier : qualifiers) {
                        arg.addBeanByQualifier(qualifier, newBean);
                    }

                } catch (RuntimeException e) {
                    if (Control.DEBUG)
                        System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
                }
            } else {
                Set<String> qualifiers = CDIUtil.findQualifiers(n);
                String name = CDIUtil.findName(n);
                n.accept(producerFieldDeclaratorIdentifierVisitor, new Triple<>(arg, name, qualifiers));
            }
        }
    }

    //##################################################################################################################
    // Auxiliary methods

    private static void addToBeanStorage(ResolvedFieldDeclaration resolvedFieldDeclaration,
                                         BeanStorage<CDIBean> beanStorage,
                                         String name,
                                         Set<String> qualifiers,
                                         Set<ResolvedType> beanTypes) {
        // add to BeanStorage<CDIBean>
        CDIBean newBean = new CDIBean(resolvedFieldDeclaration);

        if (name != null)
            beanStorage.addBeanByName(name, newBean);
        for (ResolvedType beanType : beanTypes) {
            beanStorage.addBeanByType(beanType, newBean);
        }
        for (String qualifier : qualifiers) {
            beanStorage.addBeanByQualifier(qualifier, newBean);
        }
    }

    //##################################################################################################################
    // Auxiliary classes

    private static class ProducerFieldDeclaratorIdentifierVisitor extends AbstractIdentifierVisitor<
            Triple<BeanStorage<CDIBean>, String, Set<String>>
            > {
        @Override
        public void visit(VariableDeclarator n, Triple<BeanStorage<CDIBean>, String, Set<String>> arg) {
            BeanStorage<CDIBean> beanStorage = arg.getFirst();
            String name = arg.getSecond();
            Set<String> qualifiers = arg.getThird();


            if (name == null) {
                // No Named annotation present, assign default name
                name = n.getNameAsString();
            }

            try {
                ResolvedValueDeclaration resolvedValueDeclaration = n.resolve();
                //noinspection StatementWithEmptyBody
                if (resolvedValueDeclaration.isField()) {
                    CDIBean newBean = new CDIBean(resolvedValueDeclaration);

                    try {
                        //**********************************************************************************************
                        // By type
                        ResolvedType variableType = n.getType().resolve();
                        Set<ResolvedType> beanTypes = new HashSet<>(CDIUtil.findBeanTypes(variableType));
                        for (ResolvedType beanType : beanTypes) {
                            beanStorage.addBeanByType(beanType, newBean);
                        }
                    } catch (RuntimeException e) {
                        if (Control.DEBUG)
                            System.out.println("Exception in " + this.getClass().getSimpleName() + ": " +
                                    e.getMessage());
                    }

                    //**************************************************************************************************
                    // By name
                    if (name != null)
                        beanStorage.addBeanByName(name, newBean);

                    //**************************************************************************************************
                    // By qualifiers
                    for (String qualifier : qualifiers) {
                        beanStorage.addBeanByQualifier(qualifier, newBean);
                    }

                } else {
                    // unreachable
                }
            } catch (RuntimeException e) {
                if (Control.DEBUG)
                    System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
