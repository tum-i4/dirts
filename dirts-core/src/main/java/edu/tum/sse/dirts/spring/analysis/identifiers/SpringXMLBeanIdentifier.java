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
package edu.tum.sse.dirts.spring.analysis.identifiers;

import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Identifies bean definitions in xml files
 * <p>
 * Rationale:
 * R. Johnson et al., Spring Framework Reference Documentation, 5.0.0.M1. 2016.
 * "Part II. Core Technologies" -  Chapter "3 The IoC Container" - Subchapter "3.3 Bean overview"
 */
public class SpringXMLBeanIdentifier {

    //##################################################################################################################
    // Attributes

    private final Map<String, XMLBeanDefinition> beans;

    //##################################################################################################################
    // Constructors

    public SpringXMLBeanIdentifier() {
        beans = new HashMap<>();
    }

    //##################################################################################################################
    // Getters

    public Map<String, XMLBeanDefinition> getBeans() {
        return Collections.unmodifiableMap(beans);
    }

    //##################################################################################################################
    // Methods

    public void processXMLFile(Path rootPath, Path xmlPath) {
        File xmlFile = xmlPath.toFile();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new IgnoreErrorHandler());

            if (xmlFile.exists()) {

                Document document = builder.parse(xmlFile);
                Element root = document.getDocumentElement();

                processBeans(rootPath.relativize(xmlPath).toString(), root.getChildNodes());
            }

        } catch (IOException | SAXException ignored) {
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void processBeans(String path, NodeList beanNodes) {
        for (int i = 0; i < beanNodes.getLength(); i++) {
            Node item = beanNodes.item(i);
            if (item instanceof Element && ((Element) item).getTagName().equals("bean")) {
                Element bean = (Element) item;

                if (bean.getTagName().equals("bean")) {
                    String id = bean.getAttribute("id");
                    String className = bean.getAttribute("class");
                    String qualifier = path + "." + id + "_" + className;
                    String key = qualifier;

                    int custom_number = 0;
                    while (beans.containsKey(key)) {
                        custom_number++;
                        key = qualifier + "_" + custom_number;
                    }
                    beans.put(key, new XMLBeanDefinition(key, bean));
                }
            }
        }
    }

    //##################################################################################################################
    // Helper classes

    private static class IgnoreErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException e) throws SAXException {

        }

        @Override
        public void error(SAXParseException e) throws SAXException {

        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {

        }
    }
}
