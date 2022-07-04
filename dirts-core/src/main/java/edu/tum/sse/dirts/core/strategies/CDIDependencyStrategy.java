package edu.tum.sse.dirts.core.strategies;

import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.def.finders.TypeFinderVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.cdi.analysis.CDIDependencyCollectorVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.util.IgnoreErrorHandler;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.graph.EdgeType.DI_CDI;

/**
 * Contains tasks required by the dependency-analyzing extension for CDI
 */
public class CDIDependencyStrategy implements DependencyStrategy {

    private Set<String> xmlAlternativesNewRevision;

    private final CDIDependencyCollectorVisitor dependencyCollector;

    public CDIDependencyStrategy(CDIDependencyCollectorVisitor dependencyCollector) {
        this.dependencyCollector = dependencyCollector;
    }

    @Override
    public void doImport(Path tmpPath, Blackboard blackboard, String suffix) {
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
    public void doExport(Path tmpPath, Blackboard blackboard, String suffix) {
    }

    @Override
    public void doChangeAnalysis(Blackboard blackboard) {

    }

    @Override
    public void doGraphCropping(Blackboard blackboard) {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        // remove all edges of Type CDI
        dependencyGraph.removeAllEdgesByType(Set.of(DI_CDI));
    }

    @Override
    public void doDependencyAnalysis(Blackboard blackboard) {
        dependencyCollector.setBeanStorage(new BeanStorage<>());
        dependencyCollector.setAlternatives(xmlAlternativesNewRevision);

        List<TypeDeclaration<?>> typeDeclarations = new ArrayList<>();
        TypeFinderVisitor typeFinderVisitor = new TypeFinderVisitor();
        blackboard.getCompilationUnits().forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));
        dependencyCollector.calculateDependencies(typeDeclarations, blackboard.getDependencyGraphNewRevision());
    }

    @Override
    public void combineGraphs(Blackboard blackboard) {}
}
