package edu.tum.sse.dirts.analysis.def.identifiers.nontype;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.Util.*;
import static org.assertj.core.api.Assertions.assertThat;

public class InheritanceIdentifierVisitorTest {

    private static final String PREFIX = "edu.tum.sse.dirts.test_code.inheritance.";

    private static Set<CompilationUnit> compilationUnits;

    @BeforeAll
    static void setUp() throws IOException {
        SourceRoot sourceRoot = new SourceRoot(Path.of("src/test/resources/test_code/inheritance/java"));

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
        typeSolver.add(new ReflectionTypeSolver());
        sourceRoot.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        compilationUnits = sourceRoot.tryToParse()
                .stream().map(result -> result.getResult().orElseThrow()).collect(Collectors.toSet());
    }

    @Test
    public void testDirectInheritAbstract() {
        // given
        ClassOrInterfaceDeclaration abstractClass = getClassByName("AbstractClass", compilationUnits);

        // when
        InheritanceIdentifierVisitor sut = new InheritanceIdentifierVisitor(abstractClass);

        for (MethodDeclaration method : abstractClass.getMethods()) {
            sut.visit(method, new HashSet<>());
        }

        Collection<String> inheritedMethods = methodsToSignatureString(sut.getInheritedMethods());

        // then
        assertThat(inheritedMethods).contains(PREFIX + "AbstractSuperClass.abstractMethod1()");
    }

    @Test
    public void testDirectInherit() {
        // given
        ClassOrInterfaceDeclaration directExtends = getClassByName("DirectExtends", compilationUnits);

        // when
        InheritanceIdentifierVisitor sut = new InheritanceIdentifierVisitor(directExtends);

        MethodDeclaration method1 = getMethod("method1", directExtends);
        MethodDeclaration abstractMethod2 = getMethod("abstractMethod2", directExtends);

        Collection<ResolvedMethodLikeDeclaration> method1OverridesMethods = new HashSet<>();
        Collection<ResolvedMethodLikeDeclaration> abstractMethod2OverridesMethods = new HashSet<>();

        sut.visit(method1, method1OverridesMethods);
        sut.visit(abstractMethod2, abstractMethod2OverridesMethods);

        Collection<String> method1Overrides = methodsToSignatureString(method1OverridesMethods);
        Collection<String> abstractMethod2Overrides = methodsToSignatureString(abstractMethod2OverridesMethods);

        // then
        assertThat(method1Overrides).containsExactly(PREFIX + "AbstractClass.method1()");
        assertThat(abstractMethod2Overrides).containsExactly(PREFIX + "AbstractClass.abstractMethod2()");
    }

    @Test
    public void testIndirectInherit() {
        // given
        ClassOrInterfaceDeclaration indirectExtends = getClassByName("IndirectExtends", compilationUnits);

        // when
        InheritanceIdentifierVisitor sut = new InheritanceIdentifierVisitor(indirectExtends);

        MethodDeclaration abstractMethod2 = getMethod("abstractMethod2", indirectExtends);

        Collection<ResolvedMethodLikeDeclaration> abstractMethod2OverridesMethods = new HashSet<>();

        sut.visit(abstractMethod2, abstractMethod2OverridesMethods);

        Collection<String> abstractMethod2Overrides = methodsToSignatureString(abstractMethod2OverridesMethods);

        // then
        assertThat(abstractMethod2Overrides).contains(
                PREFIX + "AbstractClass.abstractMethod2()",
                PREFIX + "DirectExtends.abstractMethod2()");
    }

    @Test
    public void testDirectDefault() {
        // given
        ClassOrInterfaceDeclaration directImplements = getClassByName("DirectImplements", compilationUnits);

        // when
        InheritanceIdentifierVisitor sut = new InheritanceIdentifierVisitor(directImplements);

        for (MethodDeclaration method : directImplements.getMethods()) {
            sut.visit(method, new HashSet<>());
        }

        Collection<String> inheritedMethods = methodsToSignatureString(sut.getInheritedMethods());

        // then
        assertThat(inheritedMethods).contains(PREFIX + "Interface.defaultMethod()");
    }

    @Test
    public void testOverriddenDefault() {
        // given
        ClassOrInterfaceDeclaration indirectImplements = getClassByName("IndirectImplements", compilationUnits);

        // when
        InheritanceIdentifierVisitor sut = new InheritanceIdentifierVisitor(indirectImplements);

        for (MethodDeclaration method : indirectImplements.getMethods()) {
            sut.visit(method, new HashSet<>());
        }

        Collection<String> inheritedMethods = methodsToSignatureString(sut.getInheritedMethods());

        // then
        assertThat(inheritedMethods).doesNotContain(PREFIX + "Interface.defaultMethod()");
    }


    @Test
    public void testDirectImplement() {
        // given
        ClassOrInterfaceDeclaration directExtends = getClassByName("DirectImplements", compilationUnits);

        // when
        InheritanceIdentifierVisitor sut = new InheritanceIdentifierVisitor(directExtends);

        MethodDeclaration interfaceMethod1 = getMethod("interfaceMethod1", directExtends);
        MethodDeclaration interfaceMethod2 = getMethod("interfaceMethod2", directExtends);

        Collection<ResolvedMethodLikeDeclaration> interfaceMethod1OverridesMethods = new HashSet<>();
        Collection<ResolvedMethodLikeDeclaration> interfaceMethod2OverridesMethods = new HashSet<>();

        sut.visit(interfaceMethod1, interfaceMethod1OverridesMethods);
        sut.visit(interfaceMethod2, interfaceMethod2OverridesMethods);

        Collection<String> interfaceMethod1Overrides = methodsToSignatureString(interfaceMethod1OverridesMethods);
        Collection<String> interfaceMethod2Overrides = methodsToSignatureString(interfaceMethod2OverridesMethods);

        // then
        assertThat(interfaceMethod1Overrides).containsExactly(PREFIX + "Interface.interfaceMethod1(" + PREFIX + "AbstractClass" + ")");
        assertThat(interfaceMethod2Overrides).containsExactly(PREFIX + "Interface.interfaceMethod2(" + PREFIX + "AbstractSuperClass" + ")");
    }

    @Test
    public void testIndirectImplement() {
        // given
        ClassOrInterfaceDeclaration indirectImplements = getClassByName("IndirectImplements", compilationUnits);

        // when
        InheritanceIdentifierVisitor sut = new InheritanceIdentifierVisitor(indirectImplements);

        MethodDeclaration interfaceMethod2 = getMethod("interfaceMethod2", indirectImplements);

        Collection<ResolvedMethodLikeDeclaration> interfaceMethod2OverridesMethods = new HashSet<>();

        sut.visit(interfaceMethod2, interfaceMethod2OverridesMethods);

        Collection<String> abstractMethod2Overrides = methodsToSignatureString(interfaceMethod2OverridesMethods);

        // then
        assertThat(abstractMethod2Overrides).contains(
                PREFIX + "Interface.interfaceMethod2(" + PREFIX + "AbstractSuperClass" + ")",
                PREFIX + "DirectImplements.interfaceMethod2(" + PREFIX + "AbstractSuperClass" + ")");
    }

    @Test
    public void testDirectInheritDirectImplements() {
        // given
        ClassOrInterfaceDeclaration directExtendsDirectImplements = getClassByName("DirectExtendsDirectImplements", compilationUnits);

        // when
        InheritanceIdentifierVisitor sut = new InheritanceIdentifierVisitor(directExtendsDirectImplements);

        MethodDeclaration abstractMethod1 = getMethod("abstractMethod1", directExtendsDirectImplements);
        MethodDeclaration abstractMethod2 = getMethod("abstractMethod2", directExtendsDirectImplements);
        MethodDeclaration interfaceMethod1 = getMethod("interfaceMethod1", directExtendsDirectImplements);
        MethodDeclaration interfaceMethod2 = getMethod("interfaceMethod2", directExtendsDirectImplements);

        Collection<ResolvedMethodLikeDeclaration> abstractMethod1OverridesMethods = new HashSet<>();
        Collection<ResolvedMethodLikeDeclaration> abstractMethod2OverridesMethods = new HashSet<>();
        Collection<ResolvedMethodLikeDeclaration> interfaceMethod1OverridesMethods = new HashSet<>();
        Collection<ResolvedMethodLikeDeclaration> interfaceMethod2OverridesMethods = new HashSet<>();

        sut.visit(abstractMethod1, abstractMethod1OverridesMethods);
        sut.visit(abstractMethod2, abstractMethod2OverridesMethods);
        sut.visit(interfaceMethod1, interfaceMethod1OverridesMethods);
        sut.visit(interfaceMethod2, interfaceMethod2OverridesMethods);

        Collection<String> abstractMethod1Overrides = methodsToSignatureString(abstractMethod1OverridesMethods);
        Collection<String> abstractMethod2Overrides = methodsToSignatureString(abstractMethod2OverridesMethods);
        Collection<String> interfaceMethod1Overrides = methodsToSignatureString(interfaceMethod1OverridesMethods);
        Collection<String> interfaceMethod2Overrides = methodsToSignatureString(interfaceMethod2OverridesMethods);

        Collection<String> inheritedMethods = methodsToSignatureString(sut.getInheritedMethods());

        // then
        assertThat(abstractMethod1Overrides).containsExactly(PREFIX + "AbstractSuperClass.abstractMethod1()");
        assertThat(abstractMethod2Overrides).containsExactly(PREFIX + "AbstractClass.abstractMethod2()");
        assertThat(interfaceMethod1Overrides).containsExactly(PREFIX + "Interface.interfaceMethod1(" + PREFIX + "AbstractClass" + ")");
        assertThat(interfaceMethod2Overrides).containsExactly(PREFIX + "Interface.interfaceMethod2(" + PREFIX + "AbstractSuperClass" + ")");

        assertThat(inheritedMethods).contains(PREFIX + "Interface.defaultMethod()");
        assertThat(inheritedMethods).contains(PREFIX + "AbstractClass.method1()");
    }

}
