package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.resolution.declarations.*;
import edu.tum.sse.dirts.analysis.di.NameMapper;
import edu.tum.sse.dirts.cdi.util.CDIBean;
import edu.tum.sse.dirts.util.alternatives.QuadAlternative;

import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to map CDI beans to the names of corresponding method level nodes
 */
public class CDIMethodLevelMapper extends CDIMapper<BodyDeclaration<?>> implements NameMapper<CDIBean> {
    @Override
    public Set<String> mapToString(CDIBean cdiBean) {
        QuadAlternative<ResolvedReferenceTypeDeclaration, Set<ResolvedMethodDeclaration>, ResolvedFieldDeclaration, ResolvedValueDeclaration> source
                = cdiBean.getSource();

        Set<String> ret = new HashSet<>();

        if (source.isFirstOption()) {
            ResolvedReferenceTypeDeclaration typeDeclaration = source.getAsFirstOption();

            String typeName = lookup(typeDeclaration);
            /*if (alternatives.contains(typeName)) {
                String toNode = CDIUtil.lookupXMlAlternativeName(lookup(typeDeclaration));
                dependencyGraph.addEdge(node, toNode, EdgeType.DI_CDI);
            }
*/
            for (ResolvedConstructorDeclaration constructor : typeDeclaration.getConstructors()) {
                String toNode = lookup(constructor);
                ret.add(toNode);
            }
        } else if (source.isSecondOption()) {
            Set<ResolvedMethodDeclaration> resolvedMethodDeclarations = source.getAsSecondOption();
            for (ResolvedMethodDeclaration resolvedMethodDeclaration : resolvedMethodDeclarations) {
                String toNode = lookup(resolvedMethodDeclaration);
                ret.add(toNode);
            }
        } else if (source.isThirdOption()) {
            ResolvedFieldDeclaration resolvedFieldDeclaration = source.getAsThirdOption();
            String toNode = lookup(resolvedFieldDeclaration.declaringType(), resolvedFieldDeclaration);
            ret.add(toNode);

        } else {
            ResolvedValueDeclaration resolvedValueDeclaration = source.getAsFourthOption();
            if (resolvedValueDeclaration.isField()) {
                String toNode = lookup(resolvedValueDeclaration.asField().declaringType(), resolvedValueDeclaration.asField());
                ret.add(toNode);
            }
        }

        return ret;
    }
}
