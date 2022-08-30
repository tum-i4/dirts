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
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.MethodLevelControl;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.function.Function;

@Mojo(name = "method_level_select")
@Execute(goal = "method_level_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class MethodLevelSelectMojo extends AbstractSelectMojo<BodyDeclaration<?>> {

    private final Function<String, String> toContainingClass = t -> {
        String ret = t;
        if (ret.contains("(")) {
            ret = ret.substring(0, ret.indexOf("("));
        }
        if (ret.contains(".")) {
            ret = ret.substring(0, ret.lastIndexOf("."));
        }
        return ret;
    };


    @Override
    protected Control<BodyDeclaration<?>> getControl() {
        Blackboard<BodyDeclaration<?>> methodLevelBlackboard = getMethodLevelBlackboard();
        methodLevelBlackboard.setTestFilter( getTestFilter());
        return new MethodLevelControl(methodLevelBlackboard, true);
    }

    @Override
    public void execute() {
        doExecute(toContainingClass);
    }
}

