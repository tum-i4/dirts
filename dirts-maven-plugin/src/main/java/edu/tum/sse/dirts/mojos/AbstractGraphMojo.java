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
package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.util.DirtsUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.INFO;

public abstract class AbstractGraphMojo<P extends BodyDeclaration<?>> extends AbstractDirtsMojo<P>{

    /**
     * Store graph representation on the filesystem instead of printing it to stdout
     */
    @Parameter(property = "toFile", defaultValue = "false")
    protected boolean toFile;

    protected abstract String getOutputFilePath();
    protected abstract String getDefaultOutputFilePath();

    @Override
    public void execute() throws MojoExecutionException {

        Log.setLogLevel(Level.parse(logging));
        JavaParserUtils.RESTRICTIVE = restrictive;

        Log.log(CONFIG, "Root path: " + getRootPath());
        Log.log(CONFIG, "Sub path: " + getSubPath());

        if (getProject().getPackaging().equals("pom")) {
            Log.log(INFO, "There are no tests that could be selected, " +
                    "since this project has packaging \"pom\".");
            try {
                Files.delete(DirtsUtil.getLibrariesPath(getRootPath(), getSubPath()));
            } catch (IOException e) {
                System.err.println("Failed to delete file containing dependencies: " + e.getMessage());
            }
            return;
        }

        Control<P> control = getControl();

        String fileName = getOutputFilePath();
        if (fileName.equals(getDefaultOutputFilePath()))
            fileName += "_" + getProject().getArtifactId();
        fileName += ".dot";

        if (toFile) {
            try {
                Path outputPath = Path.of(fileName);
                Files.writeString(outputPath, control.visualizeDependencyGraph());
            } catch (IOException ignored) {
                throw new MojoExecutionException("Unable to write dependency graph to file.\n");
            }
        } else {
            System.out.println(control.visualizeDependencyGraph());
        }
    }

}
