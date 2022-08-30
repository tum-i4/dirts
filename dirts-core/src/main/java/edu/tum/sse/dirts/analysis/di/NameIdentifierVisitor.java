/*
 * Copyright 2022. The dirts authors.
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
package edu.tum.sse.dirts.analysis.di;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import edu.tum.sse.dirts.util.Container;

import java.util.Optional;

/**
 * Identifies the name of the injected bean based on the @Qualifier annotation
 *
 * Is only able to identify names provided as StringLiterals
 * Tracing static variables of type String would be far more advanced
 */
public class NameIdentifierVisitor extends VoidVisitorAdapter<Container<String>> {

    /**
     * This class can only extract Names in String literals
     * <p>
     * It is possible to specify Names in static final fields
     * To account for that would be too complicated
     * Instead "" is returned
     */

    //##################################################################################################################
    // Singleton Pattern

    private static final NameIdentifierVisitor singleton = new NameIdentifierVisitor();

    public static String getNameFromQualifier(NodeWithAnnotations<?> injectedNode, String annotationName) {
        Container<String> arg = new Container<>(null);
        Optional<AnnotationExpr> maybeQualifier = injectedNode.getAnnotationByName(annotationName);
        maybeQualifier.ifPresent(annotationExpr -> annotationExpr.accept(singleton, arg));
        return arg.content;
    }

    public static String getNameFromNamesDotNamed(Expression n) {
        Container<String> arg = new Container<>(null);
        n.accept(singleton, arg);
        return arg.content;
    }

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(MethodCallExpr n, Container<String> arg) {
        if (n.getNameAsString().equals("named")) {
            NodeList<Expression> arguments = n.getArguments();
            if (arguments.size() == 1) {
                Expression expression = arguments.get(0);
                if (expression.isStringLiteralExpr()) {
                    StringLiteralExpr stringLiteralExpr = expression.asStringLiteralExpr();
                    arg.content = stringLiteralExpr.getValue();
                }
                // We do not support static variables of type String
            }
        } else {
            super.visit(n, arg);
        }
    }

    @Override
    public void visit(MarkerAnnotationExpr n, Container<String> arg) {
    }

    @Override
    public void visit(SingleMemberAnnotationExpr n, Container<String> arg) {
        Expression memberValue = n.getMemberValue();
        if (memberValue.isStringLiteralExpr())
            arg.content = memberValue.asStringLiteralExpr().asString();
        else
            arg.content = "";
    }

    @Override
    public void visit(NormalAnnotationExpr n, Container<String> arg) {
        MemberValuePair pair = n.getPairs().get(0);
        Expression value = pair.getValue();
        if (value.isStringLiteralExpr())
            arg.content = value.asStringLiteralExpr().asString();
        else
            arg.content = "";
    }
}
