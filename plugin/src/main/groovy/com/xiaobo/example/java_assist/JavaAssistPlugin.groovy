package com.xiaobo.example.java_assist

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by moxiaobo on 17/6/29.
 */
class JavaAssistPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        BaseExtension android = project.getExtensions().findByName("android")
        if (!(android instanceof AppExtension)) {
            throw new GradleException("plugin can only be used in application module.")
        }
        project.getExtensions().create("method_time_cost", JavaAssistExtension.class, project)
        android.registerTransform(new JavaAssistTransform(project))
    }

}
