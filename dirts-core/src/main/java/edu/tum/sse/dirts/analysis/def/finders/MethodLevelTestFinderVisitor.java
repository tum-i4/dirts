package edu.tum.sse.dirts.analysis.def.finders;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.surefire.api.testset.TestFilter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;

/**
 * Collects all method level nodes that may represent tests and their names
 * (might also contain constructors of outer classes if an inner class has test methods)
 */
public class MethodLevelTestFinderVisitor extends FinderVisitor<Collection<String>, BodyDeclaration<?>> {

    //##################################################################################################################
    // Attributes

    private final TestFilter<String, String> testFilter;

    //##################################################################################################################
    // Constructors

    public MethodLevelTestFinderVisitor(TestFilter<String, String> testFilter) {
        this.testFilter = testFilter;
    }

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Collection<String> arg) {
        super.visit(n, arg);

        // ## Tests in inner classes
        // If this class has members that contain tests, add one of the constructor of this class as tests as well
        // since there is at least one constructor this ensures that this class is treated as test class
        // Edges are set by JUnitMethodLevelDependencyCollectorVisitor
        if (recursiveMemberTest(n, testFilter)) {
            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                List<ResolvedConstructorDeclaration> constructors = resolvedReferenceTypeDeclaration.getConstructors();
                ResolvedConstructorDeclaration resolvedConstructorDeclaration = constructors.get(0);
                arg.add(lookup(resolvedConstructorDeclaration));
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // ## Account for test methods from extended test classes
        // If this class extends a test class, it inherits its methods as test methods
        // Edges are set by JUnitMethodLevelDependencyCollectorVisitor
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
                        if (!resolvedClassDeclaration.isJavaLangObject()) {
                            for (ResolvedMethodDeclaration declaredMethod : resolvedClassDeclaration.getDeclaredMethods()) {
                                if (testFilter.shouldRun(lookup(resolvedReferenceTypeDeclaration), declaredMethod.getName()))
                                    // Consider this method as a test
                                    arg.add(lookup(resolvedReferenceTypeDeclaration) + "." + declaredMethod.getSignature());
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Collection<String> arg) {
        //super.visit(n, arg);
        if (FinderVisitor.testMethodDeclaration(n, testFilter)) {
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
