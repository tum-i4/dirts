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

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

/**
 * Can be extended to identify connections between NonType-nodes in the AST
 * Provides final methods to stop before inner classes, enums or annotations
 *
 * @param <T> accumulator
 */
public abstract class AbstractIdentifierVisitor<T> extends AbstractTruncatedVisitor<T> {

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, T arg) {
        // in case of inner classes stop
        // no super.visit(n, arg);
    }

    @Override
    public void visit(EnumDeclaration n, T arg) {
        // in case of inner enums stop
        // no super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationDeclaration n, T arg) {
        // in case of inner annotations stop
        // no super.visit(n, arg);
    }
}
