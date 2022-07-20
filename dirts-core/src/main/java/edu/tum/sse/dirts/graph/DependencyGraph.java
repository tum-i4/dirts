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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tum.sse.dirts.util.Log;

import java.util.*;
import java.util.logging.Level;

/**
 * A simple graph that allows to calculate its transitive closure
 */
public class DependencyGraph {

    //##################################################################################################################
    // Attributes

    protected final Map<String, Set<String>> nodes;

    protected final MultiValueNtoNMap<String, EdgeType> edges;

    public DependencyGraph() {
        nodes = new HashMap<>();
        edges = new MultiValueNtoNMap<>();
    }

    public DependencyGraph(Map<String, Set<String>> nodes,
                           Map<String, Map<String, Set<EdgeType>>> forwardsEdges,
                           Map<String, Map<String, Set<EdgeType>>> backwardsEdges) {
        this.nodes = nodes;
        edges = new MultiValueNtoNMap<>(forwardsEdges, backwardsEdges);
    }

    //##################################################################################################################
    // Getters

    public Set<String> getNodes() {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    Set<String> getMessages(String node) {
        return nodes.get(node);
    }

    Map<String, Map<String, Set<EdgeType>>> getForwardsEdges() {
        return Collections.unmodifiableMap(edges.getRegularMap());
    }

    Map<String, Map<String, Set<EdgeType>>> getBackwardsEdges() {
        return Collections.unmodifiableMap(edges.getInverseMap());
    }


    //##################################################################################################################
    // Methods

    /**
     * Adds a node to the graph
     *
     * @param name Name of the node
     */
    public void addNode(String name) {
        nodes.computeIfAbsent(name, n -> new HashSet<>());
    }

    /**
     * Removes a node
     *
     * @param name
     */
    public void removeNode(String name) {
        if (nodes.containsKey(name)) {
            edges.remove(name);
            nodes.remove(name);
        } else {
            Log.log(Level.WARNING, "Expected node <" + name + "> to be present in the graph, when removing it.");
        }
    }

    /**
     * Removes all edges that have no ingoing and outgoing edges
     */
    public void removeNodesWithoutEdges() {
        Set<String> tmp = new HashSet<>();
        for (String node : nodes.keySet()) {
            if (!edges.getRegularMap().containsKey(node) && !edges.getInverseMap().containsKey(node)) {
                tmp.add(node);
            }
        }

        tmp.forEach(this::removeNode);
    }

    /**
     * Rename a node
     *
     * @param oldName
     * @param newName
     */
    public void renameNode(String oldName, String newName) {
        if (nodes.containsKey(oldName)) {
            nodes.put(newName, nodes.remove(oldName));
            edges.rename(oldName, newName);
        }
    }

    public boolean isNodePresent(String name) {
        return nodes.containsKey(name);
    }

    /**
     * Adds a message to a node that is printed as a comment
     *
     * @param name    Name of the node
     * @param message Message
     */
    public void addMessage(String name, String message) {
        addNode(name);
        nodes.get(name).add(message);
    }

    /**
     * Adds a directed edge between two nodes
     *
     * @param from Start
     * @param to   End
     * @param type Metadata containing the cause of this edge
     */
    public void addEdge(String from, String to, EdgeType type) {
        if (!nodes.containsKey(from))
            addNode(from);
        if (!nodes.containsKey(to))
            addNode(to);

        edges.put(from, to, type);
    }

    /**
     * Entirely removes all edges of certain types
     *
     * @param affectedEdges
     */
    public void removeAllEdgesByType(Set<EdgeType> affectedEdges) {
        edges.removeAllValues(affectedEdges);

    }

    /**
     * Removes all outgoing edges on a certain node of certain types
     *
     * @param from
     * @param affectedEdges
     * @return names of the nodes that are pointed to by removed edges
     */
    public Set<String> removeAllEdgesFrom(String from, Set<EdgeType> affectedEdges) {
        return edges.removeRegularEntries(from, affectedEdges);
    }

    /**
     * Removes all ingoing edges on a certain node of certain types
     *
     * @param to
     * @param affectedEdges
     * @return names of the nodes that are at the start of removed edges
     */
    public Set<String> removeAllEdgesTo(String to, Set<EdgeType> affectedEdges) {
        return edges.removeInverseEntries(to, affectedEdges);
    }

    //##################################################################################################################
    // IO

    public String serializeGraph() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String nodesString = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(nodes);

        String forwardsString = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(getForwardsEdges());

        String backwardsString = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(getBackwardsEdges());
        return nodesString + "\n\n" + forwardsString + "\n\n" + backwardsString;
    }

    public static DependencyGraph deserializeGraph(String input) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String[] split = input.split("\n\n");

        String nodesString = split[0];
        String forwardsString = split[1];
        String backwardsString = split[2];

        TypeReference<Map<String, Set<String>>> typeRefNodes
                = new TypeReference<>() {
        };

        TypeReference<Map<String, Map<String, Set<EdgeType>>>> typeRefForwardsEdges
                = new TypeReference<>() {
        };

        TypeReference<Map<String, Map<String, Set<EdgeType>>>> typeRefBackwardsEdges
                = new TypeReference<>() {
        };

        return new DependencyGraph(mapper.readValue(nodesString, typeRefNodes),
                mapper.readValue(forwardsString, typeRefForwardsEdges),
                mapper.readValue(backwardsString, typeRefBackwardsEdges));
    }
}
