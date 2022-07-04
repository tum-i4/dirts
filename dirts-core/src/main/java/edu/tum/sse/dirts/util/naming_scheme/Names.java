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
package edu.tum.sse.dirts.util.naming_scheme;

import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserAnonymousClassDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.util.Container;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central naming scheme to identify nodes in the DependencyGraph
 * Provides lookup() for several code objects
 */
public class Names {

    //##################################################################################################################
    // Attributes

    public static boolean DEBUG_INFO = false;

    private static final Map<ResolvedValueDeclaration, Integer> unknownValueDeclarations =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private static final UnresolvedLookupVisitor UNRESOLVED_LOOKUP_VISITOR;
    private static final ResolvingLookupVisitor RESOLVING_LOOKUP_VISITOR;

    static {
        UNRESOLVED_LOOKUP_VISITOR = new UnresolvedLookupVisitor();
        RESOLVING_LOOKUP_VISITOR = new ResolvingLookupVisitor(UNRESOLVED_LOOKUP_VISITOR);
    }

    //##################################################################################################################
    // Lookup for Annotations

    public static String lookupAnnotationsNode(String className) {
        return className + "." + "!annotations";
    }

    //##################################################################################################################
    // lookup for types that are only referenced in Strings for example in xml beans

    public static Optional<ResolvedReferenceTypeDeclaration> lookupTypeDeclaration(String name,
                                                                                   TypeSolver typeSolver) {
        SymbolReference<ResolvedReferenceTypeDeclaration> resolved = typeSolver.tryToSolveType(name);
        if (resolved.isSolved()) {
            return Optional.of(resolved.getCorrespondingDeclaration());
        } else {
            return Optional.empty();
        }
    }

    public static Set<ResolvedMethodDeclaration> lookupMethods(String typeName,
                                                               String methodName,
                                                               TypeSolver typeSolver) {
        Optional<ResolvedReferenceTypeDeclaration> maybeType = lookupTypeDeclaration(typeName, typeSolver);
        if (maybeType.isPresent()) {
            ResolvedReferenceTypeDeclaration referenceTypeDeclaration = maybeType.get();
            return referenceTypeDeclaration.getDeclaredMethods().stream().filter(m -> m.getName().equals(methodName))
                    .collect(Collectors.toSet());
        } else {
            return new HashSet<>();
        }
    }

    public static Set<ResolvedConstructorDeclaration> lookupConstructors(String typeName,
                                                                         TypeSolver typeSolver) {
        Optional<ResolvedReferenceTypeDeclaration> maybeType = lookupTypeDeclaration(typeName, typeSolver);
        if (maybeType.isPresent()) {
            ResolvedReferenceTypeDeclaration referenceTypeDeclaration = maybeType.get();
            return new HashSet<>(referenceTypeDeclaration.getConstructors());
        } else {
            return new HashSet<>();
        }
    }

    //##################################################################################################################
    // Lookup for nodes in the ast

    public static String lookupNode(Node n, DependencyGraph dependencyGraph) {
        Pair<String, Optional<String>> lookup = lookup(n);

        String node = lookup.getFirst();
        lookup.getSecond().ifPresent(e -> dependencyGraph.addMessage(node, e));

        return node;
    }

    public static Pair<String, Optional<String>> lookup(Node subject) {
        Container<String> stringContainer = new Container<>(null);
        Container<Optional<String>> messageContainer = new Container<>(Optional.empty());
        subject.accept(RESOLVING_LOOKUP_VISITOR, new Pair<>(stringContainer, messageContainer));
        return new Pair<>(stringContainer.content, messageContainer.content);
    }

    //##################################################################################################################
    // Lookup for already resolved things
    // (no support for visitors)

    public static String lookup(ResolvedTypeDeclaration resolvedTypeDeclaration) {
        return resolvedTypeDeclaration.getQualifiedName();
    }

    public static String lookup(ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration) {
        try {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration =
                    resolvedMethodLikeDeclaration.declaringType();
            if (resolvedReferenceTypeDeclaration instanceof JavaParserAnonymousClassDeclaration) {
                return "#Anonymous_method";
            } else {
                return resolvedMethodLikeDeclaration.getQualifiedSignature();
            }
        } catch (RuntimeException e) {
            return resolvedMethodLikeDeclaration.getQualifiedName() + "(Unsolved arguments...)";
        }
    }

    //##################################################################################################################
    // ResolvedValueDeclaration

    public static String lookup(ResolvedTypeDeclaration declaringType, ResolvedValueDeclaration resolvedValueDeclaration) {
        if (resolvedValueDeclaration.isField()) {                                               // Field
            return lookup(declaringType, resolvedValueDeclaration.asField());
        } else if (resolvedValueDeclaration.isEnumConstant()) {                                 // EnumConstant
            return lookup(declaringType, resolvedValueDeclaration.asEnumConstant());
        } else if (resolvedValueDeclaration instanceof ResolvedAnnotationMemberDeclaration) {   // AnnotationMember
            return lookup(declaringType, (ResolvedAnnotationMemberDeclaration) resolvedValueDeclaration);
        } else if (resolvedValueDeclaration.isParameter()) {                                    // Parameter
            return lookup(declaringType, resolvedValueDeclaration.asParameter());
        } else if (resolvedValueDeclaration.isPattern()) {                                      // Pattern
            return lookup(declaringType, resolvedValueDeclaration.asPattern());
        } else if (isArrayLengthExpression(resolvedValueDeclaration)) {                         // ArrayLengthExpression
            return "Array.length";
        } else {
            return getCustomName(unknownValueDeclarations, resolvedValueDeclaration);
        }
    }

    public static String lookup(ResolvedTypeDeclaration declaringType, ResolvedFieldDeclaration resolvedFieldDeclaration) {
        return lookup(declaringType) + "." + resolvedFieldDeclaration.getName();
    }

    public static String lookup(ResolvedTypeDeclaration declaringType, ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration) {
        return lookup(declaringType) + "." + resolvedEnumConstantDeclaration.getName();
    }

    public static String lookup(ResolvedTypeDeclaration declaringType, ResolvedAnnotationMemberDeclaration resolvedAnnotationMemberDeclaration) {
        return lookup(declaringType) + "." + resolvedAnnotationMemberDeclaration.getName();
    }

    public static String lookup(ResolvedTypeDeclaration declaringType, ResolvedParameterDeclaration resolvedParameterDeclaration) {
        return lookup(declaringType) + "." + resolvedParameterDeclaration.getName();
    }

    public static String lookup(ResolvedTypeDeclaration declaringType, ResolvedPatternDeclaration resolvedPatternDeclaration) {
        return lookup(declaringType) + "." + resolvedPatternDeclaration.getName();
    }

    //##################################################################################################################
    // ResolvedType (probably without accessible declaration)

    // Warning This should never be used as name for nodes in the graph, since it may retain type parameters
    public static String lookup(ResolvedType type) {
        try {
            return type.describe();
        } catch (RuntimeException ignored) {
            return type.toString();
        }
    }

    //##################################################################################################################
    // Auxiliary methods

    private static <N> String getCustomName(Map<N, Integer> nMap, N n) {
        if (!nMap.containsKey(n)) {
            int value = nMap.size();
            nMap.put(n, value);
        }

        return (DEBUG_INFO ? "UnknownType_" + nMap.get(n) + n.getClass().getSimpleName() : "");
    }

    private static boolean isArrayLengthExpression(ResolvedValueDeclaration resolvedValueDeclaration) {
        Class<JavaSymbolSolver> symbolSolverClass = JavaSymbolSolver.class;
        Class<?> arrayLengthValueDeclaration = Arrays.stream(symbolSolverClass.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("ArrayLengthValueDeclaration"))
                .findFirst()
                .orElse(null);

        return arrayLengthValueDeclaration != null && arrayLengthValueDeclaration.isInstance(resolvedValueDeclaration);
    }
}