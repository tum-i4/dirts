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
package edu.tum.sse.dirts.util;

import java.nio.file.Path;

public class DirtsUtil {

    public static Path getRootTemporaryDirectory(Path rootPath) {
        return rootPath.resolve(".dirts");
    }

    public static Path getSubTemporaryDirectory(Path rootPath, Path subPath) {
        return rootPath.resolve(subPath).resolve(".dirts");
    }

    public static Path getAffectedModulesPath(Path rootPath) {
        return getRootTemporaryDirectory(rootPath).resolve("affected_modules");
    }

    public static Path getChangedNodesPath(Path rootPath) {
        return getRootTemporaryDirectory(rootPath).resolve("changed_nodes");
    }

    public static Path getLibrariesPath(Path rootPath, Path subPath) {
        return getSubTemporaryDirectory(rootPath, subPath).resolve("libraries");
    }

    public static Path getSubPomPathRelative(Path subPath) {
        return subPath.resolve("pom.xml");
    }

    public static Path getGraphPath(Path rootPath, Path subPath, String suffix) {
        return getSubTemporaryDirectory(rootPath, subPath).resolve(Path.of("graph_" + suffix));
    }

    public static Path getChecksumsPath(Path rootPath, Path subPath, String suffix) {
        return getSubTemporaryDirectory(rootPath, subPath).resolve(Path.of("checksums_" + suffix));
    }

    public static Path getCUMappingPath(Path rootPath, Path subPath, String suffix) {
        return getSubTemporaryDirectory(rootPath, subPath).resolve(Path.of("cuMapping_" + suffix));
    }

    public static Path getBeansPath(Path rootPath, Path subPath, String prefix, String suffix) {
        return getSubTemporaryDirectory(rootPath, subPath).resolve(Path.of(prefix + "_beans_" + suffix));
    }

    public static Path getInjectionPointsPath(Path rootPath, Path subPath, String prefix, String suffix) {
        return getSubTemporaryDirectory(rootPath, subPath)
                .resolve(Path.of(prefix + "_injection_points_" + suffix));
    }
}
