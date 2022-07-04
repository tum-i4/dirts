package edu.tum.sse.dirts.analysis.def.finders;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.naming_scheme.Names;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Collects all Type-nodes that may represent tests and their names
 */
public class TypeTestFinderVisitor extends FinderVisitor<Collection<String>> {

    //##################################################################################################################
    // Constants

    public static final Predicate<BodyDeclaration<?>> testMethodDeclaration =
            n -> n.isAnnotationPresent("Test")
                    || n.isAnnotationPresent("ParameterizedTest")
                    || n.isAnnotationPresent("RepeatedTest");

    // Also accounts for tests from extending TestCase
    public static final Predicate<ClassOrInterfaceDeclaration> testClassDeclaration =
            n -> n.getMethods().stream().anyMatch(testMethodDeclaration)
                    || n.getExtendedTypes().stream().anyMatch(e -> e.getNameAsString().equals("TestCase"));

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Collection<String> arg) {
        super.visit(n, arg);

        // ## Tests from inner classes
        // if this class has members that contain tests, add this class as test class as well
        if (recursiveMemberTest(n)) {
            arg.add(Names.lookup(n).getFirst());
        }

        // ## Tests from extended classes
        try {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
            List<ResolvedReferenceType> allAncestors = resolvedReferenceTypeDeclaration
                    .getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList);
            for (ResolvedReferenceType ancestor : allAncestors) {
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = ancestor.getTypeDeclaration();
                if (maybeTypeDeclaration.isPresent()) {
                    ResolvedReferenceTypeDeclaration resolvedAncestorTypeDeclaration = maybeTypeDeclaration.get();
                    if (resolvedAncestorTypeDeclaration.isClass()) {
                        ResolvedClassDeclaration resolvedClassDeclaration = resolvedAncestorTypeDeclaration.asClass();
                        Optional<Node> maybeClassDeclaration = resolvedClassDeclaration.toAst();
                        if (maybeClassDeclaration.isPresent()) {
                            Node node = maybeClassDeclaration.get();
                            if (node instanceof ClassOrInterfaceDeclaration) {
                                ClassOrInterfaceDeclaration ancestorDeclaration = (ClassOrInterfaceDeclaration) node;
                                if (testClassDeclaration.test(ancestorDeclaration)) {
                                    // If class extends a test class, it can be executed as test as well
                                    arg.add(Names.lookup(n).getFirst());
                                }
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            if (Control.DEBUG)
                System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }

        if (testClassDeclaration.test(n)) {
            arg.add(Names.lookup(n).getFirst());
        }
    }

    @Override
    public void visit(EnumDeclaration n, Collection<String> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationDeclaration n,Collection<String> arg) {
        super.visit(n, arg);
    }

    //******************************************************************************************************************
    // Stop here, only go as deep as necessary

    @Override
    public void visit(ConstructorDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(EnumConstantDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(FieldDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(InitializerDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
    }
}
