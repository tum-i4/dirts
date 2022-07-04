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
package edu.tum.sse.dirts.cdi.util;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.tuples.Triple;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities for CDI
 */
public class CDIUtil {

    /**
     * Decides if a class is a managed bean
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 3. Programming model" - Subchapter "3.1. Managed beans" - Section "3.1.1. Which Java classes are managed beans"
     *
     * @param n
     * @return
     */
    public static boolean isManagedBean(ClassOrInterfaceDeclaration n) {
        // Since this does not account for @Vetoed, abstract classes with @Decorator or classes extending Extension,
        // this is an overapproximation
        if (n.isInterface()) return false;
        for (ConstructorDeclaration constructor : n.getConstructors()) {
            if (constructor.getParameters().isEmpty() ||
                    CDIUtil.isInjectableNode(constructor)) {
                return true;
            }
        }
        return n.getConstructors().isEmpty();
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> injectAnnotations = Set.of(
            new Triple<>("Inject", "javax.inject.Inject",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation)),
            new Triple<>("Inject", "javax.enterprise.inject.Inject",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation)),
            new Triple<>("Inject", "jakarta.enterprise.inject.Inject",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation))
    );

    /**
     * Decides whether a node is injectable as specified by @Inject
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 3. Programming model" - Subchapter "3.3. Initializer methods"
     * "Chapter 3. Programming model" - Subchapter "3.5. Bean constructors"
     * "Chapter 3. Programming model" - Subchapter "3.6. Injected fields"
     *
     * @param n
     * @return
     */
    public static boolean isInjectableNode(NodeWithAnnotations<?> n) {
        return JavaParserUtils.isAnnotatedWithAny(n, injectAnnotations);
    }

    /**
     * Decides whether something can be injected into a node
     * or this node is a disposer method that is called on destruction
     * or this node is a producer method that could have parameters that are injected
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 3. Programming model" - Subchapter "3.2. Producer methods"
     * "Chapter 3. Programming model" - Subchapter "3.3. Initializer methods"
     * "Chapter 3. Programming model" - Subchapter "3.4. Disposer methods"
     * "Chapter 3. Programming model" - Subchapter "3.5. Bean constructors"
     * "Chapter 3. Programming model" - Subchapter "3.6. Injected fields"
     *
     * @param bodyDeclaration
     * @return
     */
    public static boolean isInjected(BodyDeclaration<?> bodyDeclaration) {
        return CDIUtil.isInjectableNode(bodyDeclaration)
                || CDIUtil.isProducerNode(bodyDeclaration);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> producesAnnotations = Set.of(
            new Triple<>("Produces", "javax.enterprise.inject.Produces",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation)),
            new Triple<>("Produces", "jakarta.enterprise.inject.Produces",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation))
    );

    /**
     * Decides whether a nod is a producer method or producer field
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 3. Programming model" - Subchapter "3.2. Producer methods"
     * "Chapter 3. Programming model" - Subchapter "3.3. Producer fields"
     *
     * @param n
     * @return
     */
    public static boolean isProducerNode(NodeWithAnnotations<?> n) {
        return JavaParserUtils.isAnnotatedWithAny(n, producesAnnotations);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> disposesAnnotations = Set.of(
            new Triple<>("Disposes", "javax.enterprise.inject.Disposes",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation)),
            new Triple<>("Disposes", "jakarta.enterprise.inject.Disposes",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation))
    );

    /**
     * Decided whether a method is a disposer method
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 3. Programming model" - Subchapter "3.4. Disposer methods"
     *
     * @param n
     * @return
     */
    public static boolean isDisposerMethod(MethodDeclaration n) {
        return n.getParameters().stream().anyMatch(p ->
                JavaParserUtils.isAnnotatedWithAny(p, disposesAnnotations));
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> alternativeAnnotations =
            Set.of(
                    new Triple<>("Alternative", "javax.enterprise.inject.Alternative",
                            Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation)),
                    new Triple<>("Alternative", "jakarta.enterprise.inject.Alternative",
                            Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation))
            );

    /**
     * Decided whether a bean is an alternative
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 2. Concepts" - Subchapter "2.7. Alternatives"
     *
     * @param n
     * @return
     */
    public static boolean isAlternativeNode(NodeWithAnnotations<?> n) {
        return JavaParserUtils.isAnnotatedWithAny(n, alternativeAnnotations);
    }

    //------------------------------------------------------------------------------------------------------------------

    public static String lookupXMlAlternativeName(String className) {
        return className + "." + "!xmlAlternativeEntry";
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<String> nonQualifierAnnotations = new HashSet<>();

    static {
        injectAnnotations.forEach(i -> nonQualifierAnnotations.add(i.getSecond()));
        producesAnnotations.forEach(i -> nonQualifierAnnotations.add(i.getSecond()));
        disposesAnnotations.forEach(i -> nonQualifierAnnotations.add(i.getSecond()));
        alternativeAnnotations.forEach(i -> nonQualifierAnnotations.add(i.getSecond()));

        // Technically those are qualifier annotations, but not useful for our purposes here
        nonQualifierAnnotations.add("javax.inject.Named");
        nonQualifierAnnotations.add("jakarta.inject.Named");
        nonQualifierAnnotations.add("javax.inject.Any");
        nonQualifierAnnotations.add("jakarta.inject.Any");
        nonQualifierAnnotations.add("javax.annotation.Priority");
        nonQualifierAnnotations.add("jakarta.annotation.Priority");
    }

    /**
     * Finds the names of qualifier annotations of a node
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 2. Concepts" - Subchapter "2.3. Qualifiers"
     *
     * @param n
     * @return
     */
    public static Set<String> findQualifiers(NodeWithAnnotations<?> n) {
        Set<String> qualifiers = JavaParserUtils.getAnnotationsNames(n).stream()
                .filter(a -> CDIUtil.nonQualifierAnnotations.stream().noneMatch(nonQual -> nonQual.endsWith(a)))
                .collect(Collectors.toSet());
        // THIS IS DANGEROUS!
        /*if (qualifiers.isEmpty()) {
            qualifiers.add("javax.inject.Default");
            qualifiers.add("jakarta.inject.Default");
            qualifiers.add("javax.inject.Any");
            qualifiers.add("jakarta.inject.Any");
        }*/
        return qualifiers;
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Calculates the name of a bean
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 2. Concepts" - Subchapter "2.6. Bean names"
     *
     * @param n
     * @return
     */
    public static String findName(NodeWithAnnotations<?> n) {
        if (n.isAnnotationPresent("Named")) {
            String name = NameIdentifierVisitor.getNameFromQualifier(n, "Named");
            if (name == null) {
                if (n instanceof FieldDeclaration && ((FieldDeclaration) n).getVariables().size() == 1) {
                    name = defaultName(((FieldDeclaration) n));
                } else if (n instanceof MethodDeclaration) {
                    name = defaultName(((MethodDeclaration) n));
                } else if (n instanceof ClassOrInterfaceDeclaration) {
                    name = defaultName(((ClassOrInterfaceDeclaration) n));
                }
            }
            return name;
        } else {
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Calculates the bean types of a given type
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 2. Concepts"- Subchapter "2.2. Bean types"
     * "Chapter 3. Programming model" - Subchapter "3.1. Managed beans" - Section "3.1.2. Bean types of a managed bean"
     * "Chapter 3. Programming model" - Subchapter "3.2. Producer methods" - Section "3.2.1. Bean types of a producer method"
     * "Chapter 3. Programming model" - Subchapter "3.3. Producer fields" - Section "3.3.1. Bean types of a producer field"
     *
     * @param providedType
     * @return
     */
    public static Set<ResolvedType> findBeanTypes(ResolvedType providedType) {
        Set<ResolvedType> beanTypes = new HashSet<>();
        beanTypes.add(providedType);

        // class or interface
        if (providedType.isReferenceType()) {
            ResolvedReferenceType resolvedReferenceType = providedType.asReferenceType();
            Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = resolvedReferenceType.
                    getTypeDeclaration();
            if (maybeTypeDeclaration.isPresent()) {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = maybeTypeDeclaration.get();
                List<ResolvedReferenceType> ancestors = resolvedReferenceTypeDeclaration.
                        getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList);
                beanTypes.addAll(ancestors);
            }
        }

        return beanTypes;
    }

    /**
     * Calculates the bean types of a given type declaration
     * IMPORTANT: does not include the type of this declaration
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 2. Concepts" - Subchapter "2.2. Bean types"
     * "Chapter 3. Programming model" - Subchapter "3.1. Managed beans" - Section "3.1.2. Bean types of a managed bean"
     * "Chapter 3. Programming model" - Subchapter "3.2. Producer methods" - Section "3.2.1. Bean types of a producer method"
     * "Chapter 3. Programming model" - Subchapter "3.3. Producer fields" - Section "3.3.1. Bean types of a producer field"
     *
     * @param providedTypeDeclaration
     * @return
     */
    public static Set<ResolvedType> findBeanTypes(ResolvedReferenceTypeDeclaration providedTypeDeclaration) {
        List<ResolvedReferenceType> ancestors = providedTypeDeclaration.
                getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList);
        return new HashSet<>(ancestors);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the default name of a producer field
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 3. Programming model"
     * Subchapter "3.3. Producer fields"
     * Section "3.3.3. Default bean name for a producer field"
     *
     * @param n
     * @return
     */
    private static String defaultName(FieldDeclaration n) {
        return n.getVariables().get(0).getNameAsString();
    }

    /**
     * Returns the default name of a producer method
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 3. Programming model"
     * Subchapter "3.2. Producer methods"
     * Section "3.2.4. Default bean name for a producer method"
     *
     * @param n
     * @return
     */
    private static String defaultName(MethodDeclaration n) {
        String methodName = n.getNameAsString();
        if (methodName.startsWith("get")) {
            methodName = methodName.substring(3);
            return methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
        } else {
            return methodName;
        }
    }

    /**
     * Returns the default name of a managed bean
     * <p>
     * Rationale:
     * A. Sabot-Durand, Contexts and Dependency Injection 3.0. Jakarta EE, 2020.
     * "Chapter 3. Programming model"
     * Subchapter "3.1. Managed beans"
     * Section "3.3.3. Default bean name for a managed bean"
     *
     * @param n
     * @return
     */
    private static String defaultName(ClassOrInterfaceDeclaration n) {
        String className = n.getNameAsString();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }
}
