package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.core.BlackboardState.PARSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ParserTest extends KnowledgeSourceTest {

    private final BlackboardState resultState = PARSED;

    @Test
    public void testUpdateBlackboard() {
        /* given */
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        Parser<TypeDeclaration<?>> sut = new Parser<>(blackboardMock);

        ArgumentCaptor<Collection<CompilationUnit>> compilationUnitsCaptor = ArgumentCaptor.forClass(Collection.class);

        /* when */
        when(blackboardMock.getTypeSolver()).thenReturn(new CombinedTypeSolver());
        when(blackboardMock.getRootPath()).thenReturn(rootPath);
        when(blackboardMock.getSubPath()).thenReturn(subPath);

        BlackboardState blackboardState = sut.updateBlackboard();

        /* then */
        assertThat(blackboardState).isEqualTo(resultState);

        // compilationUnits should be set
        verify(blackboardMock).setCompilationUnits(compilationUnitsCaptor.capture());
        Set<String> compilationUnitsNames = compilationUnitsCaptor.getValue().stream()
                .map(CompilationUnit::getPrimaryTypeName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        assertThat(compilationUnitsNames)
                .contains("AClass", "ATest");
        assertThat(compilationUnitsNames)
                .doesNotContain("Foo");
    }

    @Test
    public void testImportCompilationUnits() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // given
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        Parser<TypeDeclaration<?>> sut = new Parser<>(blackboardMock);

        SourceRoot sourceRootMock = mock(SourceRoot.class);
        ParserConfiguration parserConfigurationMock = mock(ParserConfiguration.class);
        CompilationUnit compilationUnitMock = mock(CompilationUnit.class);
        ParseResult<CompilationUnit> result = new ParseResult<>(compilationUnitMock, List.of(), null);

        //when
        when(sourceRootMock.getParserConfiguration()).thenReturn(parserConfigurationMock);
        when(sourceRootMock.tryToParse()).thenReturn(List.of(result));
        when(sourceRootMock.getRoot()).thenReturn(null);

        Method importCompilationUnits = Parser.class.getDeclaredMethod("importCompilationUnits", List.class, CombinedTypeSolver.class);
        importCompilationUnits.setAccessible(true);
        List<CompilationUnit> compilationUnits = (List<CompilationUnit>) importCompilationUnits
                .invoke(sut, List.of(sourceRootMock), new CombinedTypeSolver());


        // then
        assertThat(compilationUnits).containsExactly(compilationUnitMock);
        verify(parserConfigurationMock).setSymbolResolver(any());
    }
}