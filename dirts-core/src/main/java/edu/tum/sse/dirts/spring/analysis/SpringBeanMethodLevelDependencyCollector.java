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
package edu.tum.sse.dirts.spring.analysis;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;
import edu.tum.sse.dirts.spring.util.SpringNames;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;

import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Collects dependencies of xml beans in Spring for method level nodes
 */
public class SpringBeanMethodLevelDependencyCollector extends SpringBeanDependencyCollector<BodyDeclaration<?>> {

    @Override
    protected void processBeanDefinition(XMLBeanDefinition XMLBeanDefinition,
                                         DependencyGraph dependencyGraph,
                                         Set<SpringBean> beanDependencies) {
        String node = SpringNames.lookup(XMLBeanDefinition);

        for (SpringBean beanDependency : beanDependencies) {
            TriAlternative<XMLBeanDefinition, ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration> definition =
                    beanDependency.getDefinition();
            if (definition.isFirstOption()) {
                XMLBeanDefinition referencedBean = definition.getAsFirstOption();
                String toNode = SpringNames.lookup(referencedBean);
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_SPRING);
            } else if (definition.isSecondOption()) {
                ResolvedMethodDeclaration method = definition.getAsSecondOption();
                String toNode = lookup(method);
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_SPRING);
            } else {
                ResolvedReferenceTypeDeclaration typeDeclaration = definition.getAsThirdOption();
                for (ResolvedConstructorDeclaration constructor : typeDeclaration.getConstructors()) {
                    String toNode = lookup(constructor);
                    dependencyGraph.addEdge(node, toNode, EdgeType.DI_SPRING);
                }
            }
        }
    }
}
