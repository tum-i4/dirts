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

import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import edu.tum.sse.edu.tum.sse.dirts.edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.edu.tum.sse.dirts.graph.ModificationType;
import edu.tum.sse.edu.tum.sse.dirts.spring.edu.tum.sse.dirts.analysis.bean.SpringBean;
import edu.tum.sse.edu.tum.sse.dirts.spring.edu.tum.sse.dirts.analysis.bean.XMLBeanDefinition;
import edu.tum.sse.edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.edu.tum.sse.dirts.util.QuintConsumer;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import java.util.*;
import java.util.stream.Collectors;

import static edu.tum.sse.edu.tum.sse.dirts.spring.util.SpringNames.lookup;
import static edu.tum.sse.edu.tum.sse.dirts.util.naming_scheme.Names.lookupTypeDeclaration;

*/
/**
 * Partitions xml beans into four categories: same, added, removed, changed
 *//*

public class SpringXMLChangeAnalyzer extends KnowledgeSource {

    //##################################################################################################################
    // Attributes

    private final Blackboard blackboard;
    private final QuintConsumer<Blackboard, Collection<XMLBeanDefinition>, Collection<XMLBeanDefinition>, Collection<XMLBeanDefinition>, Collection<XMLBeanDefinition>> setter;

    //##################################################################################################################
    // Constructors

    public SpringXMLChangeAnalyzer(Blackboard blackboard,
                                   QuintConsumer<Blackboard, Collection<XMLBeanDefinition>, Collection<XMLBeanDefinition>, Collection<XMLBeanDefinition>, Collection<XMLBeanDefinition>> setter) {
        super(blackboard);
        this.blackboard = blackboard;
        this.setter = setter;
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {
        Collection<XMLBeanDefinition> sameBeans = new ArrayList<>();
        Collection<XMLBeanDefinition> differentBeans = new ArrayList<>();
        Collection<XMLBeanDefinition> added = new ArrayList<>();
        Collection<XMLBeanDefinition> removed = new ArrayList<>();

        calculateChange(sameBeans, differentBeans, added, removed);
        registerChanges(sameBeans, differentBeans, added, removed);
        registerBeans(sameBeans, differentBeans, added, removed);

        setter.apply(this.blackboard, sameBeans, differentBeans, added, removed);

        
        return BlackboardState.CHANGES_SET;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == BlackboardState.CLASSES_SET;
    }

    private void registerBeans(Collection<XMLBeanDefinition> sameBeans, Collection<XMLBeanDefinition> differentBeans, Collection<XMLBeanDefinition> added, Collection<XMLBeanDefinition> removed) {
        BeanStorage<SpringBean> beanStorage = blackboard.getSpringBeanBeanStorage();

        addToBeanStorage(sameBeans, beanStorage, blackboard.getTypeSolverNewRevision());

        addToBeanStorage(differentBeans, beanStorage, blackboard.getTypeSolverNewRevision());

        addToBeanStorage(added, beanStorage, blackboard.getTypeSolverNewRevision());

        addToBeanStorage(removed, beanStorage, blackboard.getTypeSolverOldRevision());
    }

    private void addToBeanStorage(Collection<XMLBeanDefinition> sameBeans, BeanStorage<SpringBean> beanStorage, TypeSolver typeSolver) {
        sameBeans.forEach(definition -> {
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
                    for (ResolvedReferenceType ancestor : c.getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList)) {
                        beanStorage.addBeanByType(ancestor, bean);
                    }
                } catch (UnsolvedSymbolException ignored) {
                }
            });
        });
    }

    protected void registerChanges(Collection<XMLBeanDefinition> sameBeans, Collection<XMLBeanDefinition> differentBeans, Collection<XMLBeanDefinition> added, Collection<XMLBeanDefinition> removed) {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraph();

        sameBeans.forEach(definition -> {
            dependencyGraph.addNode(lookup(definition), ModificationType.NOT_MODIFIED);
        });

        differentBeans.forEach(definition -> {
            dependencyGraph.addNode(lookup(definition), ModificationType.MODIFIED);
        });

        added.forEach(definition -> {
            dependencyGraph.addNode(lookup(definition), ModificationType.ADDED);
        });

        removed.forEach(definition -> {
            dependencyGraph.addNode(lookup(definition), ModificationType.REMOVED);
        });
    }

    private void calculateChange(Collection<XMLBeanDefinition> beansSame,
                                 Collection<XMLBeanDefinition> beansDifferent,
                                 Collection<XMLBeanDefinition> beansAdded,
                                 Collection<XMLBeanDefinition> beansRemoved) {
        Map<String, Element> elementsOld = new HashMap<>(blackboard.getXmlBeansOldRevision());
        Map<String, Element> elementsNew = new HashMap<>(blackboard.getXmlBeansNewRevision());

        Map<String, Element> sameBeansMap = new HashMap<>();
        Map<String, Element> differentBeansMap = new HashMap<>();

        // check for same names
        for (Map.Entry<String, Element> elementNew : elementsNew.entrySet()) {
            String name = elementNew.getKey();
            Element matchingOldElement = elementsOld.getOrDefault(name, null);
            if (matchingOldElement != null) {
                if (isSameBeanDefinition(elementNew.getValue(), matchingOldElement)) {
                    // same Name, same Bean
                    sameBeansMap.put(name, elementNew.getValue());
                } else {
                    // same Name, different Bean
                    differentBeansMap.put(name, elementNew.getValue());
                }

                // remove old class from active set
                elementsOld.remove(name);
            }
        }
        // remove new ts from active set
        sameBeansMap.keySet().forEach(elementsNew::remove);
        differentBeansMap.keySet().forEach(elementsNew::remove);

        // same Code, different Name but able to clearly identify source
        for (Map.Entry<String, Element> elementNew : elementsNew.entrySet()) {
            String newName = elementNew.getKey();

            */
/*
            We need to ensure there is only one t with this bean in elementsNew and elementsOld
            Only then we can identify the source without ambiguity
             *//*


            Set<String> matchingOldT = elementsOld.entrySet().stream()
                    .filter(elementOld -> isSameBeanDefinition(elementOld.getValue(), elementNew.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            Set<String> matchingNewT = elementsNew.entrySet().stream()
                    .filter(elementNewOther -> isSameBeanDefinition(elementNewOther.getValue(), elementNew.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            if (matchingOldT.size() == 1 && matchingNewT.size() == 1) {
                sameBeansMap.put(newName, elementNew.getValue());

                // remove old class from active set
                matchingOldT.forEach(elementsOld::remove);
            }
        }
        // remove new elements from active set
        sameBeansMap.keySet().forEach(elementsNew::remove);

        // remaining in elementsNew and elementsOld: elements that have been renamed and changed without a clear source
        // remaining in elementsNew: added elements
        // remaining in elementsOld: removed elements
        beansSame.addAll(sameBeansMap.values().stream().map(XMLBeanDefinition::new).collect(Collectors.toSet()));
        beansDifferent.addAll(differentBeansMap.values().stream().map(XMLBeanDefinition::new).collect(Collectors.toSet()));
        beansAdded.addAll(elementsNew.values().stream().map(XMLBeanDefinition::new).collect(Collectors.toSet()));
        beansRemoved.addAll(elementsOld.values().stream().map(XMLBeanDefinition::new).collect(Collectors.toSet()));
    }

    private boolean isSameBeanDefinition(Element e1, Element e2) {
        // TODO: find something better

        String e1String = printElement(e1);
        String e2String = printElement(e2);

        return e1String.equals(e2String);
    }

    private String printElement(Element element) {
        // Code from stackoverflow
        // Source: https://stackoverflow.com/a/19701727
        DOMImplementationLS lsImpl = (DOMImplementationLS) element.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer serializer = lsImpl.createLSSerializer();
        serializer.getDomConfig().setParameter("xml-declaration", false); //by default its true, so set it to false to get String without xml-declaration
        return serializer.writeToString(element);
    }
}
*/
