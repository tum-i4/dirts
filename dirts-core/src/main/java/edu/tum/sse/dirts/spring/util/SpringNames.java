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
package edu.tum.sse.dirts.spring.util;

import edu.tum.sse.dirts.spring.analysis.bean.XMLBeanDefinition;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Naming scheme for spring xml beans
 */
public class SpringNames {

    //##################################################################################################################
    // Lookup for beanDefinitions

    private static final Map<XMLBeanDefinition, Integer> beanDefinitions = new IdentityHashMap<>();

    public static String lookup(XMLBeanDefinition XMLBeanDefinition) {
        return XMLBeanDefinition.getNodeName();

        /*String customName = getCustomName(beanDefinitions, XMLBeanDefinition);
        String id = XMLBeanDefinition.getId().replace(".", "");
        return customName + (id.equals("") ? "" : " " + id);*/
    }

    protected static <N> String getCustomName(Map<N, Integer> nMap, N n) {
        if (!nMap.containsKey(n)) {
            int value = nMap.size();
            nMap.put(n, value);
        }

        return "BeanDefinition" + nMap.get(n);
    }
}
