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

public class SpringTypeMapper extends SpringMapper<TypeDeclaration<?>> {
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
