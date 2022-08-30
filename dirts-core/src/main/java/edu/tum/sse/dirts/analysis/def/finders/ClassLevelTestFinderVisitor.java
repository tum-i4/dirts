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
package edu.tum.sse.dirts.analysis.def.finders;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.naming_scheme.Names;
import org.apache.maven.surefire.api.testset.TestFilter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;

/**
 * Collects all class level nodes that may represent tests and their names
 */
public class ClassLevelTestFinderVisitor extends FinderVisitor<Collection<String>, TypeDeclaration<?>> {

    //##################################################################################################################
    // Attributes

    private final TestFilter<String, String> testFilter;

    //##################################################################################################################
    // Constructors

    public ClassLevelTestFinderVisitor(TestFilter<String, String> testFilter) {
        this.testFilter = testFilter;
    }

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Collection<String> arg) {
        super.visit(n, arg);

        // ## ## Account for tests in inner classes
        // If this class has members that contain tests, add this class as test class as well
        // Edges are set by JUnitClassLevelDependencyCollectorVisitor
        if (recursiveMemberTest(n, testFilter)) {
            arg.add(Names.lookup(n).getFirst());
        }

        // ## Tests from extended classes
        // If this class extends a test class, it inherits its methods as test methods
        // Edges are set by JUnitClassLevelDependencyCollectorVisitor
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
                            if (testFilter.shouldRun(lookup(resolvedReferenceTypeDeclaration), declaredMethod.getName()))
                                // Consider this class as a test
                                arg.add(Names.lookup(n).getFirst());
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }

        if (FinderVisitor.testClassDeclaration(n, testFilter)) {
            arg.add(Names.lookup(n).getFirst());
        }
    }

    @Override
    public void visit(EnumDeclaration n, Collection<String> arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationDeclaration n, Collection<String> arg) {
        super.visit(n, arg);
    }

    //******************************************************************************************************************
    // Stop here, further proceeding is not necessary

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
