package edu.tum.sse.dirts.analysis.def.finders;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.util.JavaParserUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Collects all NonType-nodes that may represent tests and their names
 * (might also contain constructors of outer classes if an inner class has test methods)
 */
public class NonTypeTestFinderVisitor extends FinderVisitor<Collection<String>> {

    //##################################################################################################################
    // Constants

    public static final Predicate<MethodDeclaration> testMethodDeclaration =
            n -> n.isAnnotationPresent("Test")
                    || n.isAnnotationPresent("ParameterizedTest")
                    || n.isAnnotationPresent("RepeatedTest");
    ;

    public static final Predicate<ClassOrInterfaceDeclaration> testClassDeclaration =
            n -> n.getExtendedTypes().stream().anyMatch(e -> e.getNameAsString().equals("TestCase"));

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Collection<String> arg) {
        super.visit(n, arg);

        // ## Tests in inner classes
        // If this class has members that contain tests, add one of the constructor of this class as tests as well
        // since there is at least one constructor this ensures that this class is treated as test class
        if (recursiveMemberTest(n)) {
            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                List<ResolvedConstructorDeclaration> constructors = resolvedReferenceTypeDeclaration.getConstructors();
                ResolvedConstructorDeclaration resolvedConstructorDeclaration = constructors.get(0);
                arg.add(lookup(resolvedConstructorDeclaration));
            } catch (RuntimeException e) {
                if (Control.DEBUG)
                    System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // ## Tests in extended classes
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
                        for (ResolvedMethodDeclaration declaredMethod : resolvedClassDeclaration.getDeclaredMethods()) {
                            Optional<MethodDeclaration> maybeMethodDeclaration = declaredMethod.toAst();
                            if (maybeMethodDeclaration.isPresent()) {
                                MethodDeclaration methodDeclaration = maybeMethodDeclaration.get();
                                if (testMethodDeclaration.test(methodDeclaration)) {
                                    // If the class extends a test class, we have to add a node for inherited test methods
                                    arg.add(lookup(resolvedReferenceTypeDeclaration) + "." + declaredMethod.getSignature());
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

        // ## Tests from extending TestCase
        if (testClassDeclaration.test(n)) {
            for (MethodDeclaration method : n.getMethods()) {
                if (!method.getNameAsString().equals("setUp") && !method.getNameAsString().equals("tearDown")) {
                    arg.add(lookup(method).getFirst());
                }
            }
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
        if (testMethodDeclaration.test(n)) {
            arg.add(lookup(n).getFirst());
        }
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
