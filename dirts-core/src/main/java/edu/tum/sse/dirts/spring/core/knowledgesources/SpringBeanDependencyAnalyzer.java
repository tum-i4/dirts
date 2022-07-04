package edu.tum.sse.dirts.spring.core.knowledgesources;/*
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
 *//*

package edu.tum.sse.edu.tum.sse.dirts.spring.core.knowledgesources;

import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import edu.tum.sse.edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.edu.tum.sse.dirts.spring.edu.tum.sse.dirts.analysis.SpringBeanDependencyCollector;
import edu.tum.sse.edu.tum.sse.dirts.spring.edu.tum.sse.dirts.analysis.bean.XMLBeanDefinition;

import java.util.Collection;
import java.util.function.Function;

*/
/**
 *
 *//*

public class SpringBeanDependencyAnalyzer extends KnowledgeSource {

    private final Blackboard blackboard;

    private final SpringBeanDependencyCollector dependencyCollector;
    private final Function<Blackboard, TypeSolver> getterTypeSolver;
    private final Function<Blackboard, Collection<XMLBeanDefinition>> getterDeclarations;

    public SpringBeanDependencyAnalyzer(Blackboard blackboard,
                                        SpringBeanDependencyCollector dependencyCollector,
                                        Function<Blackboard, TypeSolver> getterTypeSolver,
                                        Function<Blackboard, Collection<XMLBeanDefinition>> getterTypeDeclarations) {
        super(blackboard);
        this.blackboard = blackboard;
        this.dependencyCollector = dependencyCollector;
        this.getterTypeSolver = getterTypeSolver;
        this.getterDeclarations = getterTypeDeclarations;
    }

    @Override
    public BlackboardState updateBlackboard() {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraph();

        dependencyCollector.setTypeSolver(getterTypeSolver.apply(blackboard));
        dependencyCollector.setBeanStorage(blackboard.getSpringBeanBeanStorage());

        Collection<XMLBeanDefinition> XMLBeanDefinitions = getterDeclarations.apply(blackboard);
        dependencyCollector.calculateDependencies(XMLBeanDefinitions, dependencyGraph);

        
        return BlackboardState.SPRING_BEANS_DEPENDENCIES_SET;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == BlackboardState.DEPENDENCIES_SET;
    }
}
*/
