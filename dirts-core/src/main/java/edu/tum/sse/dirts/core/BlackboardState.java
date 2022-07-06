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
package edu.tum.sse.dirts.core;

/**
 * Used to coordinate order in which KnowledgeSources are applied
 */
public enum BlackboardState {

    //##################################################################################################################
    // Enum types

    /* initial state */
    CLEAN,

    /* required */
    IMPORTED,               // ProjectImporter
    TYPE_SOLVER_SET,        // TypeSolverInitializer
    PARSED,                  // Parser
    TESTS_FOUND,            // TestFinder
    NODES_CHANGES_SET,      // CodeChangeAnalyzer<T,T>
    NEW_GRAPH_SET,          // GraphCropper

    DEPENDENCIES_UPDATED,   // DEPENDENCY_ANALYZER

    /* semi-terminal state */
    READY_TO_CALCULATE_AFFECTED_TESTS,

    /* terminal states */
    DONE,
    FAILED;

    //##################################################################################################################
    // Methods

    @Override
    public String toString() {
        switch (this) {
            case CLEAN:
                return "";
            case IMPORTED:
                return "Importing graph of the old revision";
            case TYPE_SOLVER_SET:
                return "Setting up type solvers";
            case PARSED:
                return "Parsing";
            case TESTS_FOUND:
                return "Finding tests";
            case NODES_CHANGES_SET:
                return "Calculating changes";
            case NEW_GRAPH_SET:
                return "Cropping dependency graph";
            case DEPENDENCIES_UPDATED:
                return "Calculating dependencies";
            case READY_TO_CALCULATE_AFFECTED_TESTS:
                return "Combining graphs";
            case DONE:
                return "Exporting graph of the new revision";
            case FAILED:
                return "Failed, but up to this point";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public boolean isTerminalState() {
        switch(this) {
            case DONE:
            case FAILED:
                return true;
            default:
                return false;
        }
    }

    public boolean isFailedState() {
        return this == BlackboardState.FAILED;
    }

    public boolean isDoneState() {
        return this == BlackboardState.DONE;
    }
}
