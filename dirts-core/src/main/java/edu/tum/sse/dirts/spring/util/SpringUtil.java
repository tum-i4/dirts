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
package edu.tum.sse.dirts.spring.util;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.tuples.Triple;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor.getNameFromQualifier;

/**
 * Utilities for spring
 */
public class SpringUtil {

    /**
     * Calculates the names of a bean
     * <p>
     * Rationale:
     * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.9 Annotation-based container configuration"
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.10 Classpath scanning and managed components"
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.11 Using JSR 330 Standard Annotations"
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.12 Java-based container configuration"
     *
     * @param n
     * @param beanAnnotation
     * @return
     */
    public static Set<String> findNames(BodyDeclaration<?> n, AnnotationExpr beanAnnotation) {
        Set<String> names = new HashSet<>();

        // from @Qualifier or @Named annotation
        names.add(getNameFromQualifier(n, "Qualifier"));
        names.add(getNameFromQualifier(n, "Named"));

        // from name in beanAnnotation
        if (beanAnnotation.isSingleMemberAnnotationExpr()) {
            SingleMemberAnnotationExpr singleMemberAnnotationExpr = beanAnnotation.asSingleMemberAnnotationExpr();
            Expression namesExpr = singleMemberAnnotationExpr.getMemberValue();
            extractNamesFromExpr(namesExpr, names);

        } else if (beanAnnotation.isNormalAnnotationExpr()) {
            NormalAnnotationExpr normalAnnotationExpr = beanAnnotation.asNormalAnnotationExpr();
            Set<Expression> namesExpressions = normalAnnotationExpr.getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("name") || p.getNameAsString().equals("value"))
                    .map(MemberValuePair::getValue)
                    .collect(Collectors.toSet());
            for (Expression namesExpr : namesExpressions) {
                extractNamesFromExpr(namesExpr, names);
            }

        } else {
            if (n.isMethodDeclaration()) {
                // if not specified in the @Bean annotation, the bean gets its name from the method
                names.add(n.asMethodDeclaration().getNameAsString());
            } else if (n.isClassOrInterfaceDeclaration()) {
                // if not specified in the @Component annotation, the component gets its name from the class
                String className = n.asClassOrInterfaceDeclaration().getNameAsString();
                names.add(Character.toLowerCase(className.charAt(0)) + className.substring(1));
            }
        }
        return names.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static void extractNamesFromExpr(Expression namesExpr, Set<String> names) {
        if (namesExpr.isStringLiteralExpr()) {
            names.add(namesExpr.asStringLiteralExpr().asString());
        } else if (namesExpr.isArrayInitializerExpr()) {
            ArrayInitializerExpr namesArray = namesExpr.asArrayInitializerExpr();
            for (Expression nameExpr : namesArray.getValues()) {
                if (nameExpr.isStringLiteralExpr()) {
                    names.add(nameExpr.asStringLiteralExpr().asString());
                }
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> injectAnnotations = Set.of(
            new Triple<>("Autowired", "org.springframework.beans.factory.annotation.Autowired",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Required", "org.springframework.beans.factory.annotation.Required",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Inject", "javax.inject.Inject",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Resource", "javax.inject.Resource",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Resource", "javax.annotation.Resource",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation))
    );

    /**
     * Decides whether a node is injectable
     * <p>
     * Rationale:
     * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.9 Annotation-based container configuration"
     *
     * @param n
     * @return
     */
    public static boolean isInjectNode(NodeWithAnnotations<?> n) {
        return JavaParserUtils.isAnnotatedWithAny(n, injectAnnotations);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> beanAnnotations = Set.of(
            new Triple<>("Bean", "org.springframework.context.annotation.Bean",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation))
    );

    /**
     * Decides if a mmethod declaration declares a bean and returns the annotation that declares it as a bean
     * <p>
     * Rationale:
     * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.11 Using JSR 330 Standard Annotations"
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.12 Java-based container configuration"
     *
     * @param n
     * @return
     */
    public static Set<AnnotationExpr> getBeanAnnotation(MethodDeclaration n) {
        return JavaParserUtils.getAnnotatedWithAny(n, beanAnnotations);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final Set<Triple<String, String, Set<JavaParserUtils.AnnotationType>>> componentAnnotations = Set.of(
            new Triple<>("Component", "org.springframework.stereotype.Component",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Repository", "org.springframework.stereotype.Repository",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Service", "org.springframework.stereotype.Service",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Controller", "org.springframework.stereotype.Controller",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation)),
            new Triple<>("Named", "javax.inject.Named",
                    Set.of(JavaParserUtils.AnnotationType.MarkerAnnotation,
                            JavaParserUtils.AnnotationType.NormalAnnotation,
                            JavaParserUtils.AnnotationType.SingleMemberAnnotation))
    );

    /**
     * Decides if a class or interface declaration declares a component
     * and returns the annotation that declares it as a component
     * <p>
     * Rationale:
     * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.10 Classpath scanning and managed components"
     * "Part II. Core Technologies" -  Chapter "3 The IoC Container"- Subchapter "3.11 Using JSR 330 Standard Annotations"
     *
     * @param n
     * @return
     */
    public static Set<AnnotationExpr> getComponentAnnotation(ClassOrInterfaceDeclaration n) {
        return JavaParserUtils.getAnnotatedWithAny(n, componentAnnotations);
    }
}
