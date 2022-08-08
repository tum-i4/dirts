package edu.tum.sse.dirts.core.knowledgesources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.def.checksum.ChecksumVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.util.DirtsUtil;
import edu.tum.sse.dirts.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.tum.sse.dirts.core.BlackboardState.*;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.WARNING;

/**
 * Exports checksums, graph and other important information for the next run
 */
public class ProjectExporter<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Set<String>> typeRefAffectedNodes = new TypeReference<>() {
    };

    private final Blackboard<T> blackboard;
    private final ChecksumVisitor<T> checksumVisitor;
    private final boolean overwrite;

    public ProjectExporter(Blackboard<T> blackboard,
                           ChecksumVisitor<T> checksumVisitor,
                           boolean overwrite) {
        super(blackboard);
        this.blackboard = blackboard;
        this.checksumVisitor = checksumVisitor;
        this.overwrite = overwrite;
    }

    @Override
    public BlackboardState updateBlackboard() {
        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();
        String suffix = blackboard.getSuffix();

        if (overwrite) {
            Path tmpPath = rootPath.resolve(subPath).resolve(Path.of(".dirts"));

            // DependencyGraph
            DependencyGraph dependencyGraphNewRevision = blackboard.getDependencyGraphNewRevision();

            // remove all nodes that have no edges
            dependencyGraphNewRevision.removeNodesWithoutEdges();

            Map<String, Node> nodesAdded = blackboard.getNodesAdded();
            Map<String, Node> nodesDifferent = blackboard.getNodesDifferent();
            Map<String, Integer> nodesRemoved = blackboard.getNodesRemoved();

            // Checksums
            Map<String, Integer> checksumsNodesNewRevision = new HashMap<>(blackboard.getChecksumsNodes());
            nodesRemoved.keySet().forEach(checksumsNodesNewRevision::remove);
            nodesDifferent.forEach((name, t) -> checksumsNodesNewRevision.put(name, checksumVisitor.hashCode(t)));
            nodesAdded.forEach((name, t) -> checksumsNodesNewRevision.put(name, checksumVisitor.hashCode(t)));

            // CompilationUnits mapping
            Map<String, String> compilationUnitsMappingNew = new HashMap<>(blackboard.getCompilationUnitMapping());
            nodesRemoved.keySet().forEach(compilationUnitsMappingNew::remove);
            nodesDifferent.forEach((name, t) -> {
                Optional<CompilationUnit> maybeCompilationUnit = t.findCompilationUnit();
                if (maybeCompilationUnit.isPresent()) {
                    CompilationUnit compilationUnit = maybeCompilationUnit.get();
                    Optional<TypeDeclaration<?>> maybePrimaryType = compilationUnit.getPrimaryType();
                    maybePrimaryType.ifPresent(typeDeclaration -> compilationUnitsMappingNew.put(name, lookup(typeDeclaration).getFirst()));
                }
            });
            nodesAdded.forEach((name, t) -> {
                Optional<CompilationUnit> maybeCompilationUnit = t.findCompilationUnit();
                if (maybeCompilationUnit.isPresent()) {
                    CompilationUnit compilationUnit = maybeCompilationUnit.get();
                    Optional<TypeDeclaration<?>> maybePrimaryType = compilationUnit.getPrimaryType();
                    maybePrimaryType.ifPresent(typeDeclaration -> compilationUnitsMappingNew.put(name, lookup(typeDeclaration).getFirst()));
                }
            });

            for (DependencyStrategy<T> dependencyStrategy : blackboard.getDependencyStrategies()) {
                dependencyStrategy.doExport(tmpPath, blackboard, suffix);
            }

            try {

                Files.createDirectories(tmpPath);
                Files.writeString(tmpPath.resolve(Path.of("graph_" + suffix)), dependencyGraphNewRevision.serializeGraph(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.writeString(tmpPath.resolve(Path.of("checksums_" + suffix)),
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(checksumsNodesNewRevision),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.writeString(tmpPath.resolve(Path.of("cuMapping_" + suffix)),
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(compilationUnitsMappingNew),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            } catch (IOException e) {
                return FAILED;
            }

            HashSet<String> affectedNodes = new HashSet<>();
            affectedNodes.addAll(nodesAdded.keySet());
            affectedNodes.addAll(nodesRemoved.keySet());
            affectedNodes.addAll(nodesDifferent.keySet());
            writeAffectedNodes(affectedNodes, blackboard.getNodesSame().keySet());
        }

        return DONE;
    }

    private void writeAffectedNodes(Set<String> affectedNodes, Set<String> nonAffectedNodes) {
        Path changedNodesPath = DirtsUtil.getChangedNodesPath(blackboard.getRootPath());

        if (!Files.exists(changedNodesPath)) {
            try {
                Files.createDirectories(changedNodesPath.getParent());
                Files.createFile(changedNodesPath);
            } catch (IOException e) {
                Log.errLog(WARNING, "Failed to create file containing changed nodes: " + e.getMessage());
            }
        }

        try {
            String changedNodesContent = Files.readString(changedNodesPath);
            Set<String> changedNodes = new HashSet<>();
            if (!changedNodesContent.isEmpty()) {
                changedNodes.addAll(objectMapper.readValue(changedNodesContent, typeRefAffectedNodes));
            }

            changedNodes.removeAll(nonAffectedNodes);
            changedNodes.addAll(affectedNodes);

            Files.writeString(
                    changedNodesPath,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(changedNodes),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            Log.errLog(WARNING, "Failed to read or write file containing changed nodes: " + e.getMessage());
        }
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == READY_TO_CALCULATE_AFFECTED_TESTS;
    }
}
