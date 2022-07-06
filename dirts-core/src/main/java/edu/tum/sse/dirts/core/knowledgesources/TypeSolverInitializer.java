/*
 * Copyright 2022. The ttrace authors.
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

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.tum.sse.dirts.core.BlackboardState.IMPORTED;
import static edu.tum.sse.dirts.core.BlackboardState.TYPE_SOLVER_SET;
import static java.util.logging.Level.WARNING;

/**
 * Adds a ReflectionTypeSolver and a JarTypeSolver for jars of maven dependencies
 */
public class TypeSolverInitializer extends KnowledgeSource {

    //##################################################################################################################
    // Constructors

    public TypeSolverInitializer(Blackboard blackboard) {
        super(blackboard);
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        Path mavenDependenciesPath = blackboard.getRootPath()
                .toAbsolutePath()
                .resolve(blackboard.getSubPath())
                .resolve(Path.of(".dirts"))
                .resolve(Path.of("libraries"));

        if (Files.exists(mavenDependenciesPath)) {
            try {
                String[] mavenDependencies = Files
                        .readString(mavenDependenciesPath)
                        .split(":");

                for (String mavenDependency : mavenDependencies) {
                    try {
                        Path dependencyPath = Path.of(mavenDependency);
                        typeSolver.add(new JarTypeSolver(dependencyPath));
                    } catch (IOException e) {
                        Log.errLog(WARNING, "Failed to add resolver for jar:" + mavenDependency);
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
            Log.errLog(WARNING,"Failed to read maven dependencies - " +
                    "we will not be able to resolve code from libraries");
            Log.errLog(WARNING,"File does not exist: " + mavenDependenciesPath);
        }

        blackboard.setTypeSolver(typeSolver);

        return TYPE_SOLVER_SET;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == IMPORTED;
    }
}
