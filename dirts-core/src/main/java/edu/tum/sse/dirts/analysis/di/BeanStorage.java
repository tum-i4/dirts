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
package edu.tum.sse.dirts.analysis.di;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.*;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to store Beans
 */
public class BeanStorage<T> {

    /*
    Because beans are cached between two runs and Nodes should not be serialized, we need to use Strings as keys here
     */

    //##################################################################################################################
    // Attributes

    private final Set<T> allBeans = new HashSet<>();
    private final Map<String, Set<T>> beansByName = new HashMap<>();
    private final Map<String, Set<T>> beansByType = new HashMap<>();
    private final Map<String, Set<T>> beansByQualifier = new HashMap<>();

    //##################################################################################################################
    // Methods that add beans

    public void addBeanByName(String name, T newBean) {
        if (name != null && newBean != null) {
            if (!beansByName.containsKey(name)) {
                beansByName.put(name, new HashSet<>());
            }
            allBeans.add(newBean);
            beansByName.get(name).add(newBean);
        }
    }

    public void addBeanByType(ResolvedType type, T newBean) {
        if (type != null && newBean != null) {
            if (type.isReferenceType()) {
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = type.asReferenceType()
                        .getTypeDeclaration();
                if (maybeTypeDeclaration.isPresent()) {
                    addBeanByTypeDeclaration(maybeTypeDeclaration.get(), newBean);
                } else {
                    addBeanStrictlyByType(type, newBean);
                }
            } else {
                addBeanStrictlyByType(type, newBean);
            }
        }
    }

    public void addBeanByType(String type, T newBean) {
        if (type != null && newBean != null)
            if (!beansByType.containsKey(type)) {
                beansByType.put(type, new HashSet<>());
            }
        allBeans.add(newBean);
        beansByType.get(type).add(newBean);
    }

    private void addBeanStrictlyByType(ResolvedType type, T newBean) {
        String key = lookup(type);
        if (!beansByType.containsKey(key)) {
            beansByType.put(key, new HashSet<>());
        }
        allBeans.add(newBean);
        beansByType.get(key).add(newBean);
    }

    public void addBeanByTypeDeclaration(ResolvedTypeDeclaration typeDeclaration, T newBean) {
        if (typeDeclaration != null && newBean != null) {
            String key = lookup(typeDeclaration);
            if (!beansByType.containsKey(key)) {
                beansByType.put(key, new HashSet<>());
            }
            allBeans.add(newBean);
            beansByType.get(key).add(newBean);
        }
    }

    public void addBeanByQualifier(String qualifier, T newBean) {
        if (qualifier != null && newBean != null) {
            if (!beansByQualifier.containsKey(qualifier)) {
                beansByQualifier.put(qualifier, new HashSet<>());
            }
            allBeans.add(newBean);
            beansByQualifier.get(qualifier).add(newBean);
        }
    }

    //##################################################################################################################
    // Methods that remove beans

    public void removeBean(T bean) {
        allBeans.remove(bean);

        removeHelper(bean, beansByType);
        removeHelper(bean, beansByName);
        removeHelper(bean, beansByQualifier);
    }

    private void removeHelper(T bean, Map<String, Set<T>> beansBySome) {
        Set<String> toRemoveName = new HashSet<>();
        beansBySome.forEach((key, value) -> {
            value.remove(bean);
            if (value.isEmpty()) {
                toRemoveName.add(key);
            }
        });
        toRemoveName.forEach(beansBySome::remove);
    }

    //##################################################################################################################
    // Getters (...methods that retrieve beans)

    /**
     * Query the set of eligible beans
     * @param type specified type, may be null
     * @param name specified name, may be null
     * @param qualifiers specified qualifiers
     * @return set of eligible beans
     */
    public Set<T> getBeans(String type, String name, Set<String> qualifiers) {
        HashSet<T> ret = new HashSet<>(allBeans);

        if (type != null) {
            if (beansByType.containsKey(type)) {
                ret.retainAll(beansByType.get(type));
            } else {
                // if type is given, but does not match, we do not want any beans to be returned
                ret.clear();
            }
        }
        if (name != null && beansByName.containsKey(name)) {
            ret.retainAll(beansByName.get(name));
        }
        for (String qualifier : qualifiers) {
            if (beansByQualifier.containsKey(qualifier))
                ret.retainAll(beansByQualifier.get(qualifier));
        }
        return ret;
    }

    public Set<T> getAllBeans() {
        return Collections.unmodifiableSet(allBeans);
    }

    public Map<String, Set<T>> getBeansByName() {
        return Collections.unmodifiableMap(beansByName);
    }

    public Map<String, Set<T>> getBeansByType() {
        return Collections.unmodifiableMap(beansByType);
    }

    public Map<String, Set<T>> getBeansByQualifier() {
        return Collections.unmodifiableMap(beansByQualifier);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return allBeans.isEmpty();
    }

    //##################################################################################################################
    // Auxiliary methods

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!beansByType.isEmpty()) {
            sb.append("\nBy type:\n");
            beansByType.forEach((k, v) -> sb.append(k)
                    .append(" -> ")
                    .append("{")
                    .append(v.stream().map(Objects::toString).collect(Collectors.joining("; ")))
                    .append("}\n"));
        }
        if (!beansByName.isEmpty()) {
            sb.append("\nBy name:\n");
            beansByName.forEach((k, v) -> sb.append(k)
                    .append(" -> ")
                    .append("{")
                    .append(v.stream().map(Objects::toString).collect(Collectors.joining("; ")))
                    .append("}\n"));
        }
        if (!beansByQualifier.isEmpty()) {
            sb.append("\nBy qualifier:\n");
            beansByQualifier.forEach((k, v) -> sb.append(k)
                    .append(" -> ")
                    .append("{")
                    .append(v.stream().map(Objects::toString).collect(Collectors.joining("; ")))
                    .append("}\n"));
        }

        return sb.toString();
    }
}
