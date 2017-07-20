package com.xiaobo.example

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.gradle.util.Clock

class BuildTimeListener implements TaskExecutionListener, BuildListener {

    private Clock clock
    private times = []
    def THRESHOLD = 50

    @Override
    void beforeExecute(Task task) {
        clock = new org.gradle.util.Clock()
        println "TaskStart ==>> " + task.name
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        def ms = clock.timeInMs
        if (ms > THRESHOLD) {
            times << [(ms), task.path]
        }
        println "TaskEnd   ==>> " + task.name
        println "UseTimes  ==>> " + ms
        println "===================================================="
    }

    @Override
    void buildFinished(BuildResult result) {
        println "TimesDetails:"
        times.sort { it[0] }
        for (time in times) {
            printf "%7sms  %s\n", time[0], time[1]
        }
    }

    @Override
    void buildStarted(Gradle gradle) {}

    @Override
    void projectsEvaluated(Gradle gradle) {}

    @Override
    void projectsLoaded(Gradle gradle) {}

    @Override
    void settingsEvaluated(Settings settings) {}

}