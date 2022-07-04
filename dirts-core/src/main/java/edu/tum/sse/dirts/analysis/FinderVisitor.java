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
import edu.tum.sse.dirts.analysis.def.finders.TypeNameFinderVisitor;

import static edu.tum.sse.dirts.analysis.def.finders.TypeTestFinderVisitor.testClassDeclaration;

/**
 * A generic FinderVisitor that can be extended to collect specific Nodes in the AST
 * A Predicate can be used to filter
 *
 * @param <T> Collection where the Nodes that should be collected are inserted
 */
public abstract class FinderVisitor<T> extends AbstractTruncatedVisitor<T> {

    public static boolean recursiveMemberTest(ClassOrInterfaceDeclaration n) {
        boolean memberTest = n.getMembers().stream()
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .anyMatch(testClassDeclaration);
        return memberTest
                || n.getMembers().stream()
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .anyMatch(TypeNameFinderVisitor::recursiveMemberTest);
    }
}
