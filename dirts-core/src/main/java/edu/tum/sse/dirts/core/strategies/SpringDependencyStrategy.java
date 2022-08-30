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
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.knowledgesources.ChangeAnalyzer;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.spring.analysis.SpringBeanDependencyCollector;
import edu.tum.sse.dirts.spring.analysis.SpringInjectionPointCollectorVisitor;
import edu.tum.sse.dirts.spring.analysis.SpringMapper;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;
import edu.tum.sse.dirts.spring.analysis.identifiers.SpringBeanMethodIdentifierVisitor;
import edu.tum.sse.dirts.spring.analysis.identifiers.SpringComponentIdentifierVisitor;
import edu.tum.sse.dirts.spring.analysis.identifiers.SpringXMLBeanIdentifier;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.graph.EdgeType.DI_SPRING;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupTypeDeclaration;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Contains tasks required by the dependency-analyzing extension for Spring
 */
public class SpringDependencyStrategy<T extends BodyDeclaration<?>>
        extends DIDependencyStrategy<T, SpringBean>
        implements DependencyStrategy<T> {

    private final static String PREFIX = "spring";

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Integer>> typeRefXMLBeans = new TypeReference<>() {
    };

    private final SpringBeanDependencyCollector<T> springBeanDependencyCollector;

    private Map<String, Integer> checksumsXmlBeansOldRevision;
    private Map<String, XMLBeanDefinition> xmlBeansNewRevision;

    private final Map<String, XMLBeanDefinition> sameBeans = new HashMap<>();
    private final Map<String, XMLBeanDefinition> differentBeans = new HashMap<>();
    private final Map<String, XMLBeanDefinition> addedBeans = new HashMap<>();
    private final Map<String, Integer> removedBeans = new HashMap<>();

    private final BeanStorage<SpringBean> beanStorage = new BeanStorage<>();

    public SpringDependencyStrategy(SpringInjectionPointCollectorVisitor<T> injectionPointCollector,
                                    SpringBeanDependencyCollector<T> springBeanDependencyCollector,
                                    SpringMapper<T> nameMapper) {
        super(PREFIX, injectionPointCollector, DI_SPRING, nameMapper);
        this.springBeanDependencyCollector = springBeanDependencyCollector;
    }

    @Override
    public void doImport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
        super.doImport(tmpPath, blackboard, suffix);

        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();

        try {
            Set<Path> xmlPaths = findXMLFiles(rootPath.resolve(subPath));

            SpringXMLBeanIdentifier springXMLBeanIdentifier = new SpringXMLBeanIdentifier();
            xmlPaths.forEach(p -> springXMLBeanIdentifier.processXMLFile(rootPath, p));

            xmlBeansNewRevision = springXMLBeanIdentifier.getBeans();

        } catch (IOException e) {
            Log.log(WARNING, "Failed to read xml files, that may contain spring beans");
        }

        // import Nodes
        try {
            String checksumsString = Files.readString(tmpPath.resolve(Path.of("spring_xmlbeans_" + suffix)));
            checksumsXmlBeansOldRevision = objectMapper.readValue(checksumsString, typeRefXMLBeans);
        } catch (IOException e) {
            checksumsXmlBeansOldRevision = new HashMap<>();
        }
    }

    private static Set<Path> findXMLFiles(Path root_path) throws IOException {
        return Files.walk(root_path)
                .filter(p -> !p.toString().contains("target"))
                .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                .collect(Collectors.toSet());
    }


    @Override
    public void doExport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
        super.doExport(tmpPath, blackboard, suffix);

        Map<String, Integer> checksumsXMLBeansNewRevision = new HashMap<>(checksumsXmlBeansOldRevision);
        removedBeans.keySet().forEach(checksumsXMLBeansNewRevision::remove);
        differentBeans.forEach((name, t) -> checksumsXMLBeansNewRevision.put(name, t.getContent().hashCode()));
        addedBeans.forEach((name, t) -> checksumsXMLBeansNewRevision.put(name, t.getContent().hashCode()));

        try {
            Files.createDirectories(tmpPath);

            Files.writeString(tmpPath.resolve(Path.of("spring_xmlbeans_" + suffix)),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(checksumsXMLBeansNewRevision),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException ignored) {
            Log.log(SEVERE, "Failed to export checksums of SpringXMLBeans");
        }
    }

    @Override
    public void doChangeAnalysis(Blackboard<T> blackboard) {
        super.doChangeAnalysis(blackboard);

        ChangeAnalyzer.calculateChange(
                checksumsXmlBeansOldRevision,
                b -> b.getContent().hashCode(),
                xmlBeansNewRevision,
                sameBeans,
                differentBeans,
                addedBeans,
                removedBeans);

        TypeSolver typeSolver = blackboard.getTypeSolver();

        xmlBeansNewRevision.values().forEach(definition -> {
            Set<String> names = new HashSet<>(Set.of(definition.getId()));
            if (definition.getNames() != null)
                names.addAll(Set.of(definition.getNames()));

            SpringBean bean = new SpringBean(definition);

            for (String name : names) {
                beanStorage.addBeanByName(name, bean);
            }

            lookupTypeDeclaration(definition.getClassName(), typeSolver).ifPresent(c -> {
                beanStorage.addBeanByTypeDeclaration(c, bean);
                try {
                    for (ResolvedReferenceType ancestor :
                            c.getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList)) {
                        beanStorage.addBeanByType(ancestor, bean);
                    }
                } catch (RuntimeException ignored) {
                }
            });
        });

    }

    @Override
    public void doGraphCropping(Blackboard<T> blackboard) {
        super.doGraphCropping(blackboard);

        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        // remove removed nodes
        removedBeans.forEach((n, i) -> dependencyGraph.removeNode(n));

        // add new nodes
        addedBeans.keySet().forEach(dependencyGraph::addNode);
    }

    @Override
    protected Set<Set<String>> calculateImpactedBeans() {
        return removedBeans.keySet().stream().map(Set::of).collect(Collectors.toSet());
    }

    @Override
    protected BeanStorage<SpringBean> collectBeans(Collection<TypeDeclaration<?>> ts) {

        // Collect all types annotated with @Component
        SpringComponentIdentifierVisitor.identifyDependencies(ts, beanStorage);

        // Collect all methods annotated with @Bean
        // Because of "lite mode" beans can be declared in all classes, not only in those annotated with @Configuration
        SpringBeanMethodIdentifierVisitor.identifyDependencies(ts, beanStorage);

        return beanStorage;
    }

    @Override
    public void doDependencyAnalysis(Blackboard<T> blackboard) {
        super.doDependencyAnalysis(blackboard);

        springBeanDependencyCollector.setTypeSolver(blackboard.getTypeSolver());
        springBeanDependencyCollector.calculateDependencies(xmlBeansNewRevision.values(),
                blackboard.getDependencyGraphNewRevision());
    }

    @Override
    public void combineGraphs(Blackboard<T> blackboard) {
        super.combineGraphs(blackboard);

        blackboard.getCombinedGraph().setModificationByStatus(
                sameBeans.keySet(),
                differentBeans.keySet(),
                addedBeans.keySet(),
                removedBeans.keySet()
        );
    }
}
