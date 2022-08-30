/*
 * Copyright 2022. The dirts authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static edu.tum.sse.dirts.core.BlackboardState.PARSED;
import static edu.tum.sse.dirts.core.BlackboardState.TESTS_FOUND;

/**
 * Finds tests that are present
 */
public class TestFinder<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    private final FinderVisitor<Collection<String>, T> finderVisitor;

    public TestFinder(Blackboard<T> blackboard, FinderVisitor<Collection<String>, T> finderVisitor) {
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
