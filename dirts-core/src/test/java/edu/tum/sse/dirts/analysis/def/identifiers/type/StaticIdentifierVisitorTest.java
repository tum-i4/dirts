package edu.tum.sse.dirts.analysis.def.identifiers.type;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.Util.getClassByName;
import static org.assertj.core.api.Assertions.assertThat;

public class StaticIdentifierVisitorTest {

    private static final String PREFIX = "edu.tum.sse.dirts.test_code.static_.";

    private static Set<CompilationUnit> compilationUnits;

    @BeforeAll
    static void setUp() throws IOException {
        SourceRoot sourceRoot = new SourceRoot(Path.of("src/test/resources/test_code/static/java"));

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
        typeSolver.add(new ReflectionTypeSolver());
        sourceRoot.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        compilationUnits = sourceRoot.tryToParse()
                .stream().map(result -> result.getResult().orElseThrow()).collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Commander", "QualifiedCommander", "UnQualifiedCommander"})
    public void testCommander(String className) {
        // given
        ClassOrInterfaceDeclaration implementingClass = getClassByName(className, compilationUnits);

        // when
        Collection<ResolvedTypeDeclaration> resolvedReferenceTypeDeclarations =
                StaticIdentifierVisitor.identifyDependencies(implementingClass);

        Set<String> typeDeclarationsNames = resolvedReferenceTypeDeclarations.stream()
                .map(ResolvedTypeDeclaration::getQualifiedName)
                .collect(Collectors.toSet());

        // then
        assertThat(typeDeclarationsNames).contains(PREFIX + "StaticDelegate");
        assertThat(typeDeclarationsNames).doesNotContain(PREFIX + "NonStaticDelegate");
    }


}
