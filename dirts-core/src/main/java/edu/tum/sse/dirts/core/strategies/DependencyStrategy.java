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
import edu.tum.sse.dirts.core.Blackboard;

import java.nio.file.Path;

/**
 * Represents a strategy for computing dependencies
 */
public interface DependencyStrategy<T extends BodyDeclaration<?>> {

    void doImport(Path tmpPath, Blackboard<T> blackboard, String suffix);

    void doExport(Path tmpPath, Blackboard<T> blackboard, String suffix);

    void doChangeAnalysis(Blackboard<T> blackboard);

    void doGraphCropping(Blackboard<T> blackboard);

    void doDependencyAnalysis(Blackboard<T> blackboard);

    void combineGraphs(Blackboard<T> blackboard);
}