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

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.analysis.di.NameMapper;
import edu.tum.sse.dirts.spring.analysis.bean.SpringBean;
import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;
import edu.tum.sse.dirts.spring.util.SpringNames;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;

import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to map Spring beans to the names of corresponding class level nodes
 */
public class SpringClassLevelMapper extends SpringMapper<TypeDeclaration<?>> {
    @Override
    public Set<String> mapToString(SpringBean springBean) {
            TriAlternative<XMLBeanDefinition, ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration> dependsOn =
                    springBean.getDefinition();

            String toNode;
            if (dependsOn.isFirstOption()) {
                XMLBeanDefinition referencedBean = dependsOn.getAsFirstOption();
                toNode = SpringNames.lookup(referencedBean);
            } else if (dependsOn.isSecondOption()) {
                ResolvedMethodDeclaration method = dependsOn.getAsSecondOption();
                toNode = lookup(method.declaringType());
            } else {
                ResolvedReferenceTypeDeclaration typeDeclaration = dependsOn.getAsThirdOption();
                toNode = lookup(typeDeclaration);
            }
            return  Set.of(toNode);
    }
}
