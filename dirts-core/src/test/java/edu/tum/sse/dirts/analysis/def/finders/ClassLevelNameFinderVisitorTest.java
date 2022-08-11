package edu.tum.sse.dirts.analysis.def.finders;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassLevelNameFinderVisitorTest {

    private static final String PREFIX = "edu.tum.sse.dirts.test_code.finder.";

    private static final ClassLevelNameFinderVisitor sut = new ClassLevelNameFinderVisitor();
    private static Set<String> typeNames;
    private static Collection<Node> typeDeclarations;

    @BeforeAll
    static void setUp() throws IOException {
        SourceRoot sourceRoot = new SourceRoot(Path.of("src/test/resources/test_code/finder/java"));

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
        typeSolver.add(new ReflectionTypeSolver());
        sourceRoot.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        Set<CompilationUnit> compilationUnits = sourceRoot.tryToParse()
                .stream().map(result -> result.getResult().orElseThrow()).collect(Collectors.toSet());

        // given
        Map<String, Node> typeDeclarationMap = new HashMap<>();

        // when
        for (CompilationUnit compilationUnit : compilationUnits) {
            sut.visit(compilationUnit, typeDeclarationMap);
        }

        typeNames = typeDeclarationMap.keySet();
        typeDeclarations = typeDeclarationMap.values();
    }

    @Test
    public void testSimpleClass() {
        checkIfFound(PREFIX + "SimpleClass");
    }

    @Test
    public void testAbstractClass() {
        checkIfFound(PREFIX + "AbstractClass");
    }

    @Test
    public void testSimpleInterface() {
        checkIfFound(PREFIX + "SimpleInterface");
    }

    @Test
    public void testSimpleEnum() {
        checkIfFound(PREFIX + "SimpleEnum");
    }

    @Test
    public void testSimpleAnnotation() {
        checkIfFound(PREFIX + "SimpleAnnotation");
    }

    @Test
    public void testNestedClasses() {
        checkIfFound(PREFIX + "OuterClass");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass");
        checkIfFound(PREFIX + "OuterClass.PublicInnerClass");
        checkIfFound(PREFIX + "OuterClass.PrivateStaticInnerClass");
        checkIfFound(PREFIX + "OuterClass.PublicStaticInnerClass");

        checkIfFound(PREFIX + "OuterEnum");
        checkIfFound(PREFIX + "OuterEnum.PrivateInnerClass");
        checkIfFound(PREFIX + "OuterEnum.PublicInnerClass");
        checkIfFound(PREFIX + "OuterEnum.PrivateStaticInnerClass");
        checkIfFound(PREFIX + "OuterEnum.PublicStaticInnerClass");

        checkIfFound(PREFIX + "OuterInterface");
        checkIfFound(PREFIX + "OuterInterface.InnerClass");

        checkIfFound(PREFIX + "OuterAnnotation");
        checkIfFound(PREFIX + "OuterAnnotation.InnerClass");
    }

    @Test
    public void testNestedInterfaces() {
        checkIfFound(PREFIX + "OuterClass");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerInterface");
        checkIfFound(PREFIX + "OuterClass.PublicInnerInterface");

        checkIfFound(PREFIX + "OuterEnum");
        checkIfFound(PREFIX + "OuterEnum.PrivateInnerInterface");
        checkIfFound(PREFIX + "OuterEnum.PublicInnerInterface");

        checkIfFound(PREFIX + "OuterInterface");
        checkIfFound(PREFIX + "OuterInterface.InnerInterface");

        checkIfFound(PREFIX + "OuterAnnotation");
        checkIfFound(PREFIX + "OuterAnnotation.InnerAnnotation");

    }

    @Test
    public void testNestedEnum() {
        checkIfFound(PREFIX + "OuterClass");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum");
        checkIfFound(PREFIX + "OuterClass.PublicInnerEnum");

        checkIfFound(PREFIX + "OuterEnum");
        checkIfFound(PREFIX + "OuterEnum.PrivateInnerEnum");
        checkIfFound(PREFIX + "OuterEnum.PublicInnerEnum");

        checkIfFound(PREFIX + "OuterInterface");
        checkIfFound(PREFIX + "OuterInterface.InnerEnum");

        checkIfFound(PREFIX + "OuterAnnotation");
        checkIfFound(PREFIX + "OuterAnnotation.InnerClass");
    }

    @Test
    public void testNestedAnnotation() {
        checkIfFound(PREFIX + "OuterClass");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation");
        checkIfFound(PREFIX + "OuterClass.PublicInnerAnnotation");

        checkIfFound(PREFIX + "OuterEnum");
        checkIfFound(PREFIX + "OuterEnum.PrivateInnerAnnotation");
        checkIfFound(PREFIX + "OuterEnum.PublicInnerAnnotation");

        checkIfFound(PREFIX + "OuterInterface");
        checkIfFound(PREFIX + "OuterInterface.InnerAnnotation");

        checkIfFound(PREFIX + "OuterAnnotation");
        checkIfFound(PREFIX + "OuterAnnotation.InnerAnnotation");
    }

    @Test
    public void testCommentedOut() {
        assertThat(typeNames).doesNotContain(PREFIX + "CommentedOutClass");
    }

    private void checkIfFound(String expectedName) {
        assertThat(typeNames).contains(expectedName);
        assertThat(typeDeclarations).anyMatch(c -> {
            if (c instanceof TypeDeclaration<?>) {
                return ((TypeDeclaration<?>) c).getFullyQualifiedName().orElseThrow().equals(expectedName);
            } else {
                return false;
            }
        });
    }
}
