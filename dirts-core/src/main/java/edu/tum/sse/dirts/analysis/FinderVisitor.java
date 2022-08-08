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
package edu.tum.sse.dirts.analysis;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import org.apache.maven.surefire.api.testset.TestFilter;

import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * A generic FinderVisitor that can be extended to collect specific Nodes in the AST
 * A Predicate can be used to filter
 *
 * @param <T> Collection where the Nodes that should be collected are inserted
 */
@SuppressWarnings("unused")
public abstract class FinderVisitor<T, P extends BodyDeclaration<?>> extends AbstractTruncatedVisitor<T> {

    //##################################################################################################################
    // Static methods

    public static boolean testClassDeclaration(ClassOrInterfaceDeclaration n, TestFilter<String, String> testFilter) {
        return n.getMethods().stream().anyMatch(m -> FinderVisitor.testMethodDeclaration(m, testFilter));
    }

    public static boolean testMethodDeclaration(MethodDeclaration n, TestFilter<String, String> testFilter) {
        try {
            ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();

            ResolvedReferenceTypeDeclaration declaringType = resolvedMethodDeclaration.declaringType();
            String declaringTypeName = lookup(declaringType);
            String methodName = resolvedMethodDeclaration.getName();

            return testFilter.shouldRun(declaringTypeName.replaceAll("\\.", "/") + ".class", methodName);
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    public static boolean recursiveMemberTest(ClassOrInterfaceDeclaration n, TestFilter<String, String> testFilter) {
        boolean memberTest = n.getMembers().stream()
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .anyMatch(classOrInterfaceDeclaration -> testClassDeclaration(classOrInterfaceDeclaration, testFilter));
        return memberTest
                || n.getMembers().stream()
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .anyMatch(classOrInterfaceDeclaration -> recursiveMemberTest(classOrInterfaceDeclaration, testFilter));
    }
}
