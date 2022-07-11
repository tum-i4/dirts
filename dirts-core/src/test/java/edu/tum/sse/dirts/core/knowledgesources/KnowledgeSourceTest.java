package edu.tum.sse.dirts.core.knowledgesources;

import java.io.IOException;
import java.nio.file.Path;

public abstract class KnowledgeSourceTest {

    protected final Path rootPath = Path.of("src/test/resources/test_code/knowledgesources");
    protected final Path subPath = Path.of("");

    protected final String suffix = "test";

    public abstract void testUpdateBlackboard() throws Exception;

    // Template
//    @Test
//    public void testUpdateBlackboard() {
//        /* given */
//        Blackboard<TypeDeclaration<?>> blackboardMock = mock(Blackboard.class);
//        T<TypeDeclaration<?>> sut = new T<>(blackboardMock);
//
//        /* when */
//        when(blackboardMock.getRootPath()).thenReturn(rootPath);
//        when(blackboardMock.getSubPath()).thenReturn(subPath);
//
//        BlackboardState blackboardState = sut.updateBlackboard();
//
//        /* then */
//        assertThat(blackboardState).isEqualTo(resultState);
//    }
}
