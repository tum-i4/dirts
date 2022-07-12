package edu.tum.sse.dirts.core.strategies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.def.finders.TypeFinderVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.cdi.analysis.CDIDependencyCollectorVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.ModificationGraph;
import edu.tum.sse.dirts.graph.ModificationType;
import edu.tum.sse.dirts.util.IgnoreErrorHandler;
import edu.tum.sse.dirts.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.cdi.util.CDIUtil.lookupXMlAlternativeName;
import static edu.tum.sse.dirts.graph.EdgeType.DI_CDI;
import static java.util.logging.Level.SEVERE;

/**
 * Contains tasks required by the dependency-analyzing extension for CDI
 */
public class CDIDependencyStrategy<T extends BodyDeclaration<?>> implements DependencyStrategy<T> {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Set<String>> typeRefXMLAlternatives = new TypeReference<>() {
    };

    private Set<String> xmlAlternativesNewRevision, xmlAlternativesOldRevision;

    private Set<String> xmlAlternativesAdded, xmlAlternativesRemoved, xmlAlternativesSame;

    private final CDIDependencyCollectorVisitor<T> dependencyCollector;

    public CDIDependencyStrategy(CDIDependencyCollectorVisitor<T> dependencyCollector) {
        this.dependencyCollector = dependencyCollector;
    }

    @Override
    public void doImport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();
        Set<Path> beansXMLPaths = findBeansXMLFiles(rootPath.resolve(subPath));

        xmlAlternativesNewRevision = new HashSet<>();
        for (Path beansXMLPath : beansXMLPaths) {
            File xmlFile = beansXMLPath.toFile();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder();
                builder.setErrorHandler(new IgnoreErrorHandler());

                if (xmlFile.exists()) {
                    Document document = builder.parse(xmlFile);
                    Element root = document.getDocumentElement();

                    NodeList alternatives = root.getElementsByTagName("alternatives");
                    for (int i = alternatives.getLength() - 1; i >= 0; i--) {
                        Node alternativeNode = alternatives.item(i);
                        if (alternativeNode instanceof Element) {
                            Element alternative = (Element) alternativeNode;
                            NodeList classNodes = alternative.getElementsByTagName("class");
                            for (int j = classNodes.getLength() - 1; j >= 0; j--) {
                                Node classNode = classNodes.item(j);
                                String textContent = classNode.getTextContent();
                                xmlAlternativesNewRevision.add(textContent);
                            }
                        }
                    }
                }
            } catch (IOException | SAXException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // import Nodes
        try {
            String alternativesString = Files.readString(tmpPath.resolve(Path.of("cdi_alternatives_" + suffix)));
            xmlAlternativesOldRevision = objectMapper.readValue(alternativesString, typeRefXMLAlternatives);
        } catch (IOException e) {
            xmlAlternativesOldRevision = new HashSet<>();
        }
    }

    private Set<Path> findBeansXMLFiles(Path path) {
        try {
            return Files.walk(path)
                    .filter(p -> !p.toAbsolutePath().toString().contains("/target/"))
                    .filter(p -> p.toAbsolutePath().toString().endsWith("META-INF/beans.xml"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return new HashSet<>();
        }
    }

    @Override
    public void doExport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
        try {
            Files.createDirectories(tmpPath);

            Files.writeString(tmpPath.resolve(Path.of("cdi_alternatives_" + suffix)),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(xmlAlternativesNewRevision),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException ignored) {
            Log.log(SEVERE, "Failed to export CDI alternative entries");
        }
    }

    @Override
    public void doChangeAnalysis(Blackboard<T> blackboard) {
        xmlAlternativesAdded = new HashSet<>();
        xmlAlternativesRemoved = new HashSet<>();
        xmlAlternativesSame = new HashSet<>();

        xmlAlternativesAdded.addAll(xmlAlternativesNewRevision);
        xmlAlternativesAdded.removeAll(xmlAlternativesOldRevision);

        xmlAlternativesRemoved.addAll(xmlAlternativesOldRevision);
        xmlAlternativesRemoved.removeAll(xmlAlternativesNewRevision);

        xmlAlternativesSame.addAll(xmlAlternativesNewRevision);
        xmlAlternativesSame.retainAll(xmlAlternativesOldRevision);
    }

    @Override
    public void doGraphCropping(Blackboard<T> blackboard) {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        xmlAlternativesAdded.forEach(s -> dependencyGraph.addNode(lookupXMlAlternativeName(s)));
        xmlAlternativesRemoved.forEach(s -> dependencyGraph.removeNode(lookupXMlAlternativeName(s)));

        // remove all edges of Type CDI
        dependencyGraph.removeAllEdgesByType(Set.of(DI_CDI));
    }

    @Override
    public void doDependencyAnalysis(Blackboard<T> blackboard) {
        dependencyCollector.setBeanStorage(new BeanStorage<>());
        dependencyCollector.setAlternatives(xmlAlternativesNewRevision);

        List<TypeDeclaration<?>> typeDeclarations = new ArrayList<>();
        TypeFinderVisitor typeFinderVisitor = new TypeFinderVisitor();
        blackboard.getCompilationUnits().forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));
        dependencyCollector.calculateDependencies(typeDeclarations, blackboard.getDependencyGraphNewRevision());
    }

    @Override
    public void combineGraphs(Blackboard<T> blackboard) {
        ModificationGraph modificationGraph = blackboard.getCombinedGraph();

        xmlAlternativesSame.forEach(s -> modificationGraph.setModificationType(lookupXMlAlternativeName(s),
                ModificationType.NOT_MODIFIED));
        xmlAlternativesAdded.forEach(s -> modificationGraph.setModificationType(lookupXMlAlternativeName(s),
                ModificationType.ADDED));
        xmlAlternativesRemoved.forEach(s -> modificationGraph.setModificationType(lookupXMlAlternativeName(s),
                ModificationType.REMOVED));
    }
}
