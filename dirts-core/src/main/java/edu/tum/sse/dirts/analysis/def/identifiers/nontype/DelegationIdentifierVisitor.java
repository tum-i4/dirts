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
package edu.tum.sse.dirts.analysis.def.identifiers.nontype;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.HashSet;

import static java.util.logging.Level.FINE;


/**
 * Identifies delegates of nodes in the AST
 * Considers all types of callables (MethodCallExpr, ObjectCreationExpr, ExplicitConstructorInvocationStmt)
 * Additionally considers MethodReferenceExpr
 */
public class DelegationIdentifierVisitor extends AbstractIdentifierVisitor<
        Collection<ResolvedMethodLikeDeclaration>
        > {

    //##################################################################################################################
    // Singleton Pattern

    private static final DelegationIdentifierVisitor singleton = new DelegationIdentifierVisitor();

    private DelegationIdentifierVisitor() {
    }

    public static Collection<ResolvedMethodLikeDeclaration> identifyDependencies(Node n) {
        Collection<ResolvedMethodLikeDeclaration> arg = new HashSet<>();

        if (n instanceof TypeDeclaration<?>) {
            TypeDeclaration<?> typeDeclaration = ((TypeDeclaration<?>) n);
            typeDeclaration.getMembers().forEach(m -> m.accept(singleton, arg));
        } else {
            n.accept(singleton, arg);
        }

        return arg;
    }

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(MethodCallExpr n, Collection<ResolvedMethodLikeDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedMethodDeclaration methodDecl = n.resolve();
            arg.add(methodDecl);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void visit(MethodReferenceExpr n, Collection<ResolvedMethodLikeDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedMethodDeclaration methodDecl = n.resolve();
            arg.add(methodDecl);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void visit(ObjectCreationExpr n, Collection<ResolvedMethodLikeDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedConstructorDeclaration constructorDecl = n.resolve();
            arg.add(constructorDecl);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Collection<ResolvedMethodLikeDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedConstructorDeclaration constructorDecl = n.resolve();
            arg.add(constructorDecl);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}