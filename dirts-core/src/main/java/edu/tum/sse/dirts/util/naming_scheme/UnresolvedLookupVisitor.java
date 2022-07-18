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
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults;
import edu.tum.sse.dirts.util.Container;

import java.util.*;

import static edu.tum.sse.dirts.util.naming_scheme.Names.DEBUG_INFO;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

class UnresolvedLookupVisitor extends VoidVisitorWithDefaults<Container<String>> {

    //##################################################################################################################
    // Attributes

    private final Map<Node, Integer> unresolvedUniqueNodeMap = new IdentityHashMap<>();

    private final Map<String, Integer> unresolvedOtherNodeMap = new HashMap<>();

    //##################################################################################################################
    // Methods

    private static <N> String getCustomName(Map<N, Integer> nMap, N n) {
        if (!nMap.containsKey(n)) {
            int value = nMap.size();
            nMap.put(n, value);
        }

        return "_" + nMap.get(n) + "_";
    }

    //##################################################################################################################
    // Visotor pattern

    @Override
    public void visit(ArrayType n, Container<String> arg) {
        // Should not be called
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(ClassOrInterfaceType n, Container<String> arg) {
        String identifier = n.asString();
        arg.content = (DEBUG_INFO ? "Unresolved_ClassOrInterfaceType" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(TypeParameter n, Container<String> arg) {
        String identifier = n.asString();
        arg.content = (DEBUG_INFO ? "Unresolved_TypeParameter" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }


    @Override
    public void visit(PrimitiveType n, Container<String> arg) {
        String identifier = n.asString();
        arg.content = (DEBUG_INFO ? "Unresolved_PrimitiveType" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(IntersectionType n, Container<String> arg) {
        // Should not be called
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(UnionType n, Container<String> arg) {
        // Should not be called
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(UnknownType n, Container<String> arg) {
        // Should not be called
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(VoidType n, Container<String> arg) {
        // Should not be called
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_ClassOrInterfaceDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + n.getNameAsString();
    }

    @Override
    public void visit(EnumDeclaration n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_EnumDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + n.getNameAsString();
    }

    @Override
    public void visit(AnnotationDeclaration n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_AnnotationDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + n.getNameAsString();
    }

    @Override
    public void visit(ConstructorDeclaration n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_ConstructorDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + getSignature(n.asCallableDeclaration());
    }

    @Override
    public void visit(MethodDeclaration n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_MethodDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + getSignature(n.asCallableDeclaration());
    }

    public String getSignature(CallableDeclaration<?> n) {
        StringBuilder partialSignature = new StringBuilder(n.getNameAsString() + "(");
        for (Iterator<Parameter> iterator = n.getParameters().iterator(); iterator.hasNext();
             partialSignature.append(iterator.hasNext() ? ", " : "")) {
            Parameter parameter = iterator.next();
            try {
                partialSignature.append(lookup(parameter.getType()).getFirst());
            } catch (RuntimeException e) {
                partialSignature.append("UnresolvedArgument");
            }
        }
        partialSignature.append(")");

        return partialSignature.toString();
    }

    @Override
    public void visit(EnumConstantDeclaration n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_EnumConstantDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + n.getNameAsString();
    }

    @Override
    public void visit(FieldDeclaration n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_FieldDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + n.getVariable(0).getNameAsString();
    }

    @Override
    public void visit(VariableDeclarator n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_VariableDeclarator" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + n;
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, Container<String> arg) {
        arg.content = (DEBUG_INFO ? "Unresolved_AnnotationMemberDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n) : "") + n.getNameAsString();
    }

    @Override
    public void visit(InitializerDeclaration n, Container<String> arg) {
        arg.content = "Unresolved_InitializerDeclaration" +
                getCustomName(unresolvedUniqueNodeMap, n);
    }

    @Override
    public void visit(MethodCallExpr n, Container<String> arg) {
        StringBuilder partialSignature = new StringBuilder(n.getNameAsString() + "(");
        for (Iterator<Expression> iterator = n.getArguments().iterator(); iterator.hasNext();
             partialSignature.append(iterator.hasNext() ? ", " : "")) {
            Expression argument = iterator.next();
            try {
                partialSignature.append(lookup(argument.calculateResolvedType()));
            } catch (RuntimeException e) {
                partialSignature.append("UnresolvedArgument");
            }
        }
        partialSignature.append(")");

        String identifier = partialSignature.toString();
        arg.content = (DEBUG_INFO ? "Unresolved_MethodCallExpr" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(MethodReferenceExpr n, Container<String> arg) {
        Expression scope = n.getScope();
        String name;
        if (scope.isTypeExpr()) {
            name = scope.asTypeExpr().getTypeAsString();
        } else if (scope.isThisExpr()) {
            name = "this";
        } else {
            name = "super";
        }

        String identifier = name + "::" + n.getIdentifier();
        arg.content = (DEBUG_INFO ? "Unresolved_MethodReferenceExpr" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(ObjectCreationExpr n, Container<String> arg) {
        StringBuilder partialSignature = new StringBuilder(n.getTypeAsString() + "(");
        for (Iterator<Expression> iterator = n.getArguments().iterator(); iterator.hasNext();
             partialSignature.append(iterator.hasNext() ? ", " : "")) {
            Expression argument = iterator.next();
            try {
                partialSignature.append(lookup(argument.calculateResolvedType()));
            } catch (RuntimeException e) {
                partialSignature.append("UnresolvedArgument");
            }
        }
        partialSignature.append(")");

        String identifier = partialSignature.toString();
        arg.content = (DEBUG_INFO ? "Unresolved_ObjectCreationExpr" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Container<String> arg) {
        StringBuilder partialSignature = new StringBuilder((n.isThis() ? "this" : "super") + "(");
        for (Iterator<Expression> iterator = n.getArguments().iterator(); iterator.hasNext();
             partialSignature.append(iterator.hasNext() ? ", " : "")) {
            Expression argument = iterator.next();
            try {
                partialSignature.append(lookup(argument.calculateResolvedType()));
            } catch (RuntimeException e) {
                partialSignature.append("UnresolvedArgument");
            }
        }
        partialSignature.append(")");

        String identifier = partialSignature.toString();
        arg.content = (DEBUG_INFO ? "Unresolved_ExplicitConstructorInvocationStmt" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(FieldAccessExpr n, Container<String> arg) {
        String identifier = n.getNameAsString();
        arg.content = (DEBUG_INFO ? "Unresolved_FieldAccessExpr" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(NameExpr n, Container<String> arg) {
        String identifier = n.getNameAsString();
        arg.content = (DEBUG_INFO ? "Unresolved_NameExpr" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(MarkerAnnotationExpr n, Container<String> arg) {
        String identifier = n.getNameAsString();
        arg.content = (DEBUG_INFO ? "Unresolved_MarkerAnnotationExpr" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(NormalAnnotationExpr n, Container<String> arg) {
        String identifier = n.getNameAsString();
        arg.content = (DEBUG_INFO ? "Unresolved_NormalAnnotationExpr" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }

    @Override
    public void visit(SingleMemberAnnotationExpr n, Container<String> arg) {
        String identifier = n.getNameAsString();
        arg.content = (DEBUG_INFO ? "Unresolved_SingleMemberAnnotationExpr" +
                getCustomName(unresolvedOtherNodeMap, identifier) : "") + identifier;
    }
}
