package edu.tum.sse.dirts.analysis.def.checksum;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * Enables to calculate the checksum of nontype nodes
 */
public class TypeChecksumVisitor implements ChecksumVisitor<TypeDeclaration<?>> {

    /**
     * The code in this class has been taken from NoCommentsHashCodeVisitor and slightly modified
     * https://github.com/javaparser/javaparser/blob/7a8796f7334feb9563014c7da71868cfd4c49798/javaparser-core-testing/src/test/java/com/github/javaparser/ast/visitor/NoCommentHashCodeVisitorTest.java
     */

    private final IdentityHashMap<Node, Integer> cache;

    public TypeChecksumVisitor() {
        cache = new IdentityHashMap<>();
    }

    public int hashCode(final Node node) {
        return cache.computeIfAbsent(node, n -> n.accept(this, null));
    }

    //##################################################################################################################
    // Visitor Pattern

    // Some elements are commented out, that are handled separately as types
    public Integer visit(final CompilationUnit n, final Void arg) {
        // This purposefully excludes types
        return n.getImports().accept(this, arg) * 31 + (n.getModule().isPresent() ? n.getModule().get().accept(this, arg) : 0) * 31 + (n.getPackageDeclaration().isPresent() ? n.getPackageDeclaration().get().accept(this, arg) : 0) * 31 /* +  n.getTypes().accept(this, arg)*/;
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    public Integer visit(final AnnotationDeclaration n, final Void arg) {
        return n.getMembers().accept(this, arg) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final AnnotationMemberDeclaration n, final Void arg) {
        return (n.getDefaultValue().isPresent() ? n.getDefaultValue().get().accept(this, arg) : 0) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getType().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final ArrayAccessExpr n, final Void arg) {
        return n.getIndex().accept(this, arg) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final ArrayCreationExpr n, final Void arg) {
        return n.getElementType().accept(this, arg) * 31 + (n.getInitializer().isPresent() ? n.getInitializer().get().accept(this, arg) : 0) * 31 + n.getLevels().accept(this, arg);
    }

    public Integer visit(final ArrayCreationLevel n, final Void arg) {
        return n.getAnnotations().accept(this, arg) * 31 + (n.getDimension().isPresent() ? n.getDimension().get().accept(this, arg) : 0);
    }

    public Integer visit(final ArrayInitializerExpr n, final Void arg) {
        return n.getValues().accept(this, arg);
    }

    public Integer visit(final ArrayType n, final Void arg) {
        return n.getComponentType().accept(this, arg) * 31 + visit(n.getOrigin()) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final AssertStmt n, final Void arg) {
        return n.getCheck().accept(this, arg) * 31 + (n.getMessage().isPresent() ? n.getMessage().get().accept(this, arg) : 0);
    }

    public Integer visit(final AssignExpr n, final Void arg) {
        return visit(n.getOperator()) * 31 + n.getTarget().accept(this, arg) * 31 + n.getValue().accept(this, arg);
    }

    public Integer visit(final BinaryExpr n, final Void arg) {
        return n.getLeft().accept(this, arg) * 31 + visit(n.getOperator()) * 31 + n.getRight().accept(this, arg);
    }

    public Integer visit(final BlockComment n, final Void arg) {
        return 0;
    }

    public Integer visit(final BlockStmt n, final Void arg) {
        return n.getStatements().accept(this, arg);
    }

    public Integer visit(final BooleanLiteralExpr n, final Void arg) {
        return n.isValue() ? 1 : 0;
    }

    public Integer visit(final BreakStmt n, final Void arg) {
        return n.getLabel().isPresent() ? n.getLabel().get().accept(this, arg) : 0;
    }

    public Integer visit(final CastExpr n, final Void arg) {
        return n.getExpression().accept(this, arg) * 31 + n.getType().accept(this, arg);
    }

    public Integer visit(final CatchClause n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + n.getParameter().accept(this, arg);
    }

    public Integer visit(final CharLiteralExpr n, final Void arg) {
        return n.getValue().hashCode();
    }

    public Integer visit(final ClassExpr n, final Void arg) {
        return n.getType().accept(this, arg);
    }

    public Integer visit(final ClassOrInterfaceDeclaration n, final Void arg) {
        return n.getExtendedTypes().accept(this, arg) * 31 + n.getImplementedTypes().accept(this, arg) * 31 + (n.isInterface() ? 1 : 0) * 31 + n.getTypeParameters().accept(this, arg) * 31 + n.getMembers().accept(this, arg) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final ClassOrInterfaceType n, final Void arg) {
        return n.getName().accept(this, arg) * 31 + (n.getScope().isPresent() ? n.getScope().get().accept(this, arg) : 0) * 31 + (n.getTypeArguments().isPresent() ? (Integer)((NodeList)n.getTypeArguments().get()).accept(this, arg) : 0) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final ConditionalExpr n, final Void arg) {
        return n.getCondition().accept(this, arg) * 31 + n.getElseExpr().accept(this, arg) * 31 + n.getThenExpr().accept(this, arg);
    }

    public Integer visit(final ConstructorDeclaration n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getParameters().accept(this, arg) * 31 + (n.getReceiverParameter().isPresent() ? n.getReceiverParameter().get().accept(this, arg) : 0) * 31 + n.getThrownExceptions().accept(this, arg) * 31 + n.getTypeParameters().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final ContinueStmt n, final Void arg) {
        return n.getLabel().isPresent() ? n.getLabel().get().accept(this, arg) : 0;
    }

    public Integer visit(final DoStmt n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + n.getCondition().accept(this, arg);
    }

    public Integer visit(final DoubleLiteralExpr n, final Void arg) {
        return n.getValue().hashCode();
    }

    public Integer visit(final EmptyStmt n, final Void arg) {
        return 0;
    }

    public Integer visit(final EnclosedExpr n, final Void arg) {
        return n.getInner().accept(this, arg);
    }

    public Integer visit(final EnumConstantDeclaration n, final Void arg) {
        return n.getArguments().accept(this, arg) * 31 + n.getClassBody().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final EnumDeclaration n, final Void arg) {
        return n.getEntries().accept(this, arg) * 31 + n.getImplementedTypes().accept(this, arg) * 31 + n.getMembers().accept(this, arg) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final ExplicitConstructorInvocationStmt n, final Void arg) {
        return n.getArguments().accept(this, arg) * 31 + (n.getExpression().isPresent() ? n.getExpression().get().accept(this, arg) : 0) * 31 + (n.isThis() ? 1 : 0) * 31 + (n.getTypeArguments().isPresent() ? (Integer)((NodeList)n.getTypeArguments().get()).accept(this, arg) : 0);
    }

    public Integer visit(final ExpressionStmt n, final Void arg) {
        return n.getExpression().accept(this, arg);
    }

    public Integer visit(final FieldAccessExpr n, final Void arg) {
        return n.getName().accept(this, arg) * 31 + n.getScope().accept(this, arg) * 31 + (n.getTypeArguments().isPresent() ? (Integer)((NodeList)n.getTypeArguments().get()).accept(this, arg) : 0);
    }

    public Integer visit(final FieldDeclaration n, final Void arg) {
        return n.getModifiers().accept(this, arg) * 31 + n.getVariables().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final ForStmt n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + (n.getCompare().isPresent() ? n.getCompare().get().accept(this, arg) : 0) * 31 + n.getInitialization().accept(this, arg) * 31 + n.getUpdate().accept(this, arg);
    }

    public Integer visit(final ForEachStmt n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + n.getIterable().accept(this, arg) * 31 + n.getVariable().accept(this, arg);
    }

    public Integer visit(final IfStmt n, final Void arg) {
        return n.getCondition().accept(this, arg) * 31 + (n.getElseStmt().isPresent() ? n.getElseStmt().get().accept(this, arg) : 0) * 31 + n.getThenStmt().accept(this, arg);
    }

    public Integer visit(final ImportDeclaration n, final Void arg) {
        return (n.isAsterisk() ? 1 : 0) * 31 + (n.isStatic() ? 1 : 0) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final InitializerDeclaration n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + (n.isStatic() ? 1 : 0) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final InstanceOfExpr n, final Void arg) {
        return n.getExpression().accept(this, arg) * 31 + (n.getPattern().isPresent() ? n.getPattern().get().accept(this, arg) : 0) * 31 + n.getType().accept(this, arg);
    }

    public Integer visit(final IntegerLiteralExpr n, final Void arg) {
        return n.getValue().hashCode();
    }

    public Integer visit(final IntersectionType n, final Void arg) {
        return n.getElements().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final JavadocComment n, final Void arg) {
        return 0;
    }

    public Integer visit(final LabeledStmt n, final Void arg) {
        return n.getLabel().accept(this, arg) * 31 + n.getStatement().accept(this, arg);
    }

    public Integer visit(final LambdaExpr n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + (n.isEnclosingParameters() ? 1 : 0) * 31 + n.getParameters().accept(this, arg);
    }

    public Integer visit(final LineComment n, final Void arg) {
        return 0;
    }

    public Integer visit(final LocalClassDeclarationStmt n, final Void arg) {
        return n.getClassDeclaration().accept(this, arg);
    }

    public Integer visit(final LocalRecordDeclarationStmt n, final Void arg) {
        return n.getRecordDeclaration().accept(this, arg);
    }

    public Integer visit(final LongLiteralExpr n, final Void arg) {
        return n.getValue().hashCode();
    }

    public Integer visit(final MarkerAnnotationExpr n, final Void arg) {
        return n.getName().accept(this, arg);
    }

    public Integer visit(final MemberValuePair n, final Void arg) {
        return n.getName().accept(this, arg) * 31 + n.getValue().accept(this, arg);
    }

    public Integer visit(final MethodCallExpr n, final Void arg) {
        return n.getArguments().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + (n.getScope().isPresent() ? n.getScope().get().accept(this, arg) : 0) * 31 + (n.getTypeArguments().isPresent() ? (Integer)((NodeList)n.getTypeArguments().get()).accept(this, arg) : 0);
    }

    public Integer visit(final MethodDeclaration n, final Void arg) {
        return (n.getBody().isPresent() ? n.getBody().get().accept(this, arg) : 0) * 31 + n.getType().accept(this, arg) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getParameters().accept(this, arg) * 31 + (n.getReceiverParameter().isPresent() ? n.getReceiverParameter().get().accept(this, arg) : 0) * 31 + n.getThrownExceptions().accept(this, arg) * 31 + n.getTypeParameters().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final MethodReferenceExpr n, final Void arg) {
        return n.getIdentifier().hashCode() * 31 + n.getScope().accept(this, arg) * 31 + (n.getTypeArguments().isPresent() ? (Integer)((NodeList)n.getTypeArguments().get()).accept(this, arg) : 0);
    }

    public Integer visit(final NameExpr n, final Void arg) {
        return n.getName().accept(this, arg);
    }

    public Integer visit(final Name n, final Void arg) {
        return n.getIdentifier().hashCode() * 31 + (n.getQualifier().isPresent() ? n.getQualifier().get().accept(this, arg) : 0);
    }

    public Integer visit(NodeList n, Void arg) {
        int result = 0;

        Object node;
        for(Iterator var4 = n.iterator(); var4.hasNext(); result += 31 * ((Visitable)node).accept(this, arg)) {
            node = var4.next();
        }

        return result;
    }

    public Integer visit(final NormalAnnotationExpr n, final Void arg) {
        return n.getPairs().accept(this, arg) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final NullLiteralExpr n, final Void arg) {
        return 0;
    }

    public Integer visit(final ObjectCreationExpr n, final Void arg) {
        return (n.getAnonymousClassBody().isPresent() ? (Integer)((NodeList)n.getAnonymousClassBody().get()).accept(this, arg) : 0) * 31 + n.getArguments().accept(this, arg) * 31 + (n.getScope().isPresent() ? n.getScope().get().accept(this, arg) : 0) * 31 + n.getType().accept(this, arg) * 31 + (n.getTypeArguments().isPresent() ? (Integer)((NodeList)n.getTypeArguments().get()).accept(this, arg) : 0);
    }

    public Integer visit(final PackageDeclaration n, final Void arg) {
        return n.getAnnotations().accept(this, arg) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final Parameter n, final Void arg) {
        return n.getAnnotations().accept(this, arg) * 31 + (n.isVarArgs() ? 1 : 0) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getType().accept(this, arg) * 31 + n.getVarArgsAnnotations().accept(this, arg);
    }

    public Integer visit(final PrimitiveType n, final Void arg) {
        return visit(n.getType()) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final ReturnStmt n, final Void arg) {
        return n.getExpression().isPresent() ? n.getExpression().get().accept(this, arg) : 0;
    }

    public Integer visit(final SimpleName n, final Void arg) {
        return n.getIdentifier().hashCode();
    }

    public Integer visit(final SingleMemberAnnotationExpr n, final Void arg) {
        return n.getMemberValue().accept(this, arg) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final StringLiteralExpr n, final Void arg) {
        return n.getValue().hashCode();
    }

    public Integer visit(final SuperExpr n, final Void arg) {
        return n.getTypeName().isPresent() ? n.getTypeName().get().accept(this, arg) : 0;
    }

    public Integer visit(final SwitchEntry n, final Void arg) {
        return n.getLabels().accept(this, arg) * 31 + n.getStatements().accept(this, arg) * 31 + visit(n.getType());
    }

    public Integer visit(final SwitchStmt n, final Void arg) {
        return n.getEntries().accept(this, arg) * 31 + n.getSelector().accept(this, arg);
    }

    public Integer visit(final SynchronizedStmt n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + n.getExpression().accept(this, arg);
    }

    public Integer visit(final ThisExpr n, final Void arg) {
        return n.getTypeName().isPresent() ? n.getTypeName().get().accept(this, arg) : 0;
    }

    public Integer visit(final ThrowStmt n, final Void arg) {
        return n.getExpression().accept(this, arg);
    }

    public Integer visit(final TryStmt n, final Void arg) {
        return n.getCatchClauses().accept(this, arg) * 31 + (n.getFinallyBlock().isPresent() ? n.getFinallyBlock().get().accept(this, arg) : 0) * 31 + n.getResources().accept(this, arg) * 31 + n.getTryBlock().accept(this, arg);
    }

    public Integer visit(final TypeExpr n, final Void arg) {
        return n.getType().accept(this, arg);
    }

    public Integer visit(final TypeParameter n, final Void arg) {
        return n.getName().accept(this, arg) * 31 + n.getTypeBound().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final UnaryExpr n, final Void arg) {
        return n.getExpression().accept(this, arg) * 31 + visit(n.getOperator());
    }

    public Integer visit(final UnionType n, final Void arg) {
        return n.getElements().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final UnknownType n, final Void arg) {
        return n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final VariableDeclarationExpr n, final Void arg) {
        return n.getAnnotations().accept(this, arg) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getVariables().accept(this, arg);
    }

    public Integer visit(final VariableDeclarator n, final Void arg) {
        return (n.getInitializer().isPresent() ? n.getInitializer().get().accept(this, arg) : 0) * 31 + n.getName().accept(this, arg) * 31 + n.getType().accept(this, arg);
    }

    public Integer visit(final VoidType n, final Void arg) {
        return n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final WhileStmt n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + n.getCondition().accept(this, arg);
    }

    public Integer visit(final WildcardType n, final Void arg) {
        return (n.getExtendedType().isPresent() ? n.getExtendedType().get().accept(this, arg) : 0) * 31 + (n.getSuperType().isPresent() ? n.getSuperType().get().accept(this, arg) : 0) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final ModuleDeclaration n, final Void arg) {
        return n.getAnnotations().accept(this, arg) * 31 + n.getDirectives().accept(this, arg) * 31 + (n.isOpen() ? 1 : 0) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final ModuleRequiresDirective n, final Void arg) {
        return n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final ModuleExportsDirective n, final Void arg) {
        return n.getModuleNames().accept(this, arg) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final ModuleProvidesDirective n, final Void arg) {
        return n.getName().accept(this, arg) * 31 + n.getWith().accept(this, arg);
    }

    public Integer visit(final ModuleUsesDirective n, final Void arg) {
        return n.getName().accept(this, arg);
    }

    public Integer visit(final ModuleOpensDirective n, final Void arg) {
        return n.getModuleNames().accept(this, arg) * 31 + n.getName().accept(this, arg);
    }

    public Integer visit(final UnparsableStmt n, final Void arg) {
        return 0;
    }

    public Integer visit(final ReceiverParameter n, final Void arg) {
        return n.getAnnotations().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getType().accept(this, arg);
    }

    public Integer visit(final VarType n, final Void arg) {
        return n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final Modifier n, final Void arg) {
        return visit(n.getKeyword());
    }

    public Integer visit(final SwitchExpr n, final Void arg) {
        return n.getEntries().accept(this, arg) * 31 + n.getSelector().accept(this, arg);
    }

    public Integer visit(final YieldStmt n, final Void arg) {
        return n.getExpression().accept(this, arg);
    }

    public Integer visit(final TextBlockLiteralExpr n, final Void arg) {
        return n.getValue().hashCode();
    }

    public Integer visit(final PatternExpr n, final Void arg) {
        return n.getName().accept(this, arg) * 31 + n.getType().accept(this, arg);
    }

    public Integer visit(final RecordDeclaration n, final Void arg) {
        return n.getImplementedTypes().accept(this, arg) * 31 + n.getParameters().accept(this, arg) * 31 + (n.getReceiverParameter().isPresent() ? n.getReceiverParameter().get().accept(this, arg) : 0) * 31 + n.getTypeParameters().accept(this, arg) * 31 + n.getMembers().accept(this, arg) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final CompactConstructorDeclaration n, final Void arg) {
        return n.getBody().accept(this, arg) * 31 + n.getModifiers().accept(this, arg) * 31 + n.getName().accept(this, arg) * 31 + n.getThrownExceptions().accept(this, arg) * 31 + n.getTypeParameters().accept(this, arg) * 31 + n.getAnnotations().accept(this, arg);
    }

    public Integer visit(final Enum<?> arg) {
        return arg.name().hashCode();
    }

}