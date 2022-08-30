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
package edu.tum.sse.dirts.core.strategies;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * Contains tasks required by a caching strategy for calculating dependencies
 * Incrementally updates the graph of the old revision based on a concept by
 * (J. Öqvist, G. Hedin, and B. Magnusson, “Extraction-based regression test selection,”
 * ACM Int. Conf. Proceeding Ser., vol. Part F1284, 2016, doi: 10.1145/2972206.2972224)
 */
public class CachingDependencyStrategy<T extends BodyDeclaration<?>> implements DependencyStrategy<T> {

    private final Set<DependencyCollector<T>> dependencyCollector;

    public CachingDependencyStrategy(Set<DependencyCollector<T>> dependencyCollector) {
        this.dependencyCollector = dependencyCollector;
    }


    @Override
    public void doImport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
    }

    @Override
    public void doExport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
    }

    @Override
    public void doChangeAnalysis(Blackboard<T> blackboard) {
    }

    @Override
    public void doGraphCropping(Blackboard<T> blackboard) {
    }

    @Override
    public void doDependencyAnalysis(Blackboard<T> blackboard) {
        Collection<TypeDeclaration<?>> impactedTypes = blackboard.getImpactedTypes();
        dependencyCollector.forEach(d -> d.calculateDependencies(
                impactedTypes,
                blackboard.getDependencyGraphNewRevision()));
    }

    @Override
    public void combineGraphs(Blackboard<T> blackboard) {

    }
}
