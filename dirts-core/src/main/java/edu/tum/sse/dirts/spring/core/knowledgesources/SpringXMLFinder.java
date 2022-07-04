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

import edu.tum.sse.core.vcs.client.git.GitClient;
import edu.tum.sse.edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.edu.tum.sse.dirts.spring.edu.tum.sse.dirts.analysis.identifiers.SpringXMLBeanIdentifier;
import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

*/
/**
 * Finds xml bean definitsions
 *//*

public class SpringXMLFinder extends KnowledgeSource {

    //##################################################################################################################
    // Attributes

    private final Function<Blackboard, GitClient> getterGitClient;
    private final BiConsumer<Blackboard, Map<String, Element>> setterXmlBeans;

    //##################################################################################################################
    // Constructors

    public SpringXMLFinder(Blackboard SpringBlackboard,
                           Function<Blackboard, GitClient> getterGitClient,
                           BiConsumer<Blackboard, Map<String, Element>> setterXmlBeans) {
        super(SpringBlackboard);
        this.getterGitClient = getterGitClient;
        this.setterXmlBeans = setterXmlBeans;
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {
        GitClient gitClient = getterGitClient.apply(blackboard);

        try {
            Path rootPath = Path.of(gitClient.getRepository().getPath());
            Path subPath = blackboard.getSubPath();
            Set<Path> xmlPaths = findXMLFiles(rootPath.resolve(subPath));

            SpringXMLBeanIdentifier springXMLBeanIdentifier = new SpringXMLBeanIdentifier();
            xmlPaths.forEach(p -> springXMLBeanIdentifier.processXMLFile(rootPath, p));

            Map<String, Element> beans = springXMLBeanIdentifier.getBeans();

            setterXmlBeans.accept(blackboard, beans);

        } catch (IOException e) {
            System.err.println("Failed to read xml files, that may contain spring beans");
        }

        
        return BlackboardState.TYPES_SET;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == BlackboardState.COMPILATION_UNITS_SET;
    }

    public static Set<Path> findXMLFiles(Path root_path) throws IOException {
        return Files.walk(root_path)
                .filter(p -> !p.toString().contains("target"))
                .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                .collect(Collectors.toSet());
    }
}*/
