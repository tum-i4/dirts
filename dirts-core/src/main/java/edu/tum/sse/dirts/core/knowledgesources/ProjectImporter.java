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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Imports cached information from the previous run
 */
public class ProjectImporter<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Integer>> typeRefNodes = new TypeReference<>() {
    };
    private static final TypeReference<HashMap<String, String>> typeRefCUMapping = new TypeReference<>() {
    };

    private final String suffix;


    //##################################################################################################################
    // Constructors

    public ProjectImporter(Blackboard<T> blackboard, String suffix) {
        super(blackboard);

        this.suffix = suffix;
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {
        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();

        Path tmpPath = rootPath.resolve(subPath.resolve(".dirts"));

        try {
            // DependencyGraph
            String graph = Files.readString(tmpPath.resolve(Path.of("graph_" + suffix)));
            blackboard.setGraphOldRevision(DependencyGraph.deserializeGraph(graph));
            blackboard.setGraphNewRevision(DependencyGraph.deserializeGraph(graph));

            // import Checksums
            String checksumsNodes = Files.readString(tmpPath.resolve(Path.of("checksums_" + suffix)));
            blackboard.setChecksumsNodes(objectMapper.readValue(checksumsNodes, typeRefNodes));

            // import CompilationUnits mapping
            String compilationUnitsMapping = Files.readString(tmpPath.resolve(Path.of("cuMapping_" + suffix)));
            blackboard.setCompilationUnitMapping(objectMapper.readValue(compilationUnitsMapping, typeRefCUMapping));

        } catch (IOException ignored) {

            blackboard.setGraphNewRevision(new DependencyGraph());
            blackboard.setGraphOldRevision(null);

            blackboard.setChecksumsNodes(new HashMap<>());
            blackboard.setCompilationUnitMapping(new HashMap<>());
        }

        for (DependencyStrategy<T> dependencyStrategy : blackboard.getDependencyStrategies()) {
            dependencyStrategy.doImport(tmpPath, blackboard, suffix);
        }

        return BlackboardState.IMPORTED;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == BlackboardState.CLEAN;
    }
}
