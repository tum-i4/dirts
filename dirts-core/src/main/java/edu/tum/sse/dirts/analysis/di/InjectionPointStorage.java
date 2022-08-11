package edu.tum.sse.dirts.analysis.di;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.util.tuples.Triple;

import java.util.*;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to store InjectionPoints
 */
public class InjectionPointStorage {

    private final Map<String, Set<Triple<String, String, Set<String>>>> injectionPoints = new HashMap<>();

    //##################################################################################################################
    // Methods that add InjectionPoints

    public void addInjectionPoint(String injectionPoint,
                                  ResolvedType type,
                                  String name,
                                  Set<String> qualifiers) {

        String typeString = null;
        if (type.isReferenceType()) {
            Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = type.asReferenceType()
                    .getTypeDeclaration();
            if (maybeTypeDeclaration.isPresent()) {
                typeString = lookup(maybeTypeDeclaration.get());
            }
        } else {
            typeString = lookup(type);
        }

        Triple<String, String, Set<String>> value = new Triple<>(typeString, name, qualifiers);
        injectionPoints.computeIfAbsent(injectionPoint, k -> new HashSet<>());
        injectionPoints.get(injectionPoint).add(value);
    }

    public void addAll(InjectionPointStorage other) {
        injectionPoints.putAll(other.getInjectionPoints());
    }

    //##################################################################################################################
    // Methods that remove InjectionPoints

    public void removeInjectionPoint(String injectionPoint) {
        injectionPoints.remove(injectionPoint);
    }

    //##################################################################################################################
    // Getters (methods that retrieve InjectionPoints)

    public Map<String, Set<Triple<String, String, Set<String>>>> getInjectionPoints() {
        return Collections.unmodifiableMap(injectionPoints);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return injectionPoints.isEmpty();
    }

    //##################################################################################################################
    // Auxiliary methods

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        injectionPoints.forEach((k, s) ->
                s.forEach(v ->
                        sb.append(k)
                                .append(" -> ")
                                .append("{")
                                .append("Type: ").append(v.getFirst()).append(", ")
                                .append("Name: ").append(v.getSecond()).append(", ")
                                .append("Qualifiers: ").append(v.getThird())
                                .append("}\n")));

        return sb.toString();
    }
}
