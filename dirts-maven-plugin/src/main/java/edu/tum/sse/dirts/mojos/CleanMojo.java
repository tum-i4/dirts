package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.util.DirtsUtil;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.SurefirePlugin;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.logging.Level.INFO;

@Mojo(name = "clean")
@Execute(goal = "clean")
public class CleanMojo extends AbstractDirtsMojo<BodyDeclaration<?>> {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path rootPath = getRootPath();
        Path subPath = getSubPath();

        Path temporaryDirectory = DirtsUtil.getSubTemporaryDirectory(rootPath, subPath);
        if (Files.exists(temporaryDirectory)) {
            try {
                Files.walk(temporaryDirectory).forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException e) {
                        Log.errLog(INFO, "Failed to remove file " + f + ": " + e.getMessage());
                    }
                });
                Files.delete(temporaryDirectory);
            } catch (IOException e) {
                Log.errLog(INFO, "Failed to remove temporary directory: " + e.getMessage());
            }
        }
    }

    @Override
    protected Control<BodyDeclaration<?>> getControl() {
        throw new UnsupportedOperationException();
    }
}
