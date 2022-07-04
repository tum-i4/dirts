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
public class ProjectImporter extends KnowledgeSource {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Integer>> typeRefNodes = new TypeReference<>() {};

    private final String suffix;


    //##################################################################################################################
    // Constructors

    public ProjectImporter(Blackboard blackboard, String suffix) {
        super(blackboard);

        // only for debugging
        //Log.setAdapter(new Log.StandardOutStandardErrorAdapter());
        this.suffix = suffix;
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {
        Path tmpPath = blackboard.getRootPath().resolve(blackboard.getSubPath().resolve(".edu.tum.sse.dirts"));

        try {
            // DependencyGraph
            String graph = Files.readString(tmpPath.resolve(Path.of("graph_" + suffix)));
            blackboard.setGraphOldRevision(DependencyGraph.deserializeGraph(graph));
            blackboard.setGraphNewRevision(DependencyGraph.deserializeGraph(graph));

            // import Nodes
            String checksumsNodes = Files.readString(tmpPath.resolve(Path.of("checksums_" + suffix)));
            blackboard.setChecksumsNodes(objectMapper.readValue(checksumsNodes, typeRefNodes));
        } catch (IOException ignored) {

            blackboard.setGraphNewRevision(new DependencyGraph());
            blackboard.setGraphOldRevision(null);

            blackboard.setChecksumsNodes(new HashMap<>());
        }

        for (DependencyStrategy dependencyStrategy : blackboard.getDependencyStrategies()) {
            dependencyStrategy.doImport(tmpPath, blackboard, suffix);
        }

        return BlackboardState.IMPORTED;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == BlackboardState.CLEAN;
    }
}
