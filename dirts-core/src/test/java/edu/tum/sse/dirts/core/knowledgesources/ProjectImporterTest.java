package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static edu.tum.sse.dirts.core.BlackboardState.IMPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ProjectImporterTest extends KnowledgeSourceTest {

    private final BlackboardState resultState = IMPORTED;

    @Test
    public void testUpdateBlackboard() {
        /* given */
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        ProjectImporter<TypeDeclaration<?>> sut = new ProjectImporter<>(blackboardMock);

        DependencyStrategy<TypeDeclaration<?>> dependencyStrategyMock = mock(DependencyStrategy.class);

        ArgumentCaptor<DependencyGraph> dependencyGraphOldCaptor = ArgumentCaptor.forClass(DependencyGraph.class);
        ArgumentCaptor<DependencyGraph> dependencyGraphNewCaptor = ArgumentCaptor.forClass(DependencyGraph.class);

        Map<String, Integer> expectedChecksums = Map.of("edu.tum.sse.dirts.test_code.AClass", 42);

        /* when */
        when(blackboardMock.getRootPath()).thenReturn(rootPath);
        when(blackboardMock.getSubPath()).thenReturn(subPath);

        when(blackboardMock.getDependencyStrategies()).thenReturn(List.of(dependencyStrategyMock));

        BlackboardState blackboardState = sut.updateBlackboard();

        /* then */
        assertThat(blackboardState).isEqualTo(resultState);

        // graphs should be set
        verify(blackboardMock).setGraphOldRevision(dependencyGraphOldCaptor.capture());
        verify(blackboardMock).setGraphNewRevision(dependencyGraphNewCaptor.capture());
        assertThat(dependencyGraphOldCaptor.getValue().getNodes()).contains("edu.tum.sse.dirts.test_code.AClass");
        assertThat(dependencyGraphNewCaptor.getValue().getNodes()).contains("edu.tum.sse.dirts.test_code.ATest");

        // checksums should be set
        verify(blackboardMock).setChecksumsNodes(expectedChecksums);

        // dependencyStrategies should be considered
        verify(dependencyStrategyMock).doImport(any(), same(blackboardMock), eq(suffix));
    }
}