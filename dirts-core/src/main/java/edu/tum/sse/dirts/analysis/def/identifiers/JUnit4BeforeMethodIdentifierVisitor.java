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
package edu.tum.sse.dirts.analysis.def.identifiers;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.Log;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.logging.Level.FINE;

/**
 * Identifies MethodDeclarations that are annotated with @Before, @BeforeClass or @BeforeEach, @BeforeAll
 */
public class JUnit4BeforeMethodIdentifierVisitor extends AbstractIdentifierVisitor<
        Collection<ResolvedMethodDeclaration>
        > {

    //##################################################################################################################
    // Constants

    private static final Predicate<MethodDeclaration> isBeforeMethod = m -> m.isAnnotationPresent("Before")
            || m.isAnnotationPresent("BeforeClass")
            || m.isAnnotationPresent("BeforeAll")
            || m.isAnnotationPresent("BeforeEach");

    //##################################################################################################################
    // Attributes

    private final Set<ResolvedMethodDeclaration> resolvedBeforeMethods;

    //##################################################################################################################
    // Constructors

    public JUnit4BeforeMethodIdentifierVisitor(ClassOrInterfaceDeclaration n) {
        Collection<ClassOrInterfaceType> ancestors = new HashSet<>();

        //ancestors.addAll(n.getImplementedTypes());
        ancestors.addAll(n.getExtendedTypes());

        this.resolvedBeforeMethods = ancestors.stream()
                .map(i -> {
                    try {
                        ResolvedReferenceType resolvedType = i.resolve().asReferenceType();
                        return resolvedType.getAllMethodsVisibleToInheritors();
                    } catch (RuntimeException ignored) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(m -> !m.declaringType().getQualifiedName().equals("java.lang.Object"))
                .filter(m -> {
                    Optional<MethodDeclaration> maybeMethodDeclaration = m.toAst();
                    if (maybeMethodDeclaration.isPresent()) {
                        MethodDeclaration methodDeclaration = maybeMethodDeclaration.orElseThrow();
                        return isBeforeMethod.test(methodDeclaration);
                    }
                    return false;
                }).collect(Collectors.toSet());
    }

    //##################################################################################################################
    // Getters

    public Set<ResolvedMethodDeclaration> getResolvedBeforeMethods() {
        return resolvedBeforeMethods;
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(MethodDeclaration n, Collection<ResolvedMethodDeclaration> arg) {
        if (isBeforeMethod.test(n)) {
            try {
                ResolvedMethodDeclaration methodDecl = n.resolve();
                arg.add(methodDecl);
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
