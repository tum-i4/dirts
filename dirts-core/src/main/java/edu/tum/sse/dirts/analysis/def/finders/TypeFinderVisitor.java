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
package edu.tum.sse.dirts.analysis.def.finders;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

/**
 * Collects all Type-nodes
 * (ClassOrInterfaceDeclaration, EnumDeclaration, AnnotationDeclaration)
 */
public class TypeFinderVisitor extends VoidVisitorAdapter<List<TypeDeclaration<?>>> {

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, List<TypeDeclaration<?>> arg) {
        super.visit(n, arg);
        arg.add(n);
    }

    @Override
    public void visit(EnumDeclaration n, List<TypeDeclaration<?>> arg) {
        super.visit(n, arg);
        arg.add(n);
    }

    @Override
    public void visit(AnnotationDeclaration n, List<TypeDeclaration<?>> arg) {
        super.visit(n, arg);
        arg.add(n);
    }
}
