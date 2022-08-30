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
package edu.tum.sse.dirts.util;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.util.tuples.Triple;

import java.util.*;
import java.util.function.Function;

public class JavaParserUtils {

    public static boolean RESTRICTIVE = false;

    /*
     * Source of idea: Class ResolvedReferenceTypeDeclaration of JavaParser
     * https://github.com/javaparser/javaparser/blob/a6744e0ccc65710bbf970bbdb428629bc0b30958/javaparser-core/src/main/java/com/github/javaparser/resolution/declarations/ResolvedReferenceTypeDeclaration.java#L122
     *
     * Adaptations to find resolvable ancestors without throwing exceptions
     */
    public static Function<ResolvedReferenceTypeDeclaration, List<ResolvedReferenceType>>
            depthFirstFuncAcceptIncompleteList = (rrtd) -> {
        List<ResolvedReferenceType> ancestors = new ArrayList<>();
        Queue<ResolvedReferenceTypeDeclaration> queue = new LinkedList<>();
        queue.add(rrtd);
        while (!queue.isEmpty()) {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = queue.poll();
            for (ResolvedReferenceType ancestor : resolvedReferenceTypeDeclaration.getAncestors(true)) { // true is particularly important here
                ancestors.add(ancestor);
                ancestor.getTypeDeclaration().ifPresent(queue::add);
            }
        }
        return ancestors;
    };

    public static Set<String> getAnnotationsNames(NodeWithAnnotations<?> n) {
        Set<String> ret = new HashSet<>();
        for (AnnotationExpr annotation : n.getAnnotations()) {
            try {
                ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = annotation.resolve();
                ret.add(resolvedAnnotationDeclaration.getQualifiedName());
            } catch (RuntimeException ignored) {
                ret.add(annotation.getNameAsString());
            }
        }
        return ret;
    }

    public static boolean isAnnotatedWithAny(NodeWithAnnotations<?> n,
                                             Set<Triple<String, String, Set<AnnotationType>>> annotations) {
        for (Triple<String, String, Set<AnnotationType>> annotation : annotations) {
            Optional<AnnotationType> maybeAnnotation =
                    getAnnotationTypePresent(n, annotation.getFirst(), annotation.getSecond());
            Set<AnnotationType> types = annotation.getThird();
            if (maybeAnnotation.isPresent() && types.stream().anyMatch(t -> maybeAnnotation.get() == t)) {
                return true;
            }
        }
        return false;
    }

    public static Set<AnnotationExpr> getAnnotatedWithAny(NodeWithAnnotations<?> n,
                                                          Set<Triple<String, String, Set<AnnotationType>>> annotations) {
        Set<AnnotationExpr> ret = new HashSet<>();
        for (Triple<String, String, Set<AnnotationType>> annotation : annotations) {
            Optional<AnnotationExpr> maybeAnnotation =
                    getAnnotationExprPresent(n, annotation.getFirst(), annotation.getSecond());
            Set<AnnotationType> types = annotation.getThird();
            if (maybeAnnotation.isPresent()) {
                AnnotationExpr annotationExpr = maybeAnnotation.get();
                if ((annotationExpr.isSingleMemberAnnotationExpr() && types.contains(AnnotationType.SingleMemberAnnotation))
                        || (annotationExpr.isMarkerAnnotationExpr() && types.contains(AnnotationType.MarkerAnnotation))
                        || (annotationExpr.isNormalAnnotationExpr() && types.contains(AnnotationType.NormalAnnotation)))
                    ret.add(annotationExpr);
            }
        }
        return ret;
    }

    public static Optional<AnnotationExpr> getAnnotationExprPresent(NodeWithAnnotations<?> n,
                                                                    String simpleName,
                                                                    String qualifiedAnnotation) {
        Optional<AnnotationExpr> annotationByName = n.getAnnotationByName(simpleName);
        if (annotationByName.isPresent()) {
            AnnotationExpr annotationExpr = annotationByName.get();

            if (RESTRICTIVE) {
                try {

                    ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = annotationExpr.resolve();
                    if (resolvedAnnotationDeclaration.getQualifiedName().equals(qualifiedAnnotation)) {
                        return Optional.of(annotationExpr);
                    } else {
                        return Optional.empty();
                    }
                } catch (RuntimeException ignored) {
                    if (annotationExpr.getNameAsString().equals(simpleName)) {
                        return Optional.of(annotationExpr);
                    } else {
                        return Optional.empty();
                    }
                }
            } else {
                if (annotationExpr.getNameAsString().equals(simpleName)) {
                    return Optional.of(annotationExpr);
                } else {
                    return Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }

    public static Optional<AnnotationType> getAnnotationTypePresent(NodeWithAnnotations<?> n,
                                                                    String simpleName,
                                                                    String qualifiedAnnotation) {
        Optional<AnnotationExpr> annotationByName = n.getAnnotationByName(simpleName);
        if (annotationByName.isPresent()) {
            AnnotationExpr annotationExpr = annotationByName.get();
            AnnotationType ret;
            if (annotationExpr.isNormalAnnotationExpr()) {
                ret = AnnotationType.NormalAnnotation;
            } else if (annotationExpr.isSingleMemberAnnotationExpr()) {
                ret = AnnotationType.SingleMemberAnnotation;
            } else {
                ret = AnnotationType.MarkerAnnotation;
            }

            if (RESTRICTIVE) {
                try {
                    ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = annotationExpr.resolve();
                    if (resolvedAnnotationDeclaration.getQualifiedName().equals(qualifiedAnnotation)) {
                        return Optional.of(ret);
                    } else {
                        return Optional.empty();
                    }
                } catch (RuntimeException ignored) {
                    if (annotationExpr.getNameAsString().equals(simpleName)) {
                        return Optional.of(ret);
                    } else {
                        return Optional.empty();
                    }
                }
            } else {
                if (annotationExpr.getNameAsString().equals(simpleName)) {
                    return Optional.of(ret);
                } else {
                    return Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }

    public enum AnnotationType {
        MarkerAnnotation, SingleMemberAnnotation, NormalAnnotation
    }

    public static ResolvedType extractClassType(ResolvedReferenceType expressionType, Set<String> allowedTypes) {
        ResolvedReferenceType resolvedReferenceType = expressionType.asReferenceType();
        if (allowedTypes.stream().anyMatch(a -> a.equals(resolvedReferenceType.getQualifiedName()))) {
            return resolvedReferenceType.getTypeParametersMap().get(0).b;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static boolean equalsMethodName(MethodCallExpr methodCallExpr,
                                           String simpleName,
                                           String qualifiedSignature) {
        if (RESTRICTIVE) {
            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = methodCallExpr.resolve();
                return resolvedMethodDeclaration.getQualifiedSignature().equals(qualifiedSignature);
            } catch (RuntimeException ignored) {
                return methodCallExpr.getNameAsString().equals(simpleName);
            }
        } else {
            return methodCallExpr.getNameAsString().equals(simpleName);
        }
    }

    public static boolean equalsTypeName(ResolvedReferenceType resolvedReferenceType, String qualifiedName) {
        return resolvedReferenceType.asReferenceType().getQualifiedName().equals(qualifiedName);
    }
}
