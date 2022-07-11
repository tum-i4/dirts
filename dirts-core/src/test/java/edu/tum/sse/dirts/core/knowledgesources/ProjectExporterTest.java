package edu.tum.sse.dirts.core.knowledgesources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.def.checksum.ChecksumVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static edu.tum.sse.dirts.core.BlackboardState.DONE;
import static edu.tum.sse.dirts.core.BlackboardState.IMPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class ProjectExporterTest extends KnowledgeSourceTest {

    private final BlackboardState resultState = DONE;

    @Test
    public void testUpdateBlackboard() throws JsonProcessingException {
        /* given */
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        ChecksumVisitor<TypeDeclaration<?>> checksumVisitorMock = mock(ChecksumVisitor.class);
        ProjectExporter<TypeDeclaration<?>> sut = new ProjectExporter<>(blackboardMock, suffix, checksumVisitorMock, true);

        DependencyStrategy<TypeDeclaration<?>> dependencyStrategyMock = mock(DependencyStrategy.class);

        DependencyGraph dependencyGraphMock = mock(DependencyGraph.class);

        Map<String, Integer> checksums = Map.of("edu.tum.sse.dirts.test_code.AClass", 42);

        /* when */
        when(blackboardMock.getRootPath()).thenReturn(rootPath);
        when(blackboardMock.getSubPath()).thenReturn(subPath);

        when(blackboardMock.getDependencyStrategies()).thenReturn(List.of(dependencyStrategyMock));

        when(blackboardMock.getDependencyGraphNewRevision()).thenReturn(dependencyGraphMock);
        when(blackboardMock.getChecksumsNodes()).thenReturn(checksums);
        when(dependencyGraphMock.serializeGraph()).thenReturn("{\n" +
                "  \"edu.tum.sse.dirts.test_code.AClass\" : [ ],\n" +
                "  \"edu.tum.sse.dirts.test_code.ATest\" : [ ]\n" +
                "}\n" +
                "\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "{\n" +
                "}");

        BlackboardState blackboardState = sut.updateBlackboard();

        /* then */
        assertThat(blackboardState).isEqualTo(resultState);

        // graph of new revision should be queried
        verify(blackboardMock).getDependencyGraphNewRevision();
        verify(dependencyGraphMock).removeNodesWithoutEdges();

        // dependencyStrategies should be considered
        verify(dependencyStrategyMock).doExport(any(), same(blackboardMock), eq(suffix));
    }
}