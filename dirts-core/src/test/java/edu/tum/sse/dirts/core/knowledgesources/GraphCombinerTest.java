package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.ModificationGraph;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class GraphCombinerTest extends KnowledgeSourceTest {

    private final BlackboardState resultState = BlackboardState.READY_TO_CALCULATE_AFFECTED_TESTS;

    @Test
    public void testUpdateBlackboard() {
        /* given */
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        GraphCombiner<TypeDeclaration<?>> sut = new GraphCombiner<>(blackboardMock);

        DependencyGraph dependencyGraphOldMock = new DependencyGraph();
        DependencyGraph dependencyGraphNewMock = new DependencyGraph();
        dependencyGraphNewMock.addNode("NewClass");
        dependencyGraphOldMock.addNode("OldClass");
        Map<String, String> nameMapper = new HashMap<>();

        ArgumentCaptor<ModificationGraph> modificationGraphCaptor = ArgumentCaptor.forClass(ModificationGraph.class);

        /* when */
        when(blackboardMock.getRootPath()).thenReturn(rootPath);
        when(blackboardMock.getSubPath()).thenReturn(subPath);
        when(blackboardMock.getNameMapperNodes()).thenReturn(nameMapper);

        when(blackboardMock.getDependencyGraphOldRevision()).thenReturn(dependencyGraphOldMock);
        when(blackboardMock.getDependencyGraphNewRevision()).thenReturn(dependencyGraphNewMock);

        BlackboardState blackboardState = sut.updateBlackboard();

        /* then */
        assertThat(blackboardState).isEqualTo(resultState);

        // a ModificationGraph should be created
        verify(blackboardMock).setCombinedGraph(modificationGraphCaptor.capture());
        assertThat(modificationGraphCaptor.getValue().getNodes()).containsKeys("NewClass", "OldClass");
    }

}