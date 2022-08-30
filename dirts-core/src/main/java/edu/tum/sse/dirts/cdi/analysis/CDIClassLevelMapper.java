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
package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.*;
import edu.tum.sse.dirts.analysis.di.NameMapper;
import edu.tum.sse.dirts.cdi.util.CDIBean;
import edu.tum.sse.dirts.util.alternatives.QuadAlternative;

import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to map CDI beans to the names of corresponding class level nodes
 */
public class CDIClassLevelMapper extends CDIMapper<TypeDeclaration<?>> implements NameMapper<CDIBean> {

    @Override
    public Set<String> mapToString(CDIBean cdiBean) {
        QuadAlternative<ResolvedReferenceTypeDeclaration,
                Set<ResolvedMethodDeclaration>,
                ResolvedFieldDeclaration,
                ResolvedValueDeclaration> source = cdiBean.getSource();

        Set<String> ret = new HashSet<>();

        if (source.isFirstOption()) {
            ResolvedReferenceTypeDeclaration typeDeclaration = source.getAsFirstOption();
            String toNode = lookup(typeDeclaration);
            ret.add(toNode);
        } else if (source.isSecondOption()) {
            Set<ResolvedMethodDeclaration> resolvedMethodDeclarations = source.getAsSecondOption();
            for (ResolvedMethodDeclaration resolvedMethodDeclaration : resolvedMethodDeclarations) {
                String toNode = lookup(resolvedMethodDeclaration.declaringType());
                ret.add(toNode);
            }
        } else if (source.isThirdOption()) {
            ResolvedFieldDeclaration resolvedFieldDeclaration = source.getAsThirdOption();
            String toNode = lookup(resolvedFieldDeclaration.declaringType());
            ret.add(toNode);
        } else {
            ResolvedValueDeclaration resolvedValueDeclaration = source.getAsFourthOption();
            if (resolvedValueDeclaration.isField()) {
                String toNode = lookup(resolvedValueDeclaration.asField().declaringType());
                ret.add(toNode);
            }
        }

        return ret;
    }
}
