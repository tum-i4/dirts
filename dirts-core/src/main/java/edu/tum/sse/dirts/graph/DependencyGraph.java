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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simple graph that allows to calculate its transitive closure
 */
public class DependencyGraph {

    //##################################################################################################################
    // Attributes

    protected final Map<String, Set<String>> nodes;

    protected final Map<String, Map<String, Set<EdgeType>>> forwardsEdges;
    protected final Map<String, Map<String, Set<EdgeType>>> backwardsEdges;

    public DependencyGraph() {
        forwardsEdges = new HashMap<>();
        backwardsEdges = new HashMap<>();
        nodes = new HashMap<>();
    }

    public DependencyGraph(Map<String, Set<String>> nodes,
                           Map<String, Map<String, Set<EdgeType>>> forwardsEdges,
                           Map<String, Map<String, Set<EdgeType>>> backwardsEdges) {
        this.nodes = nodes;
        this.forwardsEdges = forwardsEdges;
        this.backwardsEdges = backwardsEdges;
    }

    //##################################################################################################################
    // Getters

    public Map<String, Set<String>> getNodes() {
        return nodes;
    }

    public Map<String, Map<String, Set<EdgeType>>> getForwardsEdges() {
        return forwardsEdges;
    }

    public Map<String, Map<String, Set<EdgeType>>> getBackwardsEdges() {
        return backwardsEdges;
    }


    //##################################################################################################################
    // Methods

    /**
     * Adds a node to the graph
     *
     * @param name Name of the node
     */
    public void addNode(String name) {
        nodes.putIfAbsent(name, new HashSet<>());
        forwardsEdges.putIfAbsent(name, new HashMap<>());
        backwardsEdges.putIfAbsent(name, new HashMap<>());
    }

    /**
     * Removes a node
     *
     * @param name
     */
    public void removeNode(String name) {
        if (nodes.containsKey(name)) {
            // Remove edges from this node
            forwardsEdges.get(name).forEach((to, ts) -> backwardsEdges.get(to).remove(name));
            forwardsEdges.remove(name);

            // Remove edges to this node
            backwardsEdges.get(name).forEach((from, ts) -> forwardsEdges.get(from).remove(name));
            backwardsEdges.remove(name);

            // Remove node
            nodes.remove(name);
        }
    }

    /**
     * Removes all edges that have no ingoing and outgoing edges
     */
    public void removeNodesWithoutEdges() {
        Set<String> tmp = new HashSet<>();
        forwardsEdges.forEach((from, toNodes) -> {
            if (toNodes.isEmpty() && backwardsEdges.get(from).isEmpty()) {
                tmp.add(from);
            }
        });

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

            Set<String> forwards = forwardsEdges.get(oldName).keySet();
            Set<String> backwards = backwardsEdges.get(oldName).keySet();

            forwards.forEach(to -> backwardsEdges.get(to).put(newName, backwardsEdges.get(to).remove(oldName)));
            forwardsEdges.put(newName, forwardsEdges.remove(oldName));

            backwards.forEach(to -> forwardsEdges.get(to).put(newName, forwardsEdges.get(to).remove(oldName)));
            backwardsEdges.put(newName, backwardsEdges.remove(oldName));
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

        Map<String, Set<EdgeType>> fromMap = forwardsEdges.get(from);
        Map<String, Set<EdgeType>> toMap = backwardsEdges.get(to);
        if (!fromMap.containsKey(to)) {
            assert (!toMap.containsKey(from));

            Set<EdgeType> types = new HashSet<>();
            types.add(type);

            fromMap.put(to, types);
            toMap.put(from, types);
        } else {
            fromMap.get(to).add(type);
        }
    }

    /**
     * Entirely removes all edges of certain types
     *
     * @param affectedEdges
     */
    public void removeAllEdgesByType(Set<EdgeType> affectedEdges) {
        Set<String> tmp = new HashSet<>();

        nodes.keySet().forEach(from -> {
            Map<String, Set<EdgeType>> forwardsNodes = forwardsEdges.get(from);
            forwardsNodes.forEach((to, forwardsTypes) -> {
                forwardsTypes.removeAll(affectedEdges);
                if (forwardsTypes.isEmpty())
                    tmp.add(to);
            });
            tmp.forEach(to -> {
                forwardsNodes.remove(to);
                backwardsEdges.get(to).remove(from);
            });
        });

    }

    /**
     * Removes all outgoing edges on a certain node of certain types
     *
     * @param from
     * @param affectedEdges
     * @return names of the nodes that are pointed to by removed edges
     */
    public Set<String> removeAllEdgesFrom(String from, Set<EdgeType> affectedEdges) {
        Set<String> ret = new HashSet<>();
        if (forwardsEdges.containsKey(from)) {

            Map<String, Set<EdgeType>> forwardsNodes = forwardsEdges.get(from);
            forwardsNodes.forEach((to, type) -> {
                type.removeAll(affectedEdges);
                if (type.isEmpty()) {
                    backwardsEdges.get(to).remove(from);
                    ret.add(to);
                }
            });
            ret.forEach(forwardsNodes::remove);
        }
        return ret;
    }

    /**
     * Removes all ingoing edges on a certain node of certain types
     *
     * @param to
     * @param affectedEdges
     * @return names of the nodes that are at the start of removed edges
     */
    public Set<String> removeAllEdgesTo(String to, Set<EdgeType> affectedEdges) {
        Set<String> ret = new HashSet<>();
        if (backwardsEdges.containsKey(to)) {

            Map<String, Set<EdgeType>> backwardsNodes = backwardsEdges.get(to);
            backwardsNodes.forEach((from, type) -> {
                type.removeAll(affectedEdges);
                if (type.isEmpty()) {
                    forwardsEdges.get(from).remove(to);
                    ret.add(from);
                }
            });
            ret.forEach(backwardsNodes::remove);
        }
        return ret;
    }

    //##################################################################################################################
    // IO

    public String serializeGraph() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String nodesString = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(nodes);

        String forwardsString = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(forwardsEdges);

        String backwardsString = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(backwardsEdges);
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
