package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.tum.sse.dirts.core.BlackboardState.TYPE_SOLVER_SET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class TypeSolverInitializerTest extends KnowledgeSourceTest {

    private final BlackboardState resultState = TYPE_SOLVER_SET;

    private final Path mavenDependenciesPath = rootPath
            .toAbsolutePath()
            .resolve(subPath)
            .resolve(Path.of(".dirts"))
            .resolve(Path.of("libraries"));

    @Test
    public void testUpdateBlackboard() throws IOException {
        /* given */
        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
        TypeSolverInitializer<TypeDeclaration<?>> sut = new TypeSolverInitializer<>(blackboardMock);

        Files.createFile(mavenDependenciesPath);

        /* when */
        when(blackboardMock.getRootPath()).thenReturn(rootPath);
        when(blackboardMock.getSubPath()).thenReturn(subPath);

        BlackboardState blackboardState = sut.updateBlackboard();

        /* then */
        assertThat(blackboardState).isEqualTo(resultState);

        // typeSolver should be set
        verify(blackboardMock).setTypeSolver(any());
    }

}