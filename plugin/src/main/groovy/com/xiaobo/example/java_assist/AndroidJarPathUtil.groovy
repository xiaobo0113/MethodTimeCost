package com.xiaobo.example.java_assist

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project

class AndroidJarPathUtil {

    private static String getSDKPath(Project project) throws IOException {
        File fp = project.getRootDir()
        if (!fp.exists()) {
            return null
        }
        File fpPropPath = new File(new StringBuilder().append(fp.getAbsolutePath()).append("/local.properties").toString())

        if (!fpPropPath.exists()) {
            Map envMap = System.getenv()
            if (envMap.containsKey("ANDROID_HOME")) {
                return (String) envMap.get("ANDROID_HOME")
            }
            return null
        }

        Properties prop = new Properties()
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fpPropPath))
        prop.load(bis)

        return prop.getProperty("sdk.dir")
    }

    static String getCompileSdkVersion(Project project) {
        BaseExtension extension = project.getExtensions().getByName("android");
        if (extension != null) {
            return extension.getCompileSdkVersion();
        }
        return "android-23";
    }

    static void insertAndroidJarPath(Project project) throws IOException {
        String sdkPath = getSDKPath(project)
        println ":::: sdkPath: $sdkPath"

        String compileSdkVersion = getCompileSdkVersion(project)
        println ":::: compileSdkVersion: $compileSdkVersion"

        String fullAndroidJarPath = new StringBuilder().append(sdkPath).append("/platforms/").append(compileSdkVersion).append("/android.jar").toString()
        MyInject.appendClassPath(fullAndroidJarPath)
    }

    static void insertOtherJarPath(Project project) throws IOException {
        String sdkPath = getSDKPath(project)
        String appcompat = sdkPath + "/extras/android/support/v7/appcompat/libs/android-support-v7-appcompat.jar"
        MyInject.appendClassPath(appcompat)

        String cardview = sdkPath + "/extras/android/support/v7/cardview/libs/android-support-v7-cardview.jar"
        MyInject.appendClassPath(cardview)

        String recyclerview = sdkPath + "/extras/android/support/v7/recyclerview/libs/android-support-v7-recyclerview.jar"
        MyInject.appendClassPath(recyclerview)

        String gridlayout = sdkPath + "/extras/android/support/v7/gridlayout/libs/android-support-v7-gridlayout.jar"
        MyInject.appendClassPath(gridlayout)
    }

    static void insertTimeUtilPath(Project project) throws IOException {
        MyInject.appendClassClassPath(TimeUtil.class.name)
    }

}