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
package edu.tum.sse.dirts.core.strategies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.cdi.analysis.CDIAlternativeDependencyCollector;
import edu.tum.sse.dirts.cdi.analysis.CDIInjectionPointCollectorVisitor;
import edu.tum.sse.dirts.cdi.analysis.CDIMapper;
import edu.tum.sse.dirts.cdi.analysis.identifiers.ManagedBeanIdentifierVisitor;
import edu.tum.sse.dirts.cdi.analysis.identifiers.ProducerFieldIdentifierVisitor;
import edu.tum.sse.dirts.cdi.analysis.identifiers.ProducerMethodIdentifierVisitor;
import edu.tum.sse.dirts.cdi.util.CDIBean;
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
import java.util.*;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.cdi.util.CDIUtil.lookupXMlAlternativeName;
import static edu.tum.sse.dirts.graph.EdgeType.DI_CDI;
import static java.util.logging.Level.SEVERE;

/**
 * Contains tasks required by the dependency-analyzing extension for CDI
 */
public class CDIDependencyStrategy<T extends BodyDeclaration<?>>
        extends DIDependencyStrategy<T, CDIBean>
        implements DependencyStrategy<T> {

    private final static String PREFIX = "cdi";

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Set<String>> typeRefXMLAlternatives = new TypeReference<>() {
    };

    private final CDIAlternativeDependencyCollector<T> alternativeDependencyCollector;

    private Set<String> xmlAlternativesNewRevision, xmlAlternativesOldRevision;
    private Set<String> xmlAlternativesAdded, xmlAlternativesRemoved, xmlAlternativesSame;

    private final BeanStorage<CDIBean> beanStorage = new BeanStorage<>();

    public CDIDependencyStrategy(CDIInjectionPointCollectorVisitor<T> injectionPointCollector,
                                 CDIMapper<T> nameMapper, CDIAlternativeDependencyCollector<T> alternativeDependencyCollector) {
        super(PREFIX, injectionPointCollector, DI_CDI, nameMapper);
        this.alternativeDependencyCollector = alternativeDependencyCollector;
    }

    @Override
    public void doImport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
        super.doImport(tmpPath, blackboard, suffix);

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
            } catch (Throwable e) {
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
        super.doExport(tmpPath, blackboard, suffix);

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
        super.doChangeAnalysis(blackboard);

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
        super.doGraphCropping(blackboard);

        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        xmlAlternativesAdded.forEach(s -> dependencyGraph.addNode(lookupXMlAlternativeName(s)));
        xmlAlternativesRemoved.forEach(s -> dependencyGraph.removeNode(lookupXMlAlternativeName(s)));

    }

    @Override
    protected BeanStorage<CDIBean> collectBeans(Collection<TypeDeclaration<?>> ts) {
        ManagedBeanIdentifierVisitor.identifyDependencies(ts, beanStorage);
        ProducerFieldIdentifierVisitor.identifyDependencies(ts, beanStorage);
        ProducerMethodIdentifierVisitor.identifyDependencies(ts, beanStorage);

        return beanStorage;
    }

    @Override
    public void doDependencyAnalysis(Blackboard<T> blackboard) {
        super.doDependencyAnalysis(blackboard);

        Set<String> alternativesPresent = new HashSet<>();
        alternativesPresent.addAll(xmlAlternativesAdded);
        alternativesPresent.addAll(xmlAlternativesSame);

        alternativeDependencyCollector.setAlternatives(alternativesPresent);

        for (CompilationUnit compilationUnit : blackboard.getCompilationUnits()) {
            compilationUnit.accept(alternativeDependencyCollector, blackboard.getDependencyGraphNewRevision());
        }
    }

    @Override
    public void combineGraphs(Blackboard<T> blackboard) {
        super.combineGraphs(blackboard);

        ModificationGraph modificationGraph = blackboard.getCombinedGraph();

        xmlAlternativesSame.forEach(s -> modificationGraph.setModificationType(lookupXMlAlternativeName(s),
                ModificationType.NOT_MODIFIED));
        xmlAlternativesAdded.forEach(s -> modificationGraph.setModificationType(lookupXMlAlternativeName(s),
                ModificationType.ADDED));
        xmlAlternativesRemoved.forEach(s -> modificationGraph.setModificationType(lookupXMlAlternativeName(s),
                ModificationType.REMOVED));
    }
}
