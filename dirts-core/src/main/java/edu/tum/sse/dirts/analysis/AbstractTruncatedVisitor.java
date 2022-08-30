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
package edu.tum.sse.dirts.analysis;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitor;

/**
 * Generic Visitor that prevents traversing irrelevant nodes like Modifiers or Comments
 * @param <A>
 */
@SuppressWarnings("CommentedOutCode")
public abstract class AbstractTruncatedVisitor<A> implements VoidVisitor<A> {

    /*
    These method bodies were all taken from VoidVisitorAdapter and all unnecessary lines commented out
    Source: https://github.com/javaparser/javaparser/blob/06265a3ab5d1dfca31cc480640209f5f55179f45/javaparser-core/src/main/java/com/github/javaparser/ast/visitor/VoidVisitorAdapter.java
     */

    @Override
    public void visit(final AnnotationDeclaration n, final A arg) {
        n.getMembers().forEach(p -> p.accept(this, arg));
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final AnnotationMemberDeclaration n, final A arg) {
        n.getDefaultValue().ifPresent(l -> l.accept(this, arg));
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getType().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayAccessExpr n, final A arg) {
        n.getIndex().accept(this, arg);
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayCreationExpr n, final A arg) {
        //n.getElementType().accept(this, arg);
        n.getInitializer().ifPresent(l -> l.accept(this, arg));
        //n.getLevels().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayInitializerExpr n, final A arg) {
        n.getValues().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final AssertStmt n, final A arg) {
        n.getCheck().accept(this, arg);
        n.getMessage().ifPresent(l -> l.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final AssignExpr n, final A arg) {
        n.getTarget().accept(this, arg);
        n.getValue().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BinaryExpr n, final A arg) {
        n.getLeft().accept(this, arg);
        n.getRight().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BlockComment n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BlockStmt n, final A arg) {
        n.getStatements().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BooleanLiteralExpr n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final BreakStmt n, final A arg) {
        //n.getLabel().ifPresent(l -> l.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CastExpr n, final A arg) {
        n.getExpression().accept(this, arg);
        //n.getType().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CatchClause n, final A arg) {
        n.getBody().accept(this, arg);
        n.getParameter().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CharLiteralExpr n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ClassExpr n, final A arg) {
        //n.getType().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final A arg) {
        //n.getExtendedTypes().forEach(p -> p.accept(this, arg));
        //n.getImplementedTypes().forEach(p -> p.accept(this, arg));
        //n.getTypeParameters().forEach(p -> p.accept(this, arg));
        n.getMembers().forEach(p -> p.accept(this, arg));
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ClassOrInterfaceType n, final A arg) {
        //n.getName().accept(this, arg);
        n.getScope().ifPresent(l -> l.accept(this, arg));
        //n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CompilationUnit n, final A arg) {
        //n.getImports().forEach(p -> p.accept(this, arg));
        //n.getModule().ifPresent(l -> l.accept(this, arg));
        //n.getPackageDeclaration().ifPresent(l -> l.accept(this, arg));
        n.getTypes().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ConditionalExpr n, final A arg) {
        n.getCondition().accept(this, arg);
        n.getElseExpr().accept(this, arg);
        n.getThenExpr().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ConstructorDeclaration n, final A arg) {
        n.getBody().accept(this, arg);
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getParameters().forEach(p -> p.accept(this, arg));
        //n.getReceiverParameter().ifPresent(l -> l.accept(this, arg));
        //n.getThrownExceptions().forEach(p -> p.accept(this, arg));
        //n.getTypeParameters().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ContinueStmt n, final A arg) {
        //n.getLabel().ifPresent(l -> l.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final DoStmt n, final A arg) {
        n.getBody().accept(this, arg);
        n.getCondition().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final DoubleLiteralExpr n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final EmptyStmt n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final EnclosedExpr n, final A arg) {
        n.getInner().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final EnumConstantDeclaration n, final A arg) {
        n.getArguments().forEach(p -> p.accept(this, arg));
        n.getClassBody().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final EnumDeclaration n, final A arg) {
        n.getEntries().forEach(p -> p.accept(this, arg));
        //n.getImplementedTypes().forEach(p -> p.accept(this, arg));
        n.getMembers().forEach(p -> p.accept(this, arg));
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ExplicitConstructorInvocationStmt n, final A arg) {
        n.getArguments().forEach(p -> p.accept(this, arg));
        n.getExpression().ifPresent(l -> l.accept(this, arg));
        //n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ExpressionStmt n, final A arg) {
        n.getExpression().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final FieldAccessExpr n, final A arg) {
        //n.getName().accept(this, arg);
        n.getScope().accept(this, arg);
        //n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final FieldDeclaration n, final A arg) {
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getVariables().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ForEachStmt n, final A arg) {
        n.getBody().accept(this, arg);
        n.getIterable().accept(this, arg);
        n.getVariable().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ForStmt n, final A arg) {
        n.getBody().accept(this, arg);
        n.getCompare().ifPresent(l -> l.accept(this, arg));
        n.getInitialization().forEach(p -> p.accept(this, arg));
        n.getUpdate().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final IfStmt n, final A arg) {
        n.getCondition().accept(this, arg);
        n.getElseStmt().ifPresent(l -> l.accept(this, arg));
        n.getThenStmt().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final InitializerDeclaration n, final A arg) {
        n.getBody().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final InstanceOfExpr n, final A arg) {
        n.getExpression().accept(this, arg);
        n.getPattern().ifPresent(l -> l.accept(this, arg));
        //n.getType().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final IntegerLiteralExpr n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final JavadocComment n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LabeledStmt n, final A arg) {
        n.getLabel().accept(this, arg);
        n.getStatement().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LineComment n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LongLiteralExpr n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MarkerAnnotationExpr n, final A arg) {
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MemberValuePair n, final A arg) {
        //n.getName().accept(this, arg);
        n.getValue().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MethodCallExpr n, final A arg) {
        n.getArguments().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        n.getScope().ifPresent(l -> l.accept(this, arg));
        //n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MethodDeclaration n, final A arg) {
        n.getBody().ifPresent(l -> l.accept(this, arg));
        //n.getType().accept(this, arg);
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getParameters().forEach(p -> p.accept(this, arg));
        //n.getReceiverParameter().ifPresent(l -> l.accept(this, arg));
        //n.getThrownExceptions().forEach(p -> p.accept(this, arg));
        //n.getTypeParameters().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final NameExpr n, final A arg) {
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final NormalAnnotationExpr n, final A arg) {
        n.getPairs().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final NullLiteralExpr n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ObjectCreationExpr n, final A arg) {
        n.getAnonymousClassBody().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        n.getArguments().forEach(p -> p.accept(this, arg));
        n.getScope().ifPresent(l -> l.accept(this, arg));
        //n.getType().accept(this, arg);
        //n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final PackageDeclaration n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final Parameter n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getType().accept(this, arg);
        n.getVarArgsAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final PrimitiveType n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final Name n, final A arg) {
        n.getQualifier().ifPresent(l -> l.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SimpleName n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayType n, final A arg) {
        n.getComponentType().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ArrayCreationLevel n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getDimension().ifPresent(l -> l.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final IntersectionType n, final A arg) {
        n.getElements().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final UnionType n, final A arg) {
        n.getElements().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ReturnStmt n, final A arg) {
        n.getExpression().ifPresent(l -> l.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SingleMemberAnnotationExpr n, final A arg) {
        n.getMemberValue().accept(this, arg);
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final StringLiteralExpr n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SuperExpr n, final A arg) {
        n.getTypeName().ifPresent(l -> l.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SwitchEntry n, final A arg) {
        n.getLabels().forEach(p -> p.accept(this, arg));
        n.getStatements().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SwitchStmt n, final A arg) {
        n.getEntries().forEach(p -> p.accept(this, arg));
        n.getSelector().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SynchronizedStmt n, final A arg) {
        n.getBody().accept(this, arg);
        n.getExpression().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ThisExpr n, final A arg) {
        n.getTypeName().ifPresent(l -> l.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ThrowStmt n, final A arg) {
        n.getExpression().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final TryStmt n, final A arg) {
        n.getCatchClauses().forEach(p -> p.accept(this, arg));
        n.getFinallyBlock().ifPresent(l -> l.accept(this, arg));
        n.getResources().forEach(p -> p.accept(this, arg));
        n.getTryBlock().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LocalClassDeclarationStmt n, final A arg) {
        n.getClassDeclaration().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LocalRecordDeclarationStmt n, final A arg) {
        n.getRecordDeclaration().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final TypeParameter n, final A arg) {
        //n.getName().accept(this, arg);
        n.getTypeBound().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final UnaryExpr n, final A arg) {
        n.getExpression().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final UnknownType n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final VariableDeclarationExpr n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        n.getVariables().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final VariableDeclarator n, final A arg) {
        n.getInitializer().ifPresent(l -> l.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getType().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final VoidType n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final WhileStmt n, final A arg) {
        n.getBody().accept(this, arg);
        n.getCondition().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final WildcardType n, final A arg) {
        n.getExtendedType().ifPresent(l -> l.accept(this, arg));
        n.getSuperType().ifPresent(l -> l.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final LambdaExpr n, final A arg) {
        n.getBody().accept(this, arg);
        //n.getParameters().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final MethodReferenceExpr n, final A arg) {
        n.getScope().accept(this, arg);
        //n.getTypeArguments().ifPresent(l -> l.forEach(v -> v.accept(this, arg)));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final TypeExpr n, final A arg) {
        //n.getType().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(NodeList n, A arg) {
        for (Object node : n) {
            ((Node) node).accept(this, arg);
        }
    }

    @Override
    public void visit(final ImportDeclaration n, final A arg) {
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    public void visit(final ModuleDeclaration n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        n.getDirectives().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    public void visit(final ModuleRequiresDirective n, final A arg) {
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ModuleExportsDirective n, final A arg) {
        n.getModuleNames().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ModuleProvidesDirective n, final A arg) {
        //n.getName().accept(this, arg);
        n.getWith().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ModuleUsesDirective n, final A arg) {
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ModuleOpensDirective n, final A arg) {
        n.getModuleNames().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final UnparsableStmt n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final ReceiverParameter n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getType().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final VarType n, final A arg) {
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final Modifier n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final SwitchExpr n, final A arg) {
        n.getEntries().forEach(p -> p.accept(this, arg));
        n.getSelector().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final TextBlockLiteralExpr n, final A arg) {
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final YieldStmt n, final A arg) {
        n.getExpression().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final PatternExpr n, final A arg) {
        //n.getName().accept(this, arg);
        //n.getType().accept(this, arg);
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final RecordDeclaration n, final A arg) {
        //n.getImplementedTypes().forEach(p -> p.accept(this, arg));
        //n.getParameters().forEach(p -> p.accept(this, arg));
        //n.getReceiverParameter().ifPresent(l -> l.accept(this, arg));
        //n.getTypeParameters().forEach(p -> p.accept(this, arg));
        n.getMembers().forEach(p -> p.accept(this, arg));
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(final CompactConstructorDeclaration n, final A arg) {
        n.getBody().accept(this, arg);
        //n.getModifiers().forEach(p -> p.accept(this, arg));
        //n.getName().accept(this, arg);
        //n.getThrownExceptions().forEach(p -> p.accept(this, arg));
        //n.getTypeParameters().forEach(p -> p.accept(this, arg));
        n.getAnnotations().forEach(p -> p.accept(this, arg));
        //n.getComment().ifPresent(l -> l.accept(this, arg));
    }
}
