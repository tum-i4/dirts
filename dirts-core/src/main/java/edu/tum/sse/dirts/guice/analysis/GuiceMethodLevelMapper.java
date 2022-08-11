package edu.tum.sse.dirts.guice.analysis;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to map Guice beans to the names of corresponding method level nodes
 */
public class GuiceMethodLevelMapper extends GuiceMapper<BodyDeclaration<?>> {
    @Override
    public Set<String> mapToString(GuiceBinding guiceBinding) {
        TriAlternative<ResolvedMethodLikeDeclaration,
                ResolvedReferenceTypeDeclaration,
                Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>>> source =
                guiceBinding.getSource();

        Set<String> ret = new HashSet<>();

        String toNode;
        if (source.isFirstOption()) {
            ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration = source.getAsFirstOption();
            toNode = lookup(resolvedMethodLikeDeclaration);
            ret.add(toNode);
        } else if (source.isSecondOption()) {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = source.getAsSecondOption();

            for (ResolvedConstructorDeclaration constructor : resolvedReferenceTypeDeclaration.getConstructors()) {
                toNode = lookup(constructor);
                ret.add(toNode);
            }
        } else {
            Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>> pair = source.getAsThirdOption();
            ResolvedMethodDeclaration methodWhereBound = pair.getFirst();
            Set<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations = pair.getSecond();

            // Add edge to the method where the binding was created
            String toMethodNode = lookup(methodWhereBound);
            ret.add(toMethodNode);

            for (ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypeDeclarations) {
                for (ResolvedConstructorDeclaration constructor : resolvedReferenceTypeDeclaration.getConstructors()) {
                    toNode = lookup(constructor);
                    ret.add(toNode);
                }
            }
        }
        return ret;
    }
}
