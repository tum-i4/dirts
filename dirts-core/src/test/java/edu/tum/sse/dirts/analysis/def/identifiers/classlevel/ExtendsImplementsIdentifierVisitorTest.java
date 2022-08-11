package edu.tum.sse.dirts.analysis.def.identifiers.classlevel;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
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
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.Util.getClassByName;
import static edu.tum.sse.dirts.analysis.Util.getInterfaceByName;
import static org.assertj.core.api.Assertions.assertThat;

public class ExtendsImplementsIdentifierVisitorTest {

    private static final String PREFIX = "edu.tum.sse.dirts.test_code.extends_implements.";

    private static Set<CompilationUnit> compilationUnits;

    @BeforeAll
    static void setUp() throws IOException {
        SourceRoot sourceRoot = new SourceRoot(Path.of("src/test/resources/test_code/extends_implements/java"));

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
        typeSolver.add(new ReflectionTypeSolver());
        sourceRoot.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        compilationUnits = sourceRoot.tryToParse()
                .stream().map(result -> result.getResult().orElseThrow()).collect(Collectors.toSet());
    }

    @Test
    public void testImplements() {
        // given
        ClassOrInterfaceDeclaration implementingClass = getClassByName("ImplementingClass", compilationUnits);

        // when
        Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations =
                ExtendsImplementsIdentifierVisitor.identifyDependencies(implementingClass);

        Set<String> typeDeclarationsNames = resolvedReferenceTypeDeclarations.stream()
                .map(ResolvedTypeDeclaration::getQualifiedName)
                .collect(Collectors.toSet());

        // then
        assertThat(typeDeclarationsNames).contains(PREFIX + "Interface");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "ImplementingClass");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "AbstractClass");
    }

    @Test
    public void testExtends() {
        // given
        ClassOrInterfaceDeclaration extendingClass = getClassByName("ExtendingClass", compilationUnits);

        // when
        Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations =
                ExtendsImplementsIdentifierVisitor.identifyDependencies(extendingClass);

        Set<String> typeDeclarationsNames = resolvedReferenceTypeDeclarations.stream()
                .map(ResolvedTypeDeclaration::getQualifiedName)
                .collect(Collectors.toSet());

        // then
        assertThat(typeDeclarationsNames).contains(PREFIX + "AbstractClass");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "ExtendingClass");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "Interface");
    }

    @Test
    public void testSecondLevelExtends() {
        // given
        ClassOrInterfaceDeclaration extendingClass = getClassByName("SecondLevelExtendingClass", compilationUnits);

        // when
        Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations =
                ExtendsImplementsIdentifierVisitor.identifyDependencies(extendingClass);

        Set<String> typeDeclarationsNames = resolvedReferenceTypeDeclarations.stream()
                .map(ResolvedTypeDeclaration::getQualifiedName)
                .collect(Collectors.toSet());

        // then
        assertThat(typeDeclarationsNames).contains(PREFIX + "ExtendingClass");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "SecondLevelExtendingClass");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "AbstractClass");
    }

    @Test
    public void testExtendingInterface() {
        // given
        ClassOrInterfaceDeclaration extendingClass = getInterfaceByName("ExtendingInterface", compilationUnits);

        // when
        Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations =
                ExtendsImplementsIdentifierVisitor.identifyDependencies(extendingClass);

        Set<String> typeDeclarationsNames = resolvedReferenceTypeDeclarations.stream()
                .map(ResolvedTypeDeclaration::getQualifiedName)
                .collect(Collectors.toSet());

        // then
        assertThat(typeDeclarationsNames).contains(PREFIX + "Interface");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "ExtendingInterface");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "ImplementingInterface");
    }

    @Test
    public void testExtendingImplementing() {
        // given
        ClassOrInterfaceDeclaration extendingClass = getClassByName("ExtendingImplementingClass", compilationUnits);

        // when
        Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations =
                ExtendsImplementsIdentifierVisitor.identifyDependencies(extendingClass);

        Set<String> typeDeclarationsNames = resolvedReferenceTypeDeclarations.stream()
                .map(ResolvedTypeDeclaration::getQualifiedName)
                .collect(Collectors.toSet());

        // then
        assertThat(typeDeclarationsNames).contains(PREFIX + "Interface");
        assertThat(typeDeclarationsNames).contains(PREFIX + "AbstractClass");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "ExtendingImplementingClass");
    }

}
