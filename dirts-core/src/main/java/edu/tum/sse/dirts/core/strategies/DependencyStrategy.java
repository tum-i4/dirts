package edu.tum.sse.dirts.core.strategies;

import edu.tum.sse.dirts.core.Blackboard;

import java.nio.file.Path;

/**
 * Represents a strategy for computing dependencies
 */
public interface DependencyStrategy {

    void doImport(Path tmpPath, Blackboard blackboard, String suffix);

    void doExport(Path tmpPath, Blackboard blackboard, String suffix);

    void doChangeAnalysis(Blackboard blackboard);

    void doGraphCropping(Blackboard blackboard);

    void doDependencyAnalysis(Blackboard blackboard);

    void combineGraphs(Blackboard blackboard);
}