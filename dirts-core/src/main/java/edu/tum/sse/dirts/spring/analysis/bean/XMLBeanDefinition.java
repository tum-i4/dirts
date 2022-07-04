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
package edu.tum.sse.dirts.spring.analysis.bean;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a bean written in xml
 * <p>
 * Rationale:
 * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
 * "Part II. Core Technologies" -  Chapter "3 The IoC Container"
 * Subchapter "3.3  Bean Overview"
 */
public class XMLBeanDefinition {

    //##################################################################################################################
    // Attributes

    private final String nodeName;

    /**
     * XML: id attribute
     */
    private String id;

    /**
     * XML: name attribute
     */
    private String[] names;

    /**
     * XML: class attribute
     */
    private String className;

    /**
     * XML: factory-method attribute
     */
    private String factoryMethod;

    private final Set<String> dependsOnBeans = new HashSet<>();

    private final String content;

    //##################################################################################################################
    // Constructors

    public XMLBeanDefinition(String nodeName, Element element) {

        this.nodeName = nodeName;
        this.content = printElement(element);

        String id = element.getAttribute("id");
        String className = element.getAttribute("class");

        String factory_bean = element.getAttribute("factory-bean");
        String factory_method = element.getAttribute("factory-method");

        String name = element.getAttribute("name");

        // for constructor or setter injection through ref
        NodeList refs = element.getElementsByTagName("ref");
        Pattern refRegex = Pattern.compile("ref=\"(.*)\"");

        for (int i = refs.getLength() - 1; i >= 0; i--) {
            Node item = refs.item(i);
            if (item instanceof Element) {
                String beanName = ((Element) item).getAttribute("bean");
                if (!beanName.equals("")) {
                    addBeanDependency(beanName);
                }
            }
        }

        Matcher refMatcher = refRegex.matcher(element.getTextContent());
        while (refMatcher.find()) {
            String beanName = refMatcher.group(1);
            if (!beanName.equals(""))
                addBeanDependency(beanName);
        }

        setId(id);
        if (!factory_bean.equals(""))
            addBeanDependency(factory_bean);
        setFactoryMethod(factory_method);
        setClassName(className);

        String[] split = name.equals("") ? null : name.split("[ ,;]");
        setNames(split);
    }

    //##################################################################################################################
    // Setters

    public void setId(String id) {
        this.id = id;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public void setNames(String[] names) {
        this.names = names;
    }

    public void addBeanDependency(String name) {
        dependsOnBeans.add(name);
    }

    //##################################################################################################################
    // Getters

    public String getNodeName() {
        return nodeName;
    }

    public String getClassName() {
        return className;
    }

    public String getId() {
        return id;
    }

    public String[] getNames() {
        return names;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

    public Set<String> getDependsOnBeans() {
        return dependsOnBeans;
    }

    public String getContent() {
        return content;
    }

    //##################################################################################################################
    // Auxiliary methods

    private String printElement(Element element) {
        // Code from stackoverflow
        // Source: https://stackoverflow.com/a/19701727
        DOMImplementationLS lsImpl = (DOMImplementationLS) element.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer serializer = lsImpl.createLSSerializer();
        serializer.getDomConfig().setParameter("xml-declaration", false); //by default its true, so set it to false to get String without xml-declaration
        return serializer.writeToString(element);
    }

}
