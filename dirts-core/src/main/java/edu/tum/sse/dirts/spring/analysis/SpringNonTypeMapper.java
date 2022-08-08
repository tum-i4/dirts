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

public class SpringNonTypeMapper extends SpringMapper<BodyDeclaration<?>> {
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
