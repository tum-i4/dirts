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
package edu.tum.sse.dirts.guice.util;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.tuples.Triple;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor.getNameFromQualifier;

public class GuiceUtil {

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> injectAnnotations = Set.of(
            new Triple<>("Inject", "com.google.inject.Inject",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Inject", "javax.inject.Inject",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation))
    );

    /**
     * Decides whether a node is injectable
     * <p>
     * Rationale:
     * https://github.com/google/guice/wiki/Injections
     * https://github.com/google/guice/wiki/JSR330
     *
     * @param n
     * @return
     */
    public static boolean isInjectNode(NodeWithAnnotations<?> n) {
        return JavaParserUtils.isAnnotatedWithAny(n, injectAnnotations)
                || (n instanceof MethodDeclaration && GuiceUtil.isProvidesMethod(((MethodDeclaration) n)));
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> autoBindSingletonAnnotations =
            Set.of(
                    new Triple<>("AutoBindSingleton", "com.netflix.governator.annotations.AutoBindSingleton",
                            Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                                    JavaParserUtils.AnnotationType.NormalAnnotation,
                                    JavaParserUtils.AnnotationType.SingleMemberAnnotation))
            );

    /**
     * Decides whether a class is able to be auto-bound via @AutoBindSingleton
     * <p>
     * Rationale:
     * https://github.com/Netflix/governator/wiki/AutoBindSingleton
     *
     * @param n
     * @return
     */
    public static Set<AnnotationExpr> getAutoBindSingletonAnnotation(ClassOrInterfaceDeclaration n) {
        return JavaParserUtils.getAnnotatedWithAny(n, autoBindSingletonAnnotations);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<String> providerClasses = Set.of(
            "com.google.inject.Provider",
            "javax.inject.Provider"
    );

    /**
     * Decides whether a type is a provider class
     * <p>
     * Rationale:
     * https://github.com/google/guice/wiki/ProviderBindings
     *
     * @param n
     * @return
     */
    public static boolean equalsProviderType(ClassOrInterfaceType n) {
        try {
            ResolvedType resolvedType = n.resolve();
            if (resolvedType.isReferenceType()) {
                for (String providerClass : providerClasses) {
                    if (JavaParserUtils.equalsTypeName(resolvedType.asReferenceType(), providerClass))
                        return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> providesAnnotations = Set.of(
            new Triple<>("Provides", "com.google.inject.Provides",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation)),
            new Triple<>("ProvidesIntoSet", "com.google.inject.multibindings.ProvidesIntoSet",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation)),
            new Triple<>("ProvidesIntoMap", "com.google.inject.multibindings.ProvidesIntoMap",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation))
    );

    /**
     * Decides whether a method os a provides method
     * <p>
     * Rationale:
     * https://github.com/google/guice/wiki/ProvidesMethods
     *
     * @param n
     * @return
     */
    public static boolean isProvidesMethod(MethodDeclaration n) {
        return JavaParserUtils.isAnnotatedWithAny(n, providesAnnotations);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> implementedByAnnotations = Set.of(
            new Triple<>("ImplementedBy", "com.google.inject.ImplementedBy",
                    Set.of(JavaParserUtils.AnnotationType.SingleMemberAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation))
    );

    /**
     * Returns the @ImplementedBy annotation if present
     * <p>
     * Rationale:
     * https://github.com/google/guice/wiki/JustInTimeBindings
     *
     * @param n
     * @return
     */
    public static Set<AnnotationExpr> getImplementedByAnnotation(ClassOrInterfaceDeclaration n) {
        return JavaParserUtils.getAnnotatedWithAny(n, implementedByAnnotations);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> providedByAnnotations = Set.of(
            new Triple<>("ProvidedBy", "com.google.inject.ProvidedBy",
                    Set.of(JavaParserUtils.AnnotationType.SingleMemberAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation))
    );

    /**
     * Returns the @ProvidedBy annotation if present
     * <p>
     * Rationale:
     * https://github.com/google/guice/wiki/JustInTimeBindings
     *
     * @param n
     * @return
     */
    public static Set<AnnotationExpr> getProvidedByAnnotation(ClassOrInterfaceDeclaration n) {
        return JavaParserUtils.getAnnotatedWithAny(n, providedByAnnotations);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Decides whether a class can be created via a just-in-time binding
     * <p>
     * Rationale:
     * https://github.com/google/guice/wiki/JustInTimeBindings
     *
     * @param n
     * @return
     */
    public static boolean isJustInTimeBinding(ConstructorDeclaration n) {
        return (n.getParameters().isEmpty() && !n.isPrivate()) ||
                GuiceUtil.isInjectNode(n);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<String> nonBindingAnnotations = new HashSet<>();

    static {
        injectAnnotations.forEach(i -> nonBindingAnnotations.add(i.getSecond()));
        providedByAnnotations.forEach(i -> nonBindingAnnotations.add(i.getSecond()));
        implementedByAnnotations.forEach(i -> nonBindingAnnotations.add(i.getSecond()));
        providesAnnotations.forEach(i -> nonBindingAnnotations.add(i.getSecond()));
        autoBindSingletonAnnotations.forEach(i -> nonBindingAnnotations.add(i.getSecond()));

        // Technically those are bindingAnnotations, but not useful for our purposes here
        nonBindingAnnotations.add("javax.inject.Named");
    }

    /**
     * Finds the names of BindingAnnotations of a node
     * <p>
     * Rationale:
     * https://github.com/google/guice/wiki/BindingAnnotations
     *
     * @param n
     * @return
     */
    public static Set<String> findQualifiers(NodeWithAnnotations<?> n) {
        return JavaParserUtils.getAnnotationsNames(n).stream()
                .filter(a -> GuiceUtil.nonBindingAnnotations.stream().noneMatch(nonQual -> nonQual.endsWith(a)))
                .collect(Collectors.toSet());
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Calculates the names of a binding
     * <p>
     * Rationale:
     * https://github.com/google/guice/wiki/BindingAnnotations
     *
     * @param n
     * @return
     */
    public static Optional<String> findName(NodeWithAnnotations<?> n) {
        return Optional.ofNullable(getNameFromQualifier(n, "Named"));
    }
}
