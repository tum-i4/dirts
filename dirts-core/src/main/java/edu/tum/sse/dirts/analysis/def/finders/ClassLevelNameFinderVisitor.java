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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import edu.tum.sse.dirts.analysis.FinderVisitor;

import java.util.Map;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Collects all class level nodes and their names
 * (CompilationUnit, ClassOrInterfaceDeclaration, EnumDeclaration, AnnotationDeclaration)
 */
public class ClassLevelNameFinderVisitor extends FinderVisitor<Map<String, Node>, TypeDeclaration<?>> {

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(CompilationUnit n, Map<String, Node> arg) {
        super.visit(n, arg);
        arg.put(lookup(n).getFirst(), n);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<String, Node> arg) {
        super.visit(n, arg);
        arg.put(lookup(n).getFirst(), n);
    }

    @Override
    public void visit(EnumDeclaration n, Map<String, Node> arg) {
        super.visit(n, arg);
        arg.put(lookup(n).getFirst(), n);
    }

    @Override
    public void visit(AnnotationDeclaration n, Map<String, Node> arg) {
        super.visit(n, arg);
        arg.put(lookup(n).getFirst(), n);
    }

    //******************************************************************************************************************
    // Stop here, further proceeding is not necessary

    @Override
    public void visit(ConstructorDeclaration n, Map<String, Node> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Map<String, Node> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(EnumConstantDeclaration n, Map<String, Node> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(FieldDeclaration n, Map<String, Node> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, Map<String, Node> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(InitializerDeclaration n, Map<String, Node> arg) {
        //super.visit(n, arg);
    }
}
