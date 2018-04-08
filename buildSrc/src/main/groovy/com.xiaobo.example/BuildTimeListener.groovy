package com.xiaobo.example

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState

class BuildTimeListener implements TaskExecutionListener, BuildListener {

    private long start
    private times = []
    def THRESHOLD = 50

    @Override
    void beforeExecute(Task task) {
        start = System.currentTimeMillis()
        println "TaskStart ==>> " + task.name
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        def ms = System.currentTimeMillis() - start
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