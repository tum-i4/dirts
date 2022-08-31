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

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static edu.tum.sse.dirts.core.BlackboardState.IMPORTED;
import static edu.tum.sse.dirts.core.BlackboardState.TYPE_SOLVER_SET;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;

/**
 * Adds a ReflectionTypeSolver and a JarTypeSolver for jars of maven dependencies
 */
public class TypeSolverInitializer<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    //##################################################################################################################
    // Constructors

    public TypeSolverInitializer(Blackboard<T> blackboard) {
        super(blackboard);
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {
        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver()); // may cause build failure

        Path mavenDependenciesPath = rootPath
                .toAbsolutePath()
                .resolve(subPath)
                .resolve(Path.of(".dirts"))
                .resolve(Path.of("libraries"));

        if (Files.exists(mavenDependenciesPath)) {
            try {
                String[] mavenDependencies = Files
                        .readString(mavenDependenciesPath)
                        .split(":");

                for (String mavenDependency : mavenDependencies) {
                    if (!(mavenDependency == null || mavenDependency.equals(""))) {

                        Path mavenDependencyPath = Path.of(mavenDependency);

                        if (Files.isDirectory(mavenDependencyPath)) {
                            Path parentDirectory = mavenDependencyPath.getParent().getParent();

                            List<SourceRoot> sourceRoots = new ParserCollectionStrategy()
                                    .collect(parentDirectory)
                                    .getSourceRoots();

                            for (SourceRoot sourceRoot : sourceRoots) {
                                Log.log(FINEST, "Adding resolver for sources in "
                                        + sourceRoot.getRoot().toAbsolutePath());
                                typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
                            }
                        } else if (Files.isRegularFile(mavenDependenciesPath)) {
                            try {
                                Log.log(FINEST, "Adding resolver for jar "
                                        + mavenDependencyPath.toAbsolutePath());
                                typeSolver.add(new JarTypeSolver(mavenDependencyPath));
                            } catch (IOException e) {
                                Log.errLog(WARNING, "Failed to add resolver for jar:" + mavenDependency);
                            }
                        }
                    }
                }
                try {
                    Files.delete(mavenDependenciesPath);
                } catch (IOException e) {
                    Log.errLog(WARNING, "Failed to delete file containing dependencies: " + e.getMessage());
                }
            } catch (IOException e) {
                Log.errLog(WARNING, "Failed to read maven dependencies - " +
                        "we will not be able to resolve code from libraries");
                e.printStackTrace();
            }
        } else {
            Log.errLog(WARNING, "Failed to read maven dependencies - " +
                    "we will not be able to resolve code from libraries");
            Log.errLog(WARNING, "File does not exist: " + mavenDependenciesPath);
        }

        blackboard.setTypeSolver(typeSolver);

        return TYPE_SOLVER_SET;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == IMPORTED;
    }
}
