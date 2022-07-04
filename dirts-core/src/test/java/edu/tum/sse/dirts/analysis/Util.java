package edu.tum.sse.dirts.analysis;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import org.assertj.core.api.Condition;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Util {

    public static Collection<String> methodsToSignatureString(Collection<ResolvedMethodLikeDeclaration> methods) {
        return methods.stream().map(ResolvedMethodLikeDeclaration::getQualifiedSignature).collect(Collectors.toSet());
    }

    public static Collection<String> methodsToSignatureString(Map<String, Set<ResolvedMethodDeclaration>> methods) {
        return methods.values().stream().flatMap(Set::stream).map(ResolvedMethodLikeDeclaration::getQualifiedSignature).collect(Collectors.toSet());
    }

    public static ClassOrInterfaceDeclaration getClassByName(String name, Collection<CompilationUnit> compilationUnits) {
        return compilationUnits.stream()
                .filter(cu -> cu.getClassByName(name).isPresent())
                .findFirst().orElseThrow()
                .getClassByName(name).orElseThrow();
    }

    public static ClassOrInterfaceDeclaration getInterfaceByName(String name, Collection<CompilationUnit> compilationUnits) {
        return compilationUnits.stream()
                .filter(cu -> cu.getInterfaceByName(name).isPresent())
                .findFirst().orElseThrow()
                .getInterfaceByName(name).orElseThrow();
    }

    public static MethodDeclaration getMethod(String name, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getMethods().stream()
                .filter(m -> m.getNameAsString().equals(name))
                .findFirst().orElseThrow();
    }

    public static Condition<? super String> checkContains(String[] contained) {
        return new Condition<>() {
            @Override
            public boolean matches(String value) {
                boolean ret = true;
                for (String s : contained) {
                    ret &= value.contains(s);
                }
                return ret;
            }
        };
    }
}
