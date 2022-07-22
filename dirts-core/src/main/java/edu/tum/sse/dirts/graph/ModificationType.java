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
package edu.tum.sse.dirts.graph;

/**
 * The type of node in the DependencyGraph
 * Allows to distinguish the modification status
 */
public enum ModificationType {

    //##################################################################################################################
    // Enum types

    NOT_MODIFIED(""),
    MODIFIED(" M"),
    ADDED(" A"),
    REMOVED(" R"),
    EXTERNALLY_MODIFIED(" E"),
    UNKNOWN(" U"),
    CHANGED_DEPENDENCIES(" D");

    //##################################################################################################################
    // Attributes

    private final String suffix;

    //##################################################################################################################
    // Constructors

    ModificationType(String postfix) {
        this.suffix = postfix;
    }

    //##################################################################################################################
    // Methods

    public boolean isRelevant() {
        return this.equals(MODIFIED)
                || this.equals(ADDED)
                || this.equals(REMOVED)
                || this.equals(CHANGED_DEPENDENCIES)
                || this.equals(EXTERNALLY_MODIFIED);
    }

    @Override
    public String toString() {
        return suffix;
    }
}
