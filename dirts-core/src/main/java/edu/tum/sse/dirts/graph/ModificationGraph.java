package edu.tum.sse.dirts.graph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A graph containing information about the modification status of nodes
 * The graph can be traversed to find impacted nodes
 */
public class ModificationGraph extends DependencyGraph {

    private final Map<String, ModificationType> modificationStatus = new HashMap<>();

    private final DependencyGraph oldRevision;
    private final DependencyGraph newRevision;

    public ModificationGraph(
            DependencyGraph oldRevision,
            DependencyGraph newRevision) {
        this.oldRevision = oldRevision;
        this.newRevision = newRevision;

        // *****************************
        // combine graphs
        // *****************************

        if (newRevision != null) {

            // add all nodes from new revision
            newRevision.nodes.forEach((node, messages) -> {
                addNode(node);
                nodes.get(node).addAll(messages);
            });

            // add all edges from new revision
            newRevision.getForwardsEdges().forEach((fromNode, to) ->
                    to.forEach((toNode, edges) ->
                            edges.forEach(e -> addEdge(fromNode, toNode, e))));
        }

        // add all nodes from old revision, probably renamed
        if (oldRevision != null) {
            oldRevision.nodes.forEach((node, messages) -> {
                addNode(node);
                nodes.get(node).addAll(messages);
            });

            // add all edges from old revision, nodes probably renamed
            oldRevision.getForwardsEdges().forEach((fromNode, to) ->
                    to.forEach((toNode, edges) ->
                            edges.forEach(e -> addEdge(
                                    fromNode,
                                    toNode,
                                    e))));
        }
    }

    /**
     * Add information on modification based on information computed from checksums or similar
     *
     * @param nodesSame nodes that did not change
     * @param nodesDifferent nodes that did change
     * @param nodesAdded nodes that have been added
     * @param nodesRemoved nodes that have been removed
     */
    public void setModificationByStatus(Set<String> nodesSame,
                                        Set<String> nodesDifferent,
                                        Set<String> nodesAdded,
                                        Set<String> nodesRemoved) {
        for (String same : nodesSame) {
            modificationStatus.replace(same, ModificationType.NOT_MODIFIED);
        }
        for (String different : nodesDifferent) {
            modificationStatus.replace(different, ModificationType.MODIFIED);
        }
        for (String added : nodesAdded) {
            modificationStatus.replace(added, ModificationType.ADDED);
        }
        for (String removed : nodesRemoved) {
            modificationStatus.replace(removed, ModificationType.REMOVED);
        }
    }

    /**
     * Compute modifications based on differences in dependencies
     */
    public void setModificationByDependencies() {
        if (oldRevision != null) {

            // *****************************
            // compare graphs
            // *****************************

            // set all nodes that have different outgoing edges to CHANGED_DEPENDENCIES
            nodes.forEach((fromNode, m) -> {
                if (!modificationStatus.get(fromNode).isRelevant()
                        && newRevision.nodes.containsKey(fromNode) && oldRevision.nodes.containsKey(fromNode)
                        && oldRevision.getForwardsEdges().containsKey(fromNode)
                        && newRevision.getForwardsEdges().containsKey(fromNode)
                        && !oldRevision.getForwardsEdges().get(fromNode).equals(newRevision.getForwardsEdges().get(fromNode))
                ) {
                    setModificationType(fromNode, ModificationType.CHANGED_DEPENDENCIES);
                }
            });
        }
    }

    /**
     * Adds a node to the graph
     *
     * @param name Name of the node
     */
    @Override
    public void addNode(String name) {
        super.addNode(name);
        modificationStatus.putIfAbsent(name, ModificationType.UNKNOWN);
    }

    /**
     * Removes a node from the graph
     *
     * @param name node to remvoe
     */
    @Override
    public void removeNode(String name) {
        super.removeNode(name);
        modificationStatus.remove(name);
    }

    public void setModificationType(String name, ModificationType type) {
        addNode(name);
        modificationStatus.put(name, type);
    }

    public void setModificationTypeIfPresent(String name, ModificationType type) {
        if (nodes.containsKey(name))
            modificationStatus.put(name, type);
    }

    public ModificationType getModificationType(String name) {
        return modificationStatus.getOrDefault(name, ModificationType.UNKNOWN);
    }

    /**
     * Can be used to trace tests starting at modified code entities
     * <p>
     * The idea for this algorithm is from AutoRTS
     * (J. Öqvist, G. Hedin, and B. Magnusson, “Extraction-based regression test selection,”
     * ACM Int. Conf. Proceeding Ser., vol. Part F1284, 2016, doi: 10.1145/2972206.2972224)
     *
     * @return Map of
     * modified node and its modification status
     * to all nodes that reach this node in the transitive closure of the graph
     */
    public Map<String, Set<String>> affected() {
        return nodes.keySet().stream()
                .filter(n -> modificationStatus.get(n).isRelevant())
                .collect(Collectors.toMap(n -> n + modificationStatus.get(n), this::reachableNodes));
    }

    /**
     * Can be used to trace tests that are affected by a certain type of edge
     * <p>
     * The path in the graph from reached node to modified code entity is guaranteed
     * to contain at least one edge of the specified type
     *
     * @param edgeType appears at least once in a path from a reached node to a modified code entity
     * @return Map of
     * modified node and its modification status
     * to all nodes that reach this node in the transitive closure of the graph with the above-mentioned property
     */
    public Map<String, Set<String>> affectedByEdgeType(Set<EdgeType> edgeType) {
        // Find all nodes that have an ingoing edge with specified type
        Set<String> atTheEndOfEdge = getBackwardsEdges().entrySet().stream()
                .filter(e -> e.getValue().values().stream().flatMap(Collection::stream).anyMatch(edgeType::contains))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<String, Set<String>> reachModifiedNode = new HashMap<>();
        for (String node : atTheEndOfEdge) {
            Optional<String> result = reachModifiedNode(node, new HashSet<>(), new HashSet<>());
            result.ifPresent(s -> {
                String key = s + modificationStatus.get(s);
                reachModifiedNode.computeIfAbsent(key, e -> new HashSet<>());
                reachModifiedNode.get(key).add(node);
            });
        }

        // Find nodes at the other end of the specified edges
        reachModifiedNode.entrySet()
                .forEach(e -> e.setValue(e.getValue().stream()
                        .flatMap(p -> reachableNodes(p).stream()).collect(Collectors.toSet())));

        // Add nodes that have outgoing edges with specified type and changed dependencies
        getForwardsEdges().entrySet().stream()
                .filter(e -> e.getValue().values().stream().flatMap(Collection::stream).anyMatch(edgeType::contains))
                .map(Map.Entry::getKey)
                .forEach(start -> {
                    if (modificationStatus.get(start).isRelevant()) {
                        String key = start + modificationStatus.get(start);
                        reachModifiedNode.computeIfAbsent(key, e -> new HashSet<>());
                        reachModifiedNode.get(key).add(start);
                    }
                });

        return reachModifiedNode;
    }

    /**
     * Computes all reachable nodes via breadth-first search
     *
     * @param startingPoint node to start searching from
     * @return reachable nodes
     */
    private Set<String> reachableNodes(String startingPoint) {
        Queue<String> queue = new LinkedList<>();
        Set<String> reached = new HashSet<>();

        queue.add(startingPoint);

        while (!queue.isEmpty()) {
            String dependencyNode = queue.poll();
            reached.add(dependencyNode);

            Map<String, Set<EdgeType>> edges = getBackwardsEdges().get(dependencyNode);
            if (edges != null)
                for (String outgoingNode : edges.keySet()) {
                    if (!reached.contains(outgoingNode)) {
                        queue.add(outgoingNode);
                    }
                }
        }

        return reached;
    }

    /**
     * Calculates if a node reaches a modified node in the transitive closure of the graph
     *
     * @param node     node to check
     * @param positive accumulator to store affected nodes for subsequent invocations
     * @return Name of the modified node if found one, empty otherwise
     */
    private Optional<String> reachModifiedNode(String node, Set<String> positive, Set<String> visited) {
        // Performs depth-first-search while adding nodes that lead to modified nodes to the accumulator
        ModificationType modificationType = modificationStatus.get(node);
        if (node == null)
            return Optional.empty();
        if (positive.contains(node))
            return Optional.of(node);
        if (visited.contains(node)) // to check for cycles
            return Optional.empty();
        if (modificationType.isRelevant()) {
            positive.add(node);
            return Optional.of(node);
        }
        visited.add(node);

        Map<String, Set<EdgeType>> edges = getForwardsEdges().get(node);
        if (edges != null) {
            for (String end : edges.keySet()) {
                Optional<String> tmp = reachModifiedNode(end, positive, visited);
                if (tmp.isPresent()) {
                    positive.add(node);
                    return tmp;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return new GraphPrinter(this).toString();
    }
}
