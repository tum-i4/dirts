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
package edu.tum.sse.dirts.spring.analysis;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupMethods;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookupTypeDeclaration;

/**
 * Collects dependencies from beans to beans/code entities
 */
public abstract class SpringBeanDependencyCollector {

    //##################################################################################################################
    // Attributes

    protected TypeSolver typeSolver;

    protected BeanStorage<SpringBean> beanStorage;

    //##################################################################################################################
    // Setters

    public void setTypeSolver(TypeSolver typeSolver) {
        this.typeSolver = typeSolver;
    }

    public void setBeanStorage(BeanStorage<SpringBean> beanStorage) {
        this.beanStorage = beanStorage;
    }

    //##################################################################################################################
    // Abstract methods

    protected abstract void processBeanDefinition(XMLBeanDefinition XMLBeanDefinition,
                                                  DependencyGraph dependencyGraph,
                                                  Set<SpringBean> beanDependencies);

    //##################################################################################################################
    // Methods

    public void calculateDependencies(Collection<XMLBeanDefinition> XMLBeanDefinitions,
                                      DependencyGraph dependencyGraph) {
        for (XMLBeanDefinition XMLBeanDefinition : XMLBeanDefinitions) {
            Set<SpringBean> beanDependencies = getBeanDependencies(XMLBeanDefinition);
            processBeanDefinition(XMLBeanDefinition, dependencyGraph, beanDependencies);
        }
    }

    //##################################################################################################################
    // Auxiliary methods used by subclasses

    private Set<SpringBean> getBeanDependencies(XMLBeanDefinition XMLBeanDefinition) {
        Set<SpringBean> ret =
                new HashSet<>();

        String factoryMethod = XMLBeanDefinition.getFactoryMethod();
        String className = XMLBeanDefinition.getClassName();

        Set<String> dependsOnBeans = XMLBeanDefinition.getDependsOnBeans();

        if (!className.equals("")) {
            if (factoryMethod.equals("")) {
                // edges to class
                Optional<ResolvedReferenceTypeDeclaration> maybeResolvedReferenceTypeDeclaration =
                        lookupTypeDeclaration(className, typeSolver);
                maybeResolvedReferenceTypeDeclaration.ifPresent(r -> ret.add(new SpringBean(r)));

            } else {
                // edges to method
                ret.addAll(lookupMethods(className, factoryMethod, typeSolver).stream()
                        .map(SpringBean::new)
                        .collect(Collectors.toSet()));
            }
        }

        // edges to other beans
        for (String dependsOnBean : dependsOnBeans) {
            Set<SpringBean> beans = beanStorage.getBeansForName(dependsOnBean);
            ret.addAll(beans);
        }

        return ret;
    }
}
