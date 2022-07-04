package edu.tum.sse.dirts.core.knowledgesources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.analysis.def.checksum.ChecksumVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import static edu.tum.sse.dirts.core.BlackboardState.*;

/**
 * Exports checksums, graph and other important information for the next run
 * @param <T>
 */
public class ProjectExporter<T extends BodyDeclaration<?>> extends KnowledgeSource {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    private final Blackboard blackboard;
    private final String suffix;
    private final ChecksumVisitor checksumVisitor;
    private final boolean overwrite;

    public ProjectExporter(Blackboard blackboard,
                           String suffix,
                           ChecksumVisitor checksumVisitor,
                           boolean overwrite) {
        super(blackboard);
        this.blackboard = blackboard;
        this.suffix = suffix;
        this.checksumVisitor = checksumVisitor;
        this.overwrite = overwrite;
    }

    @Override
    public BlackboardState updateBlackboard() {
        if (overwrite) {

            Path tmpPath = blackboard.getRootPath().resolve(blackboard.getSubPath()).resolve(Path.of(".edu.tum.sse.dirts"));

            // DependencyGraph
            DependencyGraph dependencyGraphNewRevision = blackboard.getDependencyGraphNewRevision();

            // remove all nodes that have no edges
            dependencyGraphNewRevision.removeNodesWithoutEdges();

            // Nodes
            Map<String, Node> nodesAdded = blackboard.getNodesAdded();
            Map<String, Node> nodesDifferent = blackboard.getNodesDifferent();
            Map<String, Integer> nodesRemoved = blackboard.getNodesRemoved();
            Map<String, String> nameMapperNodes = blackboard.getNameMapperNodes();

            Map<String, Integer> checksumsNodesNewRevision = new HashMap<>(blackboard.getChecksumsNodes());
            nameMapperNodes.forEach((o, n) -> {
                Integer tmp = checksumsNodesNewRevision.remove(o);
                checksumsNodesNewRevision.put(n, tmp);
            });
            nodesRemoved.keySet().forEach(checksumsNodesNewRevision::remove);
            nodesDifferent.forEach((name, t) -> checksumsNodesNewRevision.put(name, checksumVisitor.hashCode(t)));
            nodesAdded.forEach((name, t) -> checksumsNodesNewRevision.put(name, checksumVisitor.hashCode(t)));

            for (DependencyStrategy dependencyStrategy : blackboard.getDependencyStrategies()) {
                dependencyStrategy.doExport(tmpPath, blackboard, suffix);
            }

            try {

                Files.createDirectories(tmpPath);
                Files.writeString(tmpPath.resolve(Path.of("graph_" + suffix)), dependencyGraphNewRevision.serializeGraph(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.writeString(tmpPath.resolve(Path.of("checksums_" + suffix)),
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(checksumsNodesNewRevision), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            } catch (IOException e) {
                return FAILED;
            }
        }

        return DONE;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == READY_TO_CALCULATE_AFFECTED_TESTS;
    }
}
