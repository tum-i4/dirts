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
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;

/**
 * Parses all compilation units
 */
public class Parser<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    //##################################################################################################################
    // Constructors

    public Parser(Blackboard<T> blackboard) {
        super(blackboard);
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {
        CombinedTypeSolver typeSolver = blackboard.getTypeSolver();

        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();

        List<SourceRoot> sourceRootsSubProject = getSourceRoots(rootPath.resolve(subPath));

        List<CompilationUnit> compilationUnits = importCompilationUnits(sourceRootsSubProject, typeSolver);

        blackboard.setCompilationUnits(compilationUnits);

        return BlackboardState.PARSED;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == BlackboardState.TYPE_SOLVER_SET;
    }

    private List<CompilationUnit> importCompilationUnits(List<SourceRoot> sourceRoots, CombinedTypeSolver typeSolver) {
        List<CompilationUnit> compilationUnits = new ArrayList<>();
        for (SourceRoot sourceRoot : sourceRoots) {
            Path root = sourceRoot.getRoot();
            if (root != null)
                typeSolver.add(new JavaParserTypeSolver(root));
        }
        sourceRoots.forEach(sourceRoot -> {
            try {
                sourceRoot.getParserConfiguration()
//                        .setAttributeComments(false)
//                        .setDoNotAssignCommentsPrecedingEmptyLines(true)
//                        .setIgnoreAnnotationsWhenAttributingComments(true)
                        .setSymbolResolver(new JavaSymbolSolver(typeSolver));
                sourceRoot.tryToParse()
                        .forEach(result -> result.ifSuccessful(compilationUnits::add));
            } catch (IOException e) {
                Log.log(WARNING, "Failed to parse SourceRoot " + sourceRoot);
                e.printStackTrace();
            }
        });
        return compilationUnits;
    }

    private static List<SourceRoot> getSourceRoots(Path path) {
        return new ParserCollectionStrategy()
                .collect(path)
                .getSourceRoots();
    }
}
