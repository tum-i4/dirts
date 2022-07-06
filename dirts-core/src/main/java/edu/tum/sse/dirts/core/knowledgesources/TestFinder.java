package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.CompilationUnit;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static edu.tum.sse.dirts.core.BlackboardState.*;

/**
 * Finds tests that are present
 */
public class TestFinder extends KnowledgeSource {

    private final FinderVisitor<Collection<String>> finderVisitor;

    public TestFinder(Blackboard blackboard, FinderVisitor<Collection<String>> finderVisitor) {
        super(blackboard);
        this.finderVisitor = finderVisitor;
    }

    @Override
    public BlackboardState updateBlackboard() {
        if (blackboard.getTestFilter() != null) {
            Collection<CompilationUnit> compilationUnits = blackboard.getCompilationUnits();
            List<String> tests = new ArrayList<>();
            compilationUnits.forEach(cu -> cu.accept(finderVisitor, tests));

            blackboard.setTests(tests);
        }
        return TESTS_FOUND;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == PARSED;
    }
}
