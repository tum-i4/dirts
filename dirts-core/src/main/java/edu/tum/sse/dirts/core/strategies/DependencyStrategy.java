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