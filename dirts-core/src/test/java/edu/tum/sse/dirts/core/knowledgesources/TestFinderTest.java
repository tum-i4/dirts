package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;

import static edu.tum.sse.dirts.core.BlackboardState.TESTS_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TestFinderTest extends KnowledgeSourceTest {

    private final BlackboardState resultState = TESTS_FOUND;

    @Test
    public void testUpdateBlackboard() {
        /* given */
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        FinderVisitor<Collection<String>, TypeDeclaration<?>> testFinderMock = mock(FinderVisitor.class);
        TestFinder<TypeDeclaration<?>> sut = new TestFinder<>(blackboardMock, testFinderMock);
        CompilationUnit compilationUnitMock = mock(CompilationUnit.class);

        ArgumentCaptor<Collection<String>> testsCaptor = ArgumentCaptor.forClass(Collection.class);

        /* when */
        when(blackboardMock.getRootPath()).thenReturn(rootPath);
        when(blackboardMock.getSubPath()).thenReturn(subPath);

        when(blackboardMock.getTestFilter()).thenReturn((klass, method) -> true);
        when(blackboardMock.getCompilationUnits()).thenReturn(List.of(compilationUnitMock));

        BlackboardState blackboardState = sut.updateBlackboard();

        /* then */
        assertThat(blackboardState).isEqualTo(resultState);

        // testFinder is invoked
        verify(compilationUnitMock).accept(eq(testFinderMock), testsCaptor.capture());

        // testsAre set
        verify(blackboardMock).setTests(same(testsCaptor.getValue()));
    }
}