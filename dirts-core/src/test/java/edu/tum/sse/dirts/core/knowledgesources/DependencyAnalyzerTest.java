package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static edu.tum.sse.dirts.core.BlackboardState.DEPENDENCIES_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class DependencyAnalyzerTest extends KnowledgeSourceTest {

    private final BlackboardState resultState = DEPENDENCIES_UPDATED;

    @Test
    public void testUpdateBlackboard() {
        /* given */
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        DependencyCollector dependencyCollectorMock = mock(DependencyCollector.class);
        DependencyAnalyzer<TypeDeclaration<?>> sut = new DependencyAnalyzer<>(blackboardMock, dependencyCollectorMock);

        TypeDeclaration<?> typeDeclarationMock = mock(TypeDeclaration.class);
        DependencyGraph dependencyGraphMock = mock(DependencyGraph.class);

        DependencyStrategy<TypeDeclaration<?>> dependencyStrategyMock = mock(DependencyStrategy.class);

        /* when */
        when(blackboardMock.getRootPath()).thenReturn(rootPath);
        when(blackboardMock.getSubPath()).thenReturn(subPath);

        when(blackboardMock.getDependencyStrategies()).thenReturn(List.of(dependencyStrategyMock));

        when(blackboardMock.getImpactedTypes()).thenReturn(List.of(typeDeclarationMock));
        when(blackboardMock.getDependencyGraphNewRevision()).thenReturn(dependencyGraphMock);

        BlackboardState blackboardState = sut.updateBlackboard();

        /* then */
        assertThat(blackboardState).isEqualTo(resultState);

        // dependencyCollector should be invoked
        verify(dependencyCollectorMock).calculateDependencies(eq(List.of(typeDeclarationMock)), eq(dependencyGraphMock));

        // dependencyStrategies should be considered
        verify(dependencyStrategyMock).doDependencyAnalysis(same(blackboardMock));
    }
}