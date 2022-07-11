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
package edu.tum.sse.dirts.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.ModificationGraph;
import org.apache.maven.surefire.api.testset.TestFilter;

import java.nio.file.Path;
import java.util.*;

/**
 * Used to store all available knowledge
 */
public class Blackboard<T extends BodyDeclaration<?>> {

    /*
     * Central class of the blackboard pattern
     *
     * The main logic for selecting tests is contained in KnowledgeSources
     * Some DependencyStrategies may be used as extensions
     */

    //##################################################################################################################
    // Phantom Data to allow compiletime checks
    private T phantom;

    //##################################################################################################################
    // Attributes

    private BlackboardState state;
    private final List<KnowledgeSource<T>> knowledgeSources;
    private final List<DependencyStrategy<T>> dependencyStrategies;

    // #################################################################################################################
    // ## Content of blackboard, actual knowledge

    private final Path rootPath;
    private final Path subPath;

    private TestFilter<String, String> testFilter;

    private Map<String, Integer> checksumsNodes;
    private Map<String, String> compilationUnitMapping;

    private CombinedTypeSolver typeSolver;

    private Collection<CompilationUnit> compilationUnits;

    private Map<String, Node> allNodes;
    private Map<String, Node> nodesSame;
    private Map<String, Node> nodesDifferent;
    private Map<String, Node> nodesAdded;
    private Map<String, Integer> nodesRemoved;

    private Map<String, String> nameMapperNodes;

    private Collection<String> tests;

    private Collection<TypeDeclaration<?>> impactedTypes;

    private DependencyGraph graphOldRevision;
    private DependencyGraph graphNewRevision;


    private ModificationGraph modificationGraph;

    //##################################################################################################################
    // Constructors

    public Blackboard(Path rootPath, Path subPath) {
        this.rootPath = rootPath;
        this.subPath = subPath;

        this.state = BlackboardState.CLEAN;
        knowledgeSources = new ArrayList<>();
        dependencyStrategies = new ArrayList<>();
    }

    //##################################################################################################################
    // Getters and Setters

    public BlackboardState getState() {
        return state;
    }

    public void setState(BlackboardState state) {
        this.state = state;
    }

    public List<KnowledgeSource<T>> getKnowledgeSources() {
        return Collections.unmodifiableList(knowledgeSources);
    }

    public void addKnowledgeSource(KnowledgeSource<T> knowledgeSource) {
        if (knowledgeSource != null)
            knowledgeSources.add(knowledgeSource);
    }

    public List<DependencyStrategy<T>> getDependencyStrategies() {
        return Collections.unmodifiableList(dependencyStrategies);
    }

    public void addDependencyStrategy(DependencyStrategy<T> dependencyStrategy) {
        dependencyStrategies.add(dependencyStrategy);
    }

    // _________________________________________________________________________________________________________________

    public Path getRootPath() {
        return rootPath;
    }

    public Path getSubPath() {
        return subPath;
    }

    // _________________________________________________________________________________________________________________

    public TestFilter<String, String> getTestFilter() {
        return testFilter;
    }

    public void setTestFilter(TestFilter<String, String> testFilter) {
        this.testFilter = testFilter;
    }

    // _________________________________________________________________________________________________________________

    public Map<String, Integer> getChecksumsNodes() {
        return Collections.unmodifiableMap(checksumsNodes);
    }

    public void setChecksumsNodes(Map<String, Integer> checksumsNodes) {
        this.checksumsNodes = checksumsNodes;
    }

    public Map<String, String> getCompilationUnitMapping() {
        return Collections.unmodifiableMap(compilationUnitMapping);
    }

    public void setCompilationUnitMapping(Map<String, String> compilationUnitMapping) {
        this.compilationUnitMapping = compilationUnitMapping;
    }

    // _________________________________________________________________________________________________________________

    public CombinedTypeSolver getTypeSolver() {
        return typeSolver;
    }

    public void setTypeSolver(CombinedTypeSolver typeSolver) {
        this.typeSolver = typeSolver;
    }

    // _________________________________________________________________________________________________________________

    public void setCompilationUnits(Collection<CompilationUnit> compilationUnits) {
        this.compilationUnits = compilationUnits;
    }

    public Collection<CompilationUnit> getCompilationUnits() {
        return Collections.unmodifiableCollection(compilationUnits);
    }

    // _________________________________________________________________________________________________________________

    public void setChangesNodes(Map<String, Node> nodesSame,
                                Map<String, Node> nodesDifferent,
                                Map<String, Node> nodesAdded,
                                Map<String, Integer> nodesRemoved) {
        this.nodesSame = nodesSame;
        this.nodesDifferent = nodesDifferent;
        this.nodesAdded = nodesAdded;
        this.nodesRemoved = nodesRemoved;
        this.allNodes = new HashMap<>();
        allNodes.putAll(nodesSame);
        allNodes.putAll(nodesAdded);
        allNodes.putAll(nodesDifferent);
    }

    public Map<String, Node> getNodesSame() {
        return Collections.unmodifiableMap(nodesSame);
    }

    public Map<String, Node> getNodesDifferent() {
        return Collections.unmodifiableMap(nodesDifferent);
    }

    public Map<String, Node> getNodesAdded() {
        return Collections.unmodifiableMap(nodesAdded);
    }

    public Map<String, Integer> getNodesRemoved() {
        return Collections.unmodifiableMap(nodesRemoved);
    }

    public Map<String, Node> getAllNodes() {
        return Collections.unmodifiableMap(allNodes);
    }

    // _________________________________________________________________________________________________________________

    public Map<String, String> getNameMapperNodes() {
        return Collections.unmodifiableMap(nameMapperNodes);
    }

    public void setNameMapperNodes(Map<String, String> nameMapperNodes) {
        this.nameMapperNodes = nameMapperNodes;
    }

    // _________________________________________________________________________________________________________________

    public void setTests(Collection<String> tests) {
        this.tests = tests;
    }

    public Collection<String> getTests() {
        return tests;
    }

    // _________________________________________________________________________________________________________________

    public Collection<TypeDeclaration<?>> getImpactedTypes() {
        return impactedTypes;
    }

    public void setImpactedTypes(Collection<TypeDeclaration<?>> impactedTypes) {
        this.impactedTypes = impactedTypes;
    }

    // _________________________________________________________________________________________________________________

    public DependencyGraph getDependencyGraphOldRevision() {
        return this.graphOldRevision;
    }

    public void setGraphOldRevision(DependencyGraph graphOldRevision) {
        this.graphOldRevision = graphOldRevision;
    }

    public DependencyGraph getDependencyGraphNewRevision() {
        return this.graphNewRevision;
    }

    public void setGraphNewRevision(DependencyGraph graphNewRevision) {
        this.graphNewRevision = graphNewRevision;
    }

    // _________________________________________________________________________________________________________________

    public ModificationGraph getCombinedGraph() {
        return modificationGraph;
    }

    public void setCombinedGraph(ModificationGraph modificationGraph) {
        this.modificationGraph = modificationGraph;
    }

}
