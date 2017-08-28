package com.xiaobo.example.java_assist

import org.gradle.api.Project

class JavaAssistExtension {

    private Project project

    private boolean enable = true
    private String tag = "MethodTimeCost"
    private long costBiggerThan = 0

    boolean getUseJava_7() {
        return useJava_7
    }

    void setUseJava_7(boolean useJava_7) {
        this.useJava_7 = useJava_7
    }
    private boolean useJava_7 = false

    private static final int MAX_LENGTH = 23

    JavaAssistExtension(Project project) {
        this.project = project
    }

    void setEnable(boolean enable) {
        this.enable = enable
    }

    boolean getEnable() {
        return this.enable
    }

    void setTag(String tag) {
        if (tag.length() > MAX_LENGTH) {
            tag = tag.substring(0, MAX_LENGTH)
        }
        this.tag = tag
    }

    String getTag() {
        return this.tag
    }

    void setCostBiggerThan(long costBiggerThan) {
        this.costBiggerThan = costBiggerThan
    }

    long getCostBiggerThan() {
        return this.costBiggerThan
    }

}