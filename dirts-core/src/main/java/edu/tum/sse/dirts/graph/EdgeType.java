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
package edu.tum.sse.dirts.graph;

/**
 * The type of edge in the DependencyGraph
 * Allows to distinguish the cause of the edge
 */
public enum EdgeType {

    //##################################################################################################################
    // Enum types

    // -----------------------------------------------------------------------------------------------------------------
    // Type level

    NEW("blue"),
    STATIC("black"),
    EXTENDS_IMPLEMENTS("green"),

    // -----------------------------------------------------------------------------------------------------------------
    // NonType level

    MULTIPLE_FIELD_DECL("black"),
    DELEGATION("black"),
    FIELD_ACCESS("green"),
    INHERITANCE("blue"),
    ANNOTATION("magenta"),

    // -----------------------------------------------------------------------------------------------------------------
    // Special

    JUNIT("cyan"),
    DI_SPRING("red"),
    DI_GUICE("red"),
    DI_CDI("red");

    //##################################################################################################################
    // Attributes

    private final String color;

    //##################################################################################################################
    // Constructors

    EdgeType(String color) {
        this.color = color;
    }

    //##################################################################################################################
    // Methods

    @Override
    public String toString() {
        return this.name();
    }

    public String toColorString() {
        return "[color = " + color + "]";
    }
}
