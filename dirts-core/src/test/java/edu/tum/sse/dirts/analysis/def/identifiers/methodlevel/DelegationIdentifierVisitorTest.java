package edu.tum.sse.dirts.analysis.def.identifiers.methodlevel;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.analysis.Util.getClassByName;
import static org.assertj.core.api.Assertions.assertThat;

public class DelegationIdentifierVisitorTest {

    private static final String PREFIX = "edu.tum.sse.dirts.test_code.delegation.";

    private static Set<String> resolvedDependencies;
    private static Set<String> resolvedDependenciesTypes;

    @BeforeAll
    static void setUp() throws IOException {
        SourceRoot sourceRoot = new SourceRoot(Path.of("src/test/resources/test_code/delegation/java"));

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
        typeSolver.add(new ReflectionTypeSolver());
        sourceRoot.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        Set<CompilationUnit> compilationUnits = sourceRoot.tryToParse()
                .stream().map(result -> result.getResult().orElseThrow()).collect(Collectors.toSet());

        // given
        ClassOrInterfaceDeclaration commander = getClassByName("Commander", compilationUnits);

        // when
        Collection<ResolvedMethodLikeDeclaration> resolvedMethods = DelegationIdentifierVisitor.identifyDependencies(commander);

        resolvedDependencies = resolvedMethods.stream()
                .map(ResolvedMethodLikeDeclaration::getQualifiedSignature)
                .collect(Collectors.toSet());

        resolvedDependenciesTypes = resolvedMethods.stream()
                .map(m -> m.declaringType().getQualifiedName())
                .collect(Collectors.toSet());

    }

    @Test
    public void testObjectDelegation() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "ObjectDelegate.acceptOrders(" + PREFIX + "Order" + ")");
    }

    @Test
    public void testStaticDelegation() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "StaticDelegate.acceptOrders(" + PREFIX + "Order" + ")");
    }


    @Test
    public void testMemberInitDelegation() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "MemberInitDelegate.MemberInitDelegate(" + PREFIX + "Order" + ")");
    }

    @Test
    public void testStaticInitDelegation() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "StaticInitDelegate.StaticInitDelegate(" + PREFIX + "Order" + ")");
    }

    @Test
    public void testStaticImportDelegation() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "StaticImportDelegate.acceptOrders(" + PREFIX + "Order" + ")");
    }

    @Test
    public void testUnusedDelegation() {
        // then
        assertThat(resolvedDependenciesTypes).doesNotContain(PREFIX + "UselessDelegate");
    }

    @Test
    public void testUnresolved() {
        // then
        assertThat(resolvedDependenciesTypes).doesNotContain(PREFIX + "General");
    }

    @Test
    public void testSuperConstructor() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "AbstractCommander.AbstractCommander(java.lang.String)");
    }

    @Test
    public void testThisConstructor() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "Commander.Commander(java.lang.String)");
    }

    @Test
    public void testSuperDelegate() {
        // then
        assertThat(resolvedDependencies).contains(PREFIX + "AbstractCommander.abstractOrders()");
    }
}
