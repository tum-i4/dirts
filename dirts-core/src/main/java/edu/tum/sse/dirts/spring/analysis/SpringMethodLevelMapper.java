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
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;
import edu.tum.sse.dirts.spring.util.SpringNames;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;

import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to map Spring beans to the names of corresponding method level nodes
 */
public class SpringMethodLevelMapper extends SpringMapper<BodyDeclaration<?>> {
    @Override
    public Set<String> mapToString(SpringBean springBean) {

        TriAlternative<XMLBeanDefinition, ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration> dependsOn =
                springBean.getDefinition();

        Set<String> ret = new HashSet<>();

        if (dependsOn.isFirstOption()) {
            XMLBeanDefinition referencedBean = dependsOn.getAsFirstOption();
            String toNode = SpringNames.lookup(referencedBean);
            ret.add(toNode);
        } else if (dependsOn.isSecondOption()) {
            ResolvedMethodDeclaration method = dependsOn.getAsSecondOption();
            String toNode = lookup(method);
            ret.add(toNode);
        } else {
            ResolvedReferenceTypeDeclaration typeDeclaration = dependsOn.getAsThirdOption();

            for (ResolvedConstructorDeclaration constructor : typeDeclaration.getConstructors()) {
                String toNode = lookup(constructor);
                ret.add(toNode);
            }
        }
        return ret;
    }
}
