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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.HashSet;

import static java.util.logging.Level.FINE;

/**
 * Identifies Annotations present on nodes in the AST
 * Considers all three types of Annotations (MarkerAnnotationExpr, SingleMemberAnnotationExpr, NormalAnnotationExpr)
 */
public class AnnotationIdentifierVisitor extends AbstractIdentifierVisitor<
        Collection<ResolvedAnnotationDeclaration>
        > {

    //##################################################################################################################
    // Singleton Pattern

    private static final AnnotationIdentifierVisitor singleton = new AnnotationIdentifierVisitor();

    private AnnotationIdentifierVisitor() {
    }

    public static Collection<ResolvedAnnotationDeclaration> identifyDependencies(Node n) {
        Collection<ResolvedAnnotationDeclaration> arg = new HashSet<>();

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
    public void visit(MarkerAnnotationExpr n, Collection<ResolvedAnnotationDeclaration> arg) {
        super.visit(n, arg);


        try {
            ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = n.resolve();
            arg.add(resolvedAnnotationDeclaration);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void visit(SingleMemberAnnotationExpr n, Collection<ResolvedAnnotationDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = n.resolve();
            arg.add(resolvedAnnotationDeclaration);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void visit(NormalAnnotationExpr n, Collection<ResolvedAnnotationDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = n.resolve();
            arg.add(resolvedAnnotationDeclaration);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
