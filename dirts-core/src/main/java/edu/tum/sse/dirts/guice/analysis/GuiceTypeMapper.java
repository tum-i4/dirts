package edu.tum.sse.dirts.guice.analysis;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

public class GuiceTypeMapper extends GuiceMapper<TypeDeclaration<?>> {
    @Override
    public Set<String> mapToString(GuiceBinding guiceBinding) {
        TriAlternative<ResolvedMethodLikeDeclaration,
                ResolvedReferenceTypeDeclaration,
                Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>>> source = guiceBinding.getSource();


        Set<String> ret = new HashSet<>();

        String toNode;
        if (source.isFirstOption()) {
            ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration = source.getAsFirstOption();
            toNode = lookup(resolvedMethodLikeDeclaration.declaringType());
            ret.add(toNode);
        } else if (source.isSecondOption()) {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = source.getAsSecondOption();
            toNode = lookup(resolvedReferenceTypeDeclaration);
            ret.add(toNode);
        } else {
            Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>> pair = source.getAsThirdOption();
            ResolvedMethodDeclaration methodWhereBound = pair.getFirst();
            Set<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations = pair.getSecond();

            // Add edge to the class containing the method where the binding was created
            if (methodWhereBound != null) {
                String toMethodNode;
                toMethodNode = lookup(methodWhereBound.declaringType());
                ret.add(toMethodNode);
            }
            for (ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypeDeclarations) {
                toNode = lookup(resolvedReferenceTypeDeclaration);
                ret.add(toNode);
            }
        }
        return ret;
    }
}
