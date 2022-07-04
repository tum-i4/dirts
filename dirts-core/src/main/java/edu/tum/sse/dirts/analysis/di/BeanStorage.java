/*
 * Copyright 2022. The ttrace authors.
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

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.*;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to store Bean
 *
 * @param <T>
 */
public class BeanStorage<T extends Bean> {

    /*
     * Unfortunately, JavaParser considers two resolved entities as different, if their corresponding declarations
     * differ
     *
     * Declarations could differ, although they refer to the same type
     * That is why we use the qualified name as keys here
     *
     * Problems may occur when types are renamed and changed at the same revision
     * This is partially solved by treating such classes as added in the CodeChangeAnalyzer
     */

    //##################################################################################################################
    // Attributes

    private final Set<T> allBeans = new HashSet<>();
    private final Map<String, Set<T>> beansByName = new HashMap<>();
    private final Map<String, Set<T>> beansByType = new HashMap<>();
    private final Map<String, Set<T>> beansByQualifier = new HashMap<>();

    //##################################################################################################################
    // Setters (...methods that add beans)
    // Are designed to add a bean only if a corresponding name, type, declaration or qualifier is present

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
    // Getters
    // Are designed to yield the intersection of beans corresponding to name, type and qualifier
    // if at least one is present

    public Set<T> getBeansForType(ResolvedType type) {
        Set<T> ret = new HashSet<>();
        if (type != null) {
            String key;
            if (type.isReferenceType()) {
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = type.asReferenceType().getTypeDeclaration();
                if (maybeTypeDeclaration.isPresent()) {
                    key = lookup(maybeTypeDeclaration.get());
                } else {
                    key = lookup(type);
                }
                if (beansByType.containsKey(key))
                    ret.addAll(beansByType.get(key));
            } else {
                key = lookup(type);
                if (beansByType.containsKey(key))
                    ret.addAll(beansByType.get(key));
            }
        }
        return Collections.unmodifiableSet(ret);
    }

    public Set<T> getBeansForName(String name) {
        Set<T> ret = new HashSet<>();
        if (name != null)
            if (beansByName.containsKey(name))
                ret.addAll(beansByName.get(name));
        return Collections.unmodifiableSet(ret);
    }

    public Set<T> getBeansForQualifiers(Set<String> qualifiers) {
        Set<T> ret = new HashSet<>();
        if (qualifiers != null) {
            for (String qualifier : qualifiers) {
                if (beansByQualifier.containsKey(qualifier))
                    ret.addAll(beansByQualifier.get(qualifier));
            }
        }
        return Collections.unmodifiableSet(ret);
    }

    public Set<T> getBeansForTypeAndName(ResolvedType type, String name) {
        Set<T> ret = new HashSet<T>();
        if (type != null || name != null)
            ret.addAll(allBeans);
        if (type != null)
            ret.retainAll(getBeansForType(type));
        if (name != null)
            ret.retainAll(getBeansForName(name));
        return Collections.unmodifiableSet(ret);
    }

    public Set<T> getBeansForTypeAndQualifiers(ResolvedType type, Set<String> qualifiers) {
        Set<T> ret = new HashSet<T>();
        if (type != null || (qualifiers != null && !qualifiers.isEmpty()))
            ret.addAll(allBeans);
        if (type != null)
            ret.retainAll(getBeansForType(type));
        if (qualifiers != null && !qualifiers.isEmpty())
            ret.retainAll(getBeansForQualifiers(qualifiers));
        return Collections.unmodifiableSet(ret);
    }

    public Set<T> getBeansForNameAndQualifiers(String name, Set<String> qualifiers) {
        Set<T> ret = new HashSet<T>();
        if (name != null || (qualifiers != null && !qualifiers.isEmpty()))
            ret.addAll(allBeans);
        if (name != null)
            ret.retainAll(getBeansForName(name));
        if (qualifiers != null && !qualifiers.isEmpty())
            ret.retainAll(getBeansForQualifiers(qualifiers));
        return Collections.unmodifiableSet(ret);
    }

    public Set<T> getBeansForTypeAndNameAndQualifiers(ResolvedType type, String
            name, Set<String> qualifiers) {
        Set<T> ret = new HashSet<T>();
        if (type != null || (qualifiers != null && !qualifiers.isEmpty()) || name != null)
            ret.addAll(allBeans);
        if (type != null)
            ret.retainAll(getBeansForType(type));
        if (name != null)
            ret.retainAll(getBeansForName(name));
        if (qualifiers != null && !qualifiers.isEmpty())
            ret.retainAll(getBeansForQualifiers(qualifiers));
        return Collections.unmodifiableSet(ret);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nBy type:\n");
        beansByType.forEach((k, v) -> sb.append(k)
                .append(" -> ")
                .append("{")
                .append(v.stream().map(Bean::asString).collect(Collectors.joining("; ")))
                .append("}\n"));

        sb.append("\nBy name:\n");
        beansByName.forEach((k, v) -> sb.append(k)
                .append(" -> ")
                .append("{")
                .append(v.stream().map(Bean::asString).collect(Collectors.joining("; ")))
                .append("}\n"));

        sb.append("\nBy qualifier:\n");
        beansByQualifier.forEach((k, v) -> sb.append(k)
                .append(" -> ")
                .append("{")
                .append(v.stream().map(Bean::asString).collect(Collectors.joining("; ")))
                .append("}\n"));

        return sb.toString();
    }
}
