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
package edu.tum.sse.dirts.core.knowledgesources.graph_cropper;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.analysis.def.finders.TypeFinderVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.core.BlackboardState.NODES_CHANGES_SET;

/**
 * Prepares the graph before analyzing dependencies
 * Calculates which dependencies need to be reanalyzed
 * <p>
 * The concept used in this class can be found in J. Öqvist et al., Extraction-Based Regression Test Selection, 2016
 */
public class ClassLevelGraphCropper extends AbstractGraphCropper<TypeDeclaration<?>> {

    public ClassLevelGraphCropper(Blackboard<TypeDeclaration<?>> blackboard,
                                  FinderVisitor<Map<String, Node>, TypeDeclaration<?>> finderVisitor,
                                  Set<EdgeType> affectedEdges,
                                  Predicate<Node> nodesFilter) {
        super(blackboard, finderVisitor, affectedEdges, nodesFilter);
    }

    @Override
    public Collection<TypeDeclaration<?>> calculateImpactedTypeDeclarations(
            DependencyGraph dependencyGraph,
            Collection<CompilationUnit> compilationUnits,
            Map<String, Node> nodesAdded,
            Map<String, Integer> nodesRemoved,
            Map<String, Node> nodesDifferent,
            Map<String, Node> nodesSame
    ) {
        // Compute the set of CompilationUnits that require a recalculation of dependencies
        HashSet<CompilationUnit> impactedCompilationUnits = new HashSet<>();

        // Add all CompilationUnits from modified nodes
        impactedCompilationUnits.addAll(nodesDifferent.values().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        // Add all CompilationUnits from added nodes (these nodes cannot be present and need not be considered before)
        impactedCompilationUnits.addAll(nodesAdded.values().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        /* Add all CompilationUnits from packages that have at least one added or renamed CompilationUnit,
         * to deal with shadowing */

        // Consider all added nodes that correspond to a CompilationUnit
        Set<String> affectedPackagesNodes = new HashSet<>();
        affectedPackagesNodes.addAll(nodesAdded.values().stream()
                .filter(n -> n instanceof CompilationUnit)
                .map(n -> ((CompilationUnit) n).getPackageDeclaration())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(NodeWithName::getNameAsString)
                .collect(Collectors.toSet()));

        // Add all CompilationUnits that are declared in an affected package
        impactedCompilationUnits.addAll(compilationUnits.stream()
                .filter(cu -> cu.getPackageDeclaration()
                        .filter(declaration -> affectedPackagesNodes.contains(declaration.getNameAsString()))
                        .isPresent())
                .collect(Collectors.toSet()));

        List<TypeDeclaration<?>> typeDeclarations = new ArrayList<>();
        TypeFinderVisitor typeFinderVisitor = new TypeFinderVisitor();
        impactedCompilationUnits.forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));

        return typeDeclarations;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == NODES_CHANGES_SET;
    }
}
