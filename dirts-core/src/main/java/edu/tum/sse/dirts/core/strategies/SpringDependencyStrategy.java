package edu.tum.sse.dirts.core.strategies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import edu.tum.sse.dirts.analysis.def.finders.TypeFinderVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.knowledgesources.CodeChangeAnalyzer;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.spring.analysis.SpringBeanDependencyCollector;
import edu.tum.sse.dirts.spring.analysis.SpringDependencyCollectorVisitor;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;
import edu.tum.sse.dirts.spring.analysis.identifiers.SpringXMLBeanIdentifier;
import edu.tum.sse.dirts.util.JavaParserUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.graph.EdgeType.DI_SPRING;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupTypeDeclaration;

/**
 * Contains tasks required by the dependency-analyzing extension for CDI
 */
public class SpringDependencyStrategy implements DependencyStrategy {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Integer>> typeRefXMLBeans = new TypeReference<>() {};

    private final SpringDependencyCollectorVisitor dependencyCollector;
    private final SpringBeanDependencyCollector springBeanDependencyCollector;

    private Map<String, Integer> checksumsXmlBeansOldRevision;
    private Map<String, XMLBeanDefinition> xmlBeansNewRevision;

    private final Map<String, XMLBeanDefinition> sameBeans = new HashMap<>();
    private final Map<String, XMLBeanDefinition> differentBeans = new HashMap<>();
    private final Map<String, XMLBeanDefinition> addedBeans = new HashMap<>();
    private final Map<String, Integer> removedBeans = new HashMap<>();
    private final Map<String, String> nameMapper = new HashMap<>();

    private final BeanStorage<SpringBean> springBeanBeanStorage = new BeanStorage<>();


    public SpringDependencyStrategy(SpringDependencyCollectorVisitor dependencyCollector,
                                    SpringBeanDependencyCollector springBeanDependencyCollector) {
        this.dependencyCollector = dependencyCollector;
        this.springBeanDependencyCollector = springBeanDependencyCollector;
    }

    @Override
    public void doImport(Path tmpPath, Blackboard blackboard, String suffix) {
        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();

        try {
            Set<Path> xmlPaths = findXMLFiles(rootPath.resolve(subPath));

            SpringXMLBeanIdentifier springXMLBeanIdentifier = new SpringXMLBeanIdentifier();
            xmlPaths.forEach(p -> springXMLBeanIdentifier.processXMLFile(rootPath, p));

            xmlBeansNewRevision = springXMLBeanIdentifier.getBeans();

        } catch (IOException e) {
            System.err.println("Failed to read xml files, that may contain spring beans");
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
    public void doExport(Path tmpPath, Blackboard blackboard, String suffix) {
        Map<String, Integer> checksumsXMLBeansNewRevision = new HashMap<>(checksumsXmlBeansOldRevision);
        nameMapper.forEach((o, n) -> {
            Integer tmp = checksumsXMLBeansNewRevision.remove(o);
            checksumsXMLBeansNewRevision.put(n, tmp);
        });
        removedBeans.keySet().forEach(checksumsXMLBeansNewRevision::remove);
        differentBeans.forEach((name, t) -> checksumsXMLBeansNewRevision.put(name, t.getContent().hashCode()));
        addedBeans.forEach((name, t) -> checksumsXMLBeansNewRevision.put(name, t.getContent().hashCode()));

        try {
            Files.createDirectories(tmpPath);

            Files.writeString(tmpPath.resolve(Path.of("spring_xmlbeans_" + suffix)),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(checksumsXMLBeansNewRevision), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException ignored) {
            System.err.println("Failed to export checksums of SpringXMLBeans");
        }
    }

    @Override
    public void doChangeAnalysis(Blackboard blackboard) {
        CodeChangeAnalyzer.calculateChange(
                checksumsXmlBeansOldRevision,
                b -> b.getContent().hashCode(),
                xmlBeansNewRevision,
                sameBeans,
                differentBeans,
                addedBeans,
                removedBeans,
                nameMapper);

        TypeSolver typeSolver = blackboard.getTypeSolver();

        xmlBeansNewRevision.values().forEach(definition -> {
            Set<String> names = new HashSet<>(Set.of(definition.getId()));
            if (definition.getNames() != null)
                names.addAll(Set.of(definition.getNames()));

            SpringBean bean = new SpringBean(definition);

            for (String name : names) {
                springBeanBeanStorage.addBeanByName(name, bean);
            }

            lookupTypeDeclaration(definition.getClassName(), typeSolver).ifPresent(c -> {
                springBeanBeanStorage.addBeanByTypeDeclaration(c, bean);
                try {
                    for (ResolvedReferenceType ancestor : c.getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList)) {
                        springBeanBeanStorage.addBeanByType(ancestor, bean);
                    }
                } catch (RuntimeException ignored) {
                }
            });
        });

    }

    @Override
    public void doGraphCropping(Blackboard blackboard) {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        // remove removed nodes
        removedBeans.forEach((n, i) -> dependencyGraph.removeNode(n));

        // rename nodes that have been renamed
        nameMapper.forEach(dependencyGraph::renameNode);

        // add new nodes
        addedBeans.keySet().forEach(dependencyGraph::addNode);

        // remove all edges of Type Spring
        dependencyGraph.removeAllEdgesByType(Set.of(DI_SPRING));
    }

    @Override
    public void doDependencyAnalysis(Blackboard blackboard) {
        Collection<CompilationUnit> compilationUnits = blackboard.getCompilationUnits();
        DependencyGraph dependencyGraphNewRevision = blackboard.getDependencyGraphNewRevision();

        List<TypeDeclaration<?>> typeDeclarations = new ArrayList<>();
        TypeFinderVisitor typeFinderVisitor = new TypeFinderVisitor();
        compilationUnits.forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));

        dependencyCollector.setBeanStorage(springBeanBeanStorage);
        dependencyCollector.calculateDependencies(typeDeclarations, dependencyGraphNewRevision);

        springBeanDependencyCollector.setTypeSolver(blackboard.getTypeSolver());
        springBeanDependencyCollector.calculateDependencies(xmlBeansNewRevision.values(), dependencyGraphNewRevision);
    }

    @Override
    public void combineGraphs(Blackboard blackboard) {
        blackboard.getCombinedGraph().setModificationByStatus(
                sameBeans.keySet(),
                differentBeans.keySet(),
                addedBeans.keySet(),
                removedBeans.keySet()
        );
    }
}
