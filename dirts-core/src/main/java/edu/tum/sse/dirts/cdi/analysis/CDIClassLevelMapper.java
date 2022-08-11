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
