package edu.tum.sse.dirts.analysis.def.identifiers.nontype;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import edu.tum.sse.dirts.util.tuples.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.Util.getClassByName;
import static org.assertj.core.api.Assertions.assertThat;

public class FieldAccessIdentifierVisitorTest {

    private static final String PREFIX = "edu.tum.sse.dirts.test_code.field_access.";

    private static Set<CompilationUnit> compilationUnits;


    @BeforeAll
    static void setUp() throws IOException {
        SourceRoot sourceRoot = new SourceRoot(Path.of("src/test/resources/test_code/field_access/java"));

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
        typeSolver.add(new ReflectionTypeSolver());
        sourceRoot.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        compilationUnits = sourceRoot.tryToParse()
                .stream().map(result -> result.getResult().orElseThrow()).collect(Collectors.toSet());
    }

    @Test
    public void testMemberAssignment() {

        // given
        ClassOrInterfaceDeclaration controller = getClassByName("Controller", compilationUnits);

        // when
        Pair<Collection<ResolvedValueDeclaration>, Collection<ResolvedValueDeclaration>> fieldAccess =
                FieldAccessIdentifierVisitor.identifyDependencies(controller);

        Collection<ResolvedValueDeclaration> resolvedAccessedFieldsDecls = fieldAccess.getFirst();
        Collection<ResolvedValueDeclaration> resolvedAssignedFieldsDecls = fieldAccess.getSecond();

        Set<String> resolvedAssignedFields = resolvedAssignedFieldsDecls.stream()
                .map(ResolvedDeclaration::getName)
                .collect(Collectors.toSet());

        // then
        assertThat(resolvedAssignedFields).contains("objectMemberAccess");
    }

    @Test
    public void testMemberAccess() {

        // given
        ClassOrInterfaceDeclaration observer = getClassByName("Observer", compilationUnits);

        // when
        Pair<Collection<ResolvedValueDeclaration>, Collection<ResolvedValueDeclaration>> fieldAccess =
                FieldAccessIdentifierVisitor.identifyDependencies(observer);

        Collection<ResolvedValueDeclaration> resolvedAccessedFieldsDecls = fieldAccess.getFirst();
        Collection<ResolvedValueDeclaration> resolvedAssignedFieldsDecls = fieldAccess.getSecond();


        Set<String> resolvedAccessedFields = resolvedAccessedFieldsDecls.stream()
                .map(ResolvedDeclaration::getName)
                .collect(Collectors.toSet());

        // then
        assertThat(resolvedAccessedFields).contains("member", "staticMember", "staticMember2");
    }

}
