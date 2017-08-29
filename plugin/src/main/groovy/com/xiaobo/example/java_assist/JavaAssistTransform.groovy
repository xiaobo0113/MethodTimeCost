package com.xiaobo.example.java_assist

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import javax.xml.crypto.dsig.TransformException

/**
 * 参考：http://blog.csdn.net/u010386612/article/details/51131642
 *
 * Created by moxiaobo on 17/6/29.
 */

class JavaAssistTransform extends Transform {

    private Project mProject

    JavaAssistTransform(Project project) {
        mProject = project
    }

    @Override
    String getName() {
        return "JavaAssistTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        // 接受输入的类型：我们这里只处理Java类
        // return [QualifiedContent.DefaultContentType.CLASSES]

        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        // 作用范围：这里我们只处理工程中编写的类
        // return [QualifiedContent.Scope.PROJECT]

        return TransformManager.SCOPE_FULL_PROJECT
//        return com.google.common.collect.Sets.immutableEnumSet(
//                QualifiedContent.Scope.PROJECT,
//                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
//                QualifiedContent.Scope.SUB_PROJECTS,
//                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
//                // QualifiedContent.Scope.EXTERNAL_LIBRARIES,
//        )
    }

    @Override
    boolean isIncremental() {
        // 是否支持增量：这里暂时不实现增量
        return false
    }

    /**
     * 也就是遍历输入文件，转换后写入对应的输出文件。
     * 如果打算实现增量编译的话，可以通过方法 DirectoryInput.getChangedFiles() 来获取文件变化的状况，并作出相应的处理。
     */
    @Override
    void transform(TransformInvocation invocation) throws TransformException, InterruptedException, IOException {
        boolean ignore = false
        if (!getAppProject().method_time_cost.enable) {
            println ">>>>>>>>>>>>>> ${JavaAssistPlugin.class.simpleName} disabled, do nothing."
            ignore = true
        }

        TransformOutputProvider outputProvider = invocation.outputProvider
        // 获取 dir 输出路径
        def outDirDir = outputProvider.getContentLocation("java_assist_dir", outputTypes, scopes, Format.DIRECTORY)
        outDirDir.deleteDir()
        outDirDir.mkdirs()

        MyInject.setAppProject(getAppProject())
        appendBasicClassPath()
        releaseTimeUtilClass()

        // 提前把所有文件加入路径中，必须使用 insertClassPath 而不是 appendClassPath！！
        // 因为有些 jar 包中有占坑的类，比如 lego.jar 中就有 android.graphics.drawable.StateListDrawable 类，
        // 该类参与编译，但是最终运行在设备上时是运行的 android-sdk.jar 中的类。
        // 问题是 lego.jar 是用 java_7 编译的，如果不使用 insertClassPath，JavaAssist 读取到的是本地的 android-sdk.jar 中的类，
        // 而本地的该 jar 包很有可能由 java_8 编译而成，所以 JavaAssist 认为该 class 是 java_8 编译的，
        // 在写回 jar 包时也采用 java_8 的方式写回，最终导致 dex 过程出错！
        invocation.inputs.each {
            it.directoryInputs.each { DirectoryInput dir ->
                MyInject.insertClassPath(dir.file.absolutePath)
            }
            it.jarInputs.each { JarInput jar ->
                MyInject.insertClassPath(jar.file.absolutePath)
            }
        }

        invocation.inputs.each {
            // directoryInputs 就是 class 文件所在的目录：
            it.directoryInputs.each { DirectoryInput dir ->
                // 1. 先把修改后的文件写回原目录，再把原目录拷贝到目的目录
                // MyInject.injectDir(dir.file.absolutePath)
                // FileUtils.copyDirectory(dir.file, outDirDir)

                // 2. 先把原目录拷贝到目的目录，再把修改后的文件写回目的目录，这样原目录中的文件没有改动
                FileUtils.copyDirectory(dir.file, outDirDir)
                if (!ignore) {
                    println "\n>>>>>>>>>>>>> processing directory ${dir.file} begin >>>>>>>>>>>>>"
                    MyInject.injectDir(outDirDir.absolutePath)
                    println "<<<<<<<<<<<<< processing directory ${dir.file} end <<<<<<<<<<<<<\n"
                }
            }

            // 如果需要处理 jar 文件，那么也要对 it.jarInputs 进行处理：
            it.jarInputs.each { JarInput jar ->
                // 同上，采用方案 2
                String md5Path = DigestUtils.md5Hex(jar.file.absolutePath)
                String outName = jar.file.name.substring(0, jar.file.name.length() - '.jar'.length()) + "_${md5Path}"
                File outFile = outputProvider.getContentLocation(outName, outputTypes, scopes, Format.JAR)
                FileUtils.copyFile(jar.file, outFile)

                // 暂时不处理 libs 中的 jar 包代码
                if (!jar.file.absolutePath.startsWith(mProject.rootDir.absolutePath) || jar.file.name != 'classes.jar') {
                    return
                }

                if (!ignore) {
                    println "\n>>>>>>>>>>>>> processing jar ${jar.file} begin >>>>>>>>>>>>>"
                    MyInject.injectJar(outFile.absolutePath)
                    println ">>>>>>>>>>>>> processing jar ${jar.file} end >>>>>>>>>>>>>\n"
                }
            }
        }
    }

    private Project getAppProject() {
        for (Project p : mProject.rootProject.subprojects) {
            if (p.plugins.hasPlugin(AppPlugin.class)) {
                return p
            }
        }
        return null
    }

    private void appendBasicClassPath() {
        // 添加 android.jar 否则会提示找不到 android.util.Log 等类似的错误！！！
        AndroidJarPathUtil.appendAndroidJarPath(mProject)
        // 添加 org.apache.http.legacy.jar
        AndroidJarPathUtil.appendApacheHttpLegacyJarPath(mProject)
        /** 添加 {@link com.xiaobo.example.java_assist.TimeUtil} */
        AndroidJarPathUtil.appendTimeUtilPath(mProject)
    }

    private void releaseTimeUtilClass() {
        BaseExtension android = mProject.getExtensions().getByName("android")

        // 释放 TimeUtil 到编译目录，供运行时使用
        if (android instanceof AppExtension) {
            android.sourceSets.getByName('main') {
                android.getBuildTypes().forEach() {
                    InputStream is = TimeUtil.class.getResourceAsStream(TimeUtil.class.simpleName + ".class")
                    // println "============== build type: ${it.name}"
                    String timeUtilPackage = TimeUtil.class.package.name.replace('.', '/')
                    File dir = new File(mProject.buildDir, "intermediates/classes/${it.name}/${timeUtilPackage}")
                    println "============== dir: ${dir.absolutePath}"
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    OutputStream os = new FileOutputStream(new File(dir, TimeUtil.class.simpleName + ".class"))
                    os << is
                }
            }
        }
    }

}
