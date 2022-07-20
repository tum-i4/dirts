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

import java.util.*;
import java.util.stream.Collectors;

public class GraphPrinter {

    private final ModificationGraph graph;

    public GraphPrinter(ModificationGraph graph) {
        this.graph = graph;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");

        Map<String, String> nodesMap = new HashMap<>();
        graph.getNodes().forEach(n -> {
            String qualifier;
            if (n.startsWith("Unresolved")) {
                qualifier = "";
            } else {
                qualifier = n.substring(0, n.contains("(") ? n.indexOf('(') : n.length());
                qualifier = qualifier.substring(0, qualifier.contains("<") ? qualifier.indexOf('<') : qualifier.length());
                //qualifier = qualifier.substring(0, qualifier.contains("!") ? qualifier.indexOf('!') : qualifier.length());
            }
            nodesMap.put(qualifier, n);
        });

        printSubgraphs(sb, null, 0, nodesMap);

        printEdges(sb);

        sb.append("}");
        return sb.toString();
    }

    private void printSubgraphs(StringBuilder sb, String subgraphName, int indentLevel, Map<String, String> nodes) {
        List<String> nodesInSubgraph = new ArrayList<>();
        Map<String, Map<String, String>> otherSubgraphs = new HashMap<>();

        nodes.forEach((q, n) -> {
            if (q.contains(".")) {
                String prefix = q.substring(0, q.indexOf('.'));
                String remainder = q.substring(q.indexOf('.') + 1);
                Map<String, String> otherSubgraph;
                if (otherSubgraphs.containsKey(prefix)) {
                    otherSubgraph = otherSubgraphs.get(prefix);
                } else {
                    otherSubgraph = new HashMap<>();
                    otherSubgraphs.put(prefix, otherSubgraph);
                }
                otherSubgraph.put(remainder, n);
            } else {
                nodesInSubgraph.add(n);
            }
        });

        if (subgraphName != null) {
            sb.append("    ".repeat(indentLevel))
                    .append("subgraph ")
                    .append("cluster_")
                    .append(subgraphName.replaceAll("-", ""))
                    .append("{\n")
                    .append("    ".repeat(indentLevel + 1))
                    .append("label = \"")
                    .append(subgraphName)
                    .append("\"")
                    .append("\n")
                    .append("    ".repeat(indentLevel + 1))
                    .append("style = filled\n");
            sb.append("    ".repeat(indentLevel + 1));
            if (indentLevel % 2 == 1) {
                sb.append("color = lightgrey\n");
            } else {
                sb.append("color = white\n");
            }
        }

        nodesInSubgraph.stream()
                .sorted()
                .forEach(node -> {
                    if (graph.getForwardsEdges().get(node) == null && graph.getBackwardsEdges().get(node) == null) {
                        sb.append("//  ");
                        printNode(sb, node, indentLevel);
                    } else {
                        printNode(sb, node, indentLevel + 1);
                    }
                });

        otherSubgraphs.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> printSubgraphs(sb, e.getKey(), indentLevel + 1, e.getValue()));

        if (subgraphName != null) {
            sb.append("    ".repeat(indentLevel))
                    .append("}\n");
        }
    }

    private void printEdges(StringBuilder sb) {
        graph.getForwardsEdges().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(n -> n.getValue().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(o -> {
                                    for (EdgeType edgeType : o.getValue()) {
                                        printNode(sb, n.getKey());
                                        sb.append(" -> ");
                                        printNode(sb, o.getKey());
                                        sb.append(" ");
                                        sb.append(edgeType.toColorString());
                                        sb.append("\n");
                                    }
                                }
                        ));
    }

    private void printNode(StringBuilder sb, String node, int indentLevel) {
        sb.append("    ".repeat(indentLevel));
        printNode(sb, node);
        Set<String> messages = graph.getMessages(node);
        if (messages != null && !messages.isEmpty()) {
            sb.append(" // ");
            sb.append(messages.stream()
                    .map(m -> {
                        String suffix = "";
                        if (m.contains(":")) {
                            suffix = m.substring(m.lastIndexOf(":"));
                            m = m.substring(0, m.lastIndexOf(":"));
                        }
                        return shorten(m) + shorten(suffix);
                    })
                    .collect(Collectors.joining(", ")));
        }
        sb.append("\n");
    }

    private void printNode(StringBuilder sb, String node) {
        sb.append("\"").append(node).append(graph.getModificationType(node)).append("\"");
    }

    private String shorten(String arg) {
        String ret = arg;
        if (ret.length() > 50) {
            ret = ret.substring(0, 50) + "...";
        }
        if (ret.contains("\n")) {
            ret = ret.substring(0, ret.indexOf("\n")) + "...";
        }
        return ret;
    }
}
