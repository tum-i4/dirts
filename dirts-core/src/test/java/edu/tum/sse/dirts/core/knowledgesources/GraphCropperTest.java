package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.graph.DependencyGraph;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphCropperTest extends KnowledgeSourceTest {

    private final BlackboardState resultState = BlackboardState.NEW_GRAPH_SET;

    @Test
    public void testUpdateBlackboard() {
        /* given */
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        FinderVisitor<Map<String, Node>, TypeDeclaration<?>> finderVisitorMock = mock(FinderVisitor.class);
        DependencyGraph dependencyGraph = new DependencyGraph();

        GraphCropper<TypeDeclaration<?>> sut = new GraphCropper<>(blackboardMock, finderVisitorMock, Set.of(), t->true, false);

        /* when */
        when(blackboardMock.getRootPath()).thenReturn(rootPath);
        when(blackboardMock.getSubPath()).thenReturn(subPath);

        when(blackboardMock.getDependencyGraphNewRevision()).thenReturn(dependencyGraph);

        BlackboardState blackboardState = sut.updateBlackboard();

        /* then */
        assertThat(blackboardState).isEqualTo(resultState);
    }
}
