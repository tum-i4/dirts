package edu.tum.sse.dirts.analysis.def.identifiers.type;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
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
import static org.assertj.core.api.Assertions.assertThat;

public class NewIdentifierVisitorTest {

    private static final String PREFIX = "edu.tum.sse.dirts.test_code.new_.";

    private static Set<String> resolvedDependencies;

    @BeforeAll
    static void setUp() throws IOException {
        SourceRoot sourceRoot = new SourceRoot(Path.of("src/test/resources/test_code/new/java"));

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
        typeSolver.add(new ReflectionTypeSolver());
        sourceRoot.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        Set<CompilationUnit> compilationUnits = sourceRoot.tryToParse()
                .stream().map(result -> result.getResult().orElseThrow()).collect(Collectors.toSet());

        // given
        ClassOrInterfaceDeclaration builder = getClassByName("Builder", compilationUnits);

        // when
        Collection<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations =
                NewIdentifierVisitor.identifyDependencies(builder);

        resolvedDependencies = resolvedReferenceTypeDeclarations.stream()
                .map(ResolvedReferenceTypeDeclaration::getQualifiedName)
                .collect(Collectors.toSet());
    }

    @Test
    public void testHouse() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "House");
    }

    @Test
    public void testTower() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "Tower");
    }

    @Test
    public void testConstructable() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "Constructable");
    }

    @Test
    public void testGarden() {
        // then
        assertThat(resolvedDependencies).doesNotContain(PREFIX + "Garden");
    }

}
