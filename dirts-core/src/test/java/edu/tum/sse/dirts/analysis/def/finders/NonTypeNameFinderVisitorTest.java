package edu.tum.sse.dirts.analysis.def.finders;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import edu.tum.sse.dirts.util.naming_scheme.Names;
import edu.tum.sse.dirts.util.tuples.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class NonTypeNameFinderVisitorTest {

    private static final String PREFIX = "edu.tum.sse.dirts.test_code.finder.";

    private static final NonTypeNameFinderVisitor sut = new NonTypeNameFinderVisitor();
    private static Set<String> nonTypeNames;
    private static Set<String> mappedNonTypeNames;

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

        nonTypeNames = typeDeclarationMap.keySet();
        mappedNonTypeNames = typeDeclarationMap.values().stream()
                .filter(Objects::nonNull)
                .map(Names::lookup)
                .map(Pair::getFirst)
                .collect(Collectors.toSet());
    }

    @Test
    public void testAttributesInClass() {
        checkIfFound(PREFIX + "SimpleClass.privatePrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleClass.protectedPrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleClass.packagePrivatePrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleClass.publicPrimitiveAttribute");

        checkIfFound(PREFIX + "SimpleClass.privateReferenceAttribute");
        checkIfFound(PREFIX + "SimpleClass.protectedReferenceAttribute");
        checkIfFound(PREFIX + "SimpleClass.packagePrivateReferenceAttribute");
        checkIfFound(PREFIX + "SimpleClass.publicReferenceAttribute");

        checkIfFound(PREFIX + "SimpleClass.privateStaticPrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleClass.protectedStaticPrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleClass.packagePrivateStaticPrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleClass.publicStaticPrimitiveAttribute");

        checkIfFound(PREFIX + "SimpleClass.privateStaticReferenceAttribute");
        checkIfFound(PREFIX + "SimpleClass.protectedStaticReferenceAttribute");
        checkIfFound(PREFIX + "SimpleClass.packagePrivateStaticReferenceAttribute");
        checkIfFound(PREFIX + "SimpleClass.publicStaticReferenceAttribute");
    }


    @Test
    public void testAttributesInInterface() {
        checkIfFound(PREFIX + "SimpleInterface.packagePrivatePrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleInterface.publicPrimitiveAttribute");

        checkIfFound(PREFIX + "SimpleInterface.packagePrivateReferenceAttribute");
        checkIfFound(PREFIX + "SimpleInterface.publicReferenceAttribute");
    }

    @Test
    public void testAttributesInEnum() {
        checkIfFound(PREFIX + "SimpleEnum.privatePrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleEnum.protectedPrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleEnum.packagePrivatePrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleEnum.publicPrimitiveAttribute");

        checkIfFound(PREFIX + "SimpleEnum.privateReferenceAttribute");
        checkIfFound(PREFIX + "SimpleEnum.protectedReferenceAttribute");
        checkIfFound(PREFIX + "SimpleEnum.packagePrivateReferenceAttribute");
        checkIfFound(PREFIX + "SimpleEnum.publicReferenceAttribute");
    }

    @Test
    public void testAttributesInAnnotation() {
        checkIfFound(PREFIX + "SimpleAnnotation.packagePrivatePrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleAnnotation.publicPrimitiveAttribute");

        checkIfFound(PREFIX + "SimpleAnnotation.packagePrivateReferenceAttribute");
        checkIfFound(PREFIX + "SimpleAnnotation.publicReferenceAttribute");

        checkIfFound(PREFIX + "SimpleAnnotation.packagePrivateStaticPrimitiveAttribute");
        checkIfFound(PREFIX + "SimpleAnnotation.publicStaticPrimitiveAttribute");

        checkIfFound(PREFIX + "SimpleAnnotation.packagePrivateStaticReferenceAttribute");
        checkIfFound(PREFIX + "SimpleAnnotation.publicStaticReferenceAttribute");
    }

    @Test
    public void testAttributesInnerClass() {
        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass.privatePrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass.protectedPrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass.packagePrivatePrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass.publicPrimitiveAttribute");

        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass.privateReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass.protectedReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass.packagePrivateReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerClass.publicReferenceAttribute");
    }

    @Test
    public void testAttributesInnerInterface() {
        checkIfFound(PREFIX + "OuterClass.PrivateInnerInterface.packagePrivatePrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerInterface.publicPrimitiveAttribute");

        checkIfFound(PREFIX + "OuterClass.PrivateInnerInterface.packagePrivateReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerInterface.publicReferenceAttribute");
    }

    @Test
    public void testAttributesInnerEnum() {
        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum.privatePrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum.protectedPrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum.packagePrivatePrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum.publicPrimitiveAttribute");

        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum.privateReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum.protectedReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum.packagePrivateReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerEnum.publicReferenceAttribute");
    }

    @Test
    public void testAttributesInnerAnnotation() {
        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation.packagePrivatePrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation.publicPrimitiveAttribute");

        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation.packagePrivateReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation.publicReferenceAttribute");

        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation.packagePrivateStaticPrimitiveAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation.publicStaticPrimitiveAttribute");

        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation.packagePrivateStaticReferenceAttribute");
        checkIfFound(PREFIX + "OuterClass.PrivateInnerAnnotation.publicStaticReferenceAttribute");
    }

    @Test
    public void testMultipleAttribute() {
        assertThat(nonTypeNames).anyMatch(n -> n.contains("FieldDeclaration_"));
    }

    @Test
    public void testCommentedOutAttribute() {
        assertThat(nonTypeNames).doesNotContain(PREFIX + "SimpleClass.commentedOutAttribute");
    }

    @Test
    public void testConstructor() {
        checkIfFound(PREFIX + "SimpleClass.SimpleClass()");
        checkIfFound(PREFIX + "SimpleEnum.SimpleEnum(int, java.lang.String)");
    }

    @Test
    public void testAbstractMethod() {
        checkIfFound(PREFIX + "AbstractClass.abstractMethod()");
    }

    @Test
    public void testMethod() {
        checkIfFound(PREFIX + "SimpleClass.privateVoidMethod(int)");
        checkIfFound(PREFIX + "SimpleClass.protectedVoidMethod()");
        checkIfFound(PREFIX + "SimpleClass.packagePrivateVoidMethod(java.lang.String)");
        checkIfFound(PREFIX + "SimpleClass.publicVoidMethod(java.lang.String)");

        checkIfFound(PREFIX + "SimpleClass.privatePrimitiveMethod(int)");
        checkIfFound(PREFIX + "SimpleClass.protectedPrimitiveMethod()");
        checkIfFound(PREFIX + "SimpleClass.packagePrivatePrimitiveMethod(java.lang.String)");
        checkIfFound(PREFIX + "SimpleClass.publicPrimitiveMethod(java.lang.String)");

        checkIfFound(PREFIX + "SimpleClass.privateReferenceMethod(int)");
        checkIfFound(PREFIX + "SimpleClass.protectedReferenceMethod()");
        checkIfFound(PREFIX + "SimpleClass.packagePrivateReferenceMethod(java.lang.String)");
        checkIfFound(PREFIX + "SimpleClass.publicReferenceMethod(java.lang.String)");
    }

    // TODO: Tests for annotation members
    // TODO: Tests for Initializer

    private void checkIfFound(String expectedName) {
        assertThat(nonTypeNames).contains(expectedName);
        assertThat(mappedNonTypeNames).contains(expectedName);
    }
}
