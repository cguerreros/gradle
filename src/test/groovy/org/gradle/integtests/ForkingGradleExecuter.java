/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests;

import org.gradle.util.GUtil;
import static org.gradle.util.Matchers.*;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

// todo: implement more of the unsupported methods
public class ForkingGradleExecuter implements GradleExecuter {
    private final GradleDistribution distribution;
    private File workingDir;
    private int logLevel;
    private List<String> tasks;
    private List<String> args;
    private Map<String, Object> environmentVars = new HashMap<String, Object>();
    private String command;

    public ForkingGradleExecuter(GradleDistribution distribution) {
        logLevel = Executer.LIFECYCLE;
        tasks = new ArrayList<String>();
        args = new ArrayList<String>();
        this.distribution = distribution;
        workingDir = distribution.getTestDir();
    }

    public GradleExecuter inDirectory(File directory) {
        workingDir = directory;
        return this;
    }

    public GradleExecuter withSearchUpwards() {
        throw new UnsupportedOperationException();
    }

    public GradleExecuter withTasks(String... names) {
        tasks = Arrays.asList(names);
        return this;
    }

    public GradleExecuter withTasks(List<String> names) {
        tasks = new ArrayList<String>(names);
        return this;
    }

    public GradleExecuter withTaskList() {
        throw new UnsupportedOperationException();
    }

    public GradleExecuter withDependencyList() {
        throw new UnsupportedOperationException();
    }

    public GradleExecuter usingSettingsFile(File settingsFile) {
        throw new UnsupportedOperationException();
    }

    public GradleExecuter usingBuildScript(String script) {
        throw new UnsupportedOperationException();
    }

    public GradleExecuter withArguments(String... args) {
        this.args = Arrays.asList(args);
        return this;
    }

    public GradleExecuter withEnvironmentVars(Map<String, ?> environment) {
        environmentVars.clear();
        environmentVars.putAll(environment);
        return this;
    }

    public GradleExecuter withQuietLogging() {
        logLevel = Executer.QUIET;
        return this;
    }

    public GradleExecuter usingExecutable(String script) {
        command = script;
        return this;
    }

    public ExecutionResult run() {
        Map result = doRun(false);
        return new ForkedExecutionResult(result);
    }

    public ExecutionFailure runWithFailure() {
        Map result = doRun(true);
        return new ForkedExecutionFailure(result);
    }

    private Map doRun(boolean expectFailure) {
        String windowsCmd;
        String unixCmd;
        if (command != null) {
            windowsCmd = command;
            unixCmd = String.format("%s/%s", workingDir.getAbsolutePath(), command);
        } else {
            windowsCmd = "gradle";
            unixCmd = String.format("%s/bin/gradle", distribution.getGradleHomeDir().getAbsolutePath());
        }
        return Executer.executeInternal(windowsCmd,
                unixCmd,
                distribution.getGradleHomeDir().getAbsolutePath(),
                environmentVars,
                workingDir.getAbsolutePath(),
                GUtil.addLists(args, tasks),
                "",
                logLevel,
                expectFailure);
    }

    private static class ForkedExecutionResult implements ExecutionResult {
        private final Map result;

        public ForkedExecutionResult(Map result) {
            this.result = result;
        }

        public String getOutput() {
            return result.get("output").toString();
        }

        public String getError() {
            return result.get("error").toString();
        }

        public void assertTasksExecuted(String... taskPaths) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ForkedExecutionFailure extends ForkedExecutionResult implements ExecutionFailure {
        private final Map result;

        public ForkedExecutionFailure(Map result) {
            super(result);
            this.result = result;
        }

        public void assertHasLineNumber(int lineNumber) {
            throw new UnsupportedOperationException();
        }

        public void assertHasFileName(String filename) {
            assertThat(getError(), containsLine(startsWith(filename)));
        }

        public void assertHasCause(String description) {
            assertThatCause(equalTo(description));
        }

        public void assertThatCause(final Matcher<String> matcher) {
            assertThat(getError(), containsLine(new BaseMatcher<String>() {
                public boolean matches(Object o) {
                    String str = (String) o;
                    String prefix = "Cause: ";
                    return str.startsWith(prefix) && matcher.matches(str.substring(prefix.length()));
                }

                public void describeTo(Description description) {
                    matcher.describeTo(description);
                }
            }));
        }

        public void assertHasDescription(String context) {
            assertThatDescription(equalTo(context));
        }

        public void assertThatDescription(Matcher<String> matcher) {
            assertThat(getError(), containsLine(matcher));
        }
    }
}
