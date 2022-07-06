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
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static java.util.logging.Level.FINE;

/**
 * Identifies accessed and assigned member variables
 * Considers FieldAccessExpr and NameExpr
 */
public class FieldAccessIdentifierVisitor extends AbstractIdentifierVisitor<
        Pair<Collection<ResolvedValueDeclaration>, Collection<ResolvedValueDeclaration>>
        > {

    //##################################################################################################################
    // Singleton Pattern

    private static final FieldAccessIdentifierVisitor singleton = new FieldAccessIdentifierVisitor();

    private FieldAccessIdentifierVisitor() {
    }

    public static Pair<Collection<ResolvedValueDeclaration>, Collection<ResolvedValueDeclaration>> identifyDependencies(Node n) {

        Collection<ResolvedValueDeclaration> resolvedAccessedFields = new HashSet<>();
        Collection<ResolvedValueDeclaration> resolvedAssignedFields = new HashSet<>();
        Pair<Collection<ResolvedValueDeclaration>, Collection<ResolvedValueDeclaration>> arg =
                new Pair<>(resolvedAccessedFields, resolvedAssignedFields);

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
    public void visit(FieldAccessExpr n, Pair<Collection<ResolvedValueDeclaration>, Collection<ResolvedValueDeclaration>> arg) {
        super.visit(n, arg);

        Collection<ResolvedValueDeclaration> resolvedAccessedFields = arg.getFirst();
        Collection<ResolvedValueDeclaration> resolvedAssignedFields = arg.getSecond();

        try {
            ResolvedValueDeclaration resolvedValueDeclaration = n.resolve();
            if (resolvedValueDeclaration.isField()) {
                if (isNotTargetOfAssignment(n)) {
                    resolvedAccessedFields.add(resolvedValueDeclaration);
                } else {
                    resolvedAssignedFields.add(resolvedValueDeclaration);
                }
            }
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }


    }

    @Override
    public void visit(NameExpr n, Pair<Collection<ResolvedValueDeclaration>, Collection<ResolvedValueDeclaration>> arg) {
        super.visit(n, arg);

        Collection<ResolvedValueDeclaration> resolvedAccessedFields = arg.getFirst();
        Collection<ResolvedValueDeclaration> resolvedAssignedFields = arg.getSecond();

        try {
            ResolvedValueDeclaration resolvedValueDeclaration = n.resolve();
            if (resolvedValueDeclaration.isField()) {
                if (isNotTargetOfAssignment(n)) {
                    resolvedAccessedFields.add(resolvedValueDeclaration);
                } else {
                    resolvedAssignedFields.add(resolvedValueDeclaration);
                }
            } else if (resolvedValueDeclaration.isEnumConstant()) {
                resolvedAccessedFields.add(resolvedValueDeclaration);
            }
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static boolean isNotTargetOfAssignment(Expression n) {
        Optional<Node> maybeParentNode = n.getParentNode();
        if (maybeParentNode.isPresent()) {
            Node parentNode = maybeParentNode.get();

            if (parentNode instanceof Expression) {
                Expression parentExpression = (Expression) parentNode;

                if (parentExpression.isAssignExpr()) {
                    AssignExpr assignExpr = parentExpression.asAssignExpr();

                    return n != assignExpr.getTarget();
                } else {
                    return isNotTargetOfAssignment(parentExpression);
                }
            }
        }
        return true;
    }
}
