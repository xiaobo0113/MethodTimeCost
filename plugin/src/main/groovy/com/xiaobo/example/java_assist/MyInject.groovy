package com.xiaobo.example.java_assist

import javassist.*
import javassist.bytecode.ClassFile
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * 注入代码分为两种情况，一种是目录，需要遍历里面的 class 进行注入;
 * 另外一种是 jar 包，需要先解压 jar 包，注入代码之后重新打包成 jar
 */
class MyInject {

    private static Project appProject
    private static ClassPool pool = ClassPool.getDefault()
    private static String tagReal = "MethodTimeCost"
    private static long costBiggerThanReal = 0

    static void setAppProject(Project appProject) {
        this.appProject = appProject
        if (appProject.method_time_cost.tag) {
            tagReal = appProject.method_time_cost.tag
        }
        if (appProject.method_time_cost.costBiggerThan > 0) {
            costBiggerThanReal = appProject.method_time_cost.costBiggerThan
        }
    }

    static void removeClassPath(ClassPath classPath) {
        pool.removeClassPath(classPath)
    }

    /**
     * 添加 classPath 到 ClassPool 末尾
     * @param libPath
     */
    static ClassPath appendClassPath(String libPath) {
        ClassPath classPath = null
        if (new File(libPath).exists()) {
            classPath = pool.appendClassPath(libPath)
        }

        return classPath
    }

    /**
     * 添加 classPath 到 ClassPool 开始
     * @param libPath
     */
    static ClassPath insertClassPath(String libPath) {
        ClassPath classPath = null
        if (new File(libPath).exists()) {
            println "\n>>>>>>>>>>>>> insertClassPath ${libPath} begin >>>>>>>>>>>>>"
            classPath = pool.insertClassPath(libPath)
            println "<<<<<<<<<<<<< insertClassPath ${libPath} end <<<<<<<<<<<<<\n"
        }

        return classPath
    }

    /**
     * 添加单个 class 文件到 ClassPool
     * @param className 形如 com.example.java_assist.TimeUtil
     * @param fullPath 全路径
     */
    static void appendByteArrayClassPath(String className, String fullPath) {
        ByteArrayClassPath path = new ByteArrayClassPath(className, new File(fullPath).bytes)
        pool.appendClassPath(path)
    }

    /**
     * 添加单个 class 文件到 ClassPool
     * @param className 形如 com.example.java_assist.TimeUtil
     */
    static void appendClassClassPath(String className) {
        ClassClassPath path = new ClassClassPath(Class.forName(className))
        pool.appendClassPath(path)
    }

    /**
     * 遍历该目录下的所有 class，对所有 class 进行代码注入。
     * 其中以下 class 是不需要注入代码的：
     * --- 1. R 文件相关
     * --- 2. 配置文件相关（BuildConfig）
     * --- 3. Application
     * @param path 目录的绝对路径
     */
    static void injectDir(String path) {
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                if (filePath.endsWith(".class")
                        && !filePath.contains('R$')
                        && !filePath.contains('R.class')
                        && !filePath.contains("BuildConfig.class")) {

                    // 获取 class name，形如 com.xiaobo.example.modifybytecode_javaassist.Test
                    String className
                    if (path.contains("\\")) {
                        // on windows
                        className = filePath.replace(path + "\\", "").replace('\\', '.').replace(".class", "")
                    } else {
                        // on linux
                        className = filePath.replace(path + "/", "").replace('/', '.').replace(".class", "")
                    }
                    // println "============= processing class ${className} ============="
                    injectClass(className, path)
                }
            }
        }
    }

    /**
     * 这里需要将 jar 包先解压，注入代码后再重新生成 jar 包
     * @param path 输入 jar 包的绝对路径
     */
    static void injectJar(String path) {
        if (path.endsWith(".jar")) {
            File jarFile = new File(path)

            // jar包解压后的保存路径
            String jarZipDir = jarFile.getParent() + "/" + jarFile.getName().replace('.jar', '')

            // 解压jar包, 返回jar包中所有class的完整类名的集合（带.class后缀）
            List classNameList = JarZipUtil.unzipJarWithCheck(path, jarZipDir)

            if (null != classNameList) {
                // 删除原来的jar包
                jarFile.delete()
                // 注入代码
                for (String className : classNameList) {
                    if (className.endsWith(".class")
                            && !className.contains('R$')
                            && !className.contains('R.class')
                            && !className.contains("BuildConfig.class")) {
                        className = className.substring(0, className.length() - 6)
                        // println "============= processing class ${className} ============="
                        injectClass(className, jarZipDir)
                    }
                }
                // 从新打包jar
                JarZipUtil.zipJar(jarZipDir, path)
            } else {
                println "?????????????????? error: file ${path} have duplicate class(ignore case), ignored."
            }

            // 删除目录
            FileUtils.deleteDirectory(new File(jarZipDir))
        }
    }

    private static void injectClass(String className, String path) {
        CtClass c = pool.getCtClass(className)
        if (c.isFrozen()) {
            c.defrost()
        }

        if (className == TimeUtil.canonicalName) {
            return
        }

        // 尝试计算每个方法调用的时间
        boolean hasInsertedField = false
        CtMethod[] methods = c.getDeclaredMethods()
        for (CtMethod method : methods) {
            // println "------------- ${method.longName} -------------"
            // 抽象方法和 native 法不处理
            if (null != method.getMethodInfo2().getCodeAttribute()) {
                try {
                    // addTiming1(c, method.name)
                    // addTiming2(c, method)

                    if (!hasInsertedField) {
                        hasInsertedField = true

                        CtClass linkedListClass = pool.get("java.util.LinkedList");
                        // CtField startTimeList = new CtField("Ljava/lang/Object;", "__start_time_list", c)
                        CtField startTimeList = new CtField(linkedListClass, "__start_time_list", c)
                        startTimeList.setModifiers(Modifier.PUBLIC)
                        startTimeList.setModifiers(Modifier.STATIC)
                        c.addField(startTimeList, CtField.Initializer.byNew(linkedListClass))
                    }
                    addTiming3(c, method)
                } catch (Exception e) {
                    println "?????????????????? error: $e, ${method.longName} ignored."
                }
            }
        }

        if (c.getClassFile().getMajorVersion() == ClassFile.JAVA_8) {
            println "++++++++++++++++++++${path}, ${c.name} is compiled with JAVA_8."
        }

        // 直接使用 c.writeFile 会有写入失败的情况，比如某个 jar 包中的类引用了另一个不存在的类。
        try {
            // c.writeFile(path)
            def realPath = new File(path, c.name.replace('.', '/') + '.class')
            // 先转换成 bytes，此时可能失败，比如某个 jar 包中的类引用了另一个不存在的类，但是此时 OutputStream 还没有介入，不至于最原始的文件被清空了。
            byte[] bytes = c.toBytecode()
            realPath.withOutputStream {
                it.write(bytes)
            }
        } catch (Exception e) {
            println "?????????????????? error: $e, ${c.name} ignored."
        }

        c.detach()
    }

    private static String getLog(String methodName) {
        return """
            if (java.lang.Thread.currentThread().getName().equals("main")) {
                long cost = System.currentTimeMillis() - start;
                if (cost >= $costBiggerThanReal) {
                    android.util.Log.d("$tagReal", "${methodName} cost " + cost + " ms.");
                }
            }
        """
    }

    static void addFieldsToTimeUtil() {
        CtClass c = pool.get(TimeUtil.canonicalName)
        // 改变 sTag sCostBiggerThan 变量值
        try {
            c.getDeclaredField("sTag_Real")
        } catch (NotFoundException e) {
            CtField field = c.getDeclaredField("sTag")
            CtField realField = new CtField(field, c)
            realField.setName("sTag_Real")
            c.addField(realField, "\"${appProject.method_time_cost.tag}\"")

            // println "%%%%%%% add sTag_Real: ${realField}"
        }
        try {
            c.getDeclaredField("sCostBiggerThan_Real")
        } catch (NotFoundException e) {
            CtField field = c.getDeclaredField("sCostBiggerThan")
            CtField realField = new CtField(field, c)
            realField.setName("sCostBiggerThan_Real")
            c.addField(realField, "(long)${appProject.method_time_cost.costBiggerThan}")

            // println "%%%%%%% add sCostBiggerThan_Real: ${realField}"
        }
    }

    /**
     * 参考 https://www.ibm.com/developerworks/library/j-dyn0916/index.html
     *
     * 采用包装一个新方法来调用原方法的方式，有一个问题是方法数会多一倍，用 addTiming2() 改进！
     */
    private static void addTiming1(CtClass clazz, String oldName) {
        //  get the method information (throws exception if method with
        //  given name is not declared directly by this class, returns
        //  arbitrary choice if more than one with the given name)
        CtMethod oldMethod = clazz.getDeclaredMethod(oldName)

        //  rename old method to synthetic name, then duplicate the
        //  method with original name for use as interceptor
        String newName = oldName + "_Impl"
        oldMethod.setName(newName)
        CtMethod newMethod = CtNewMethod.copy(oldMethod, oldName, clazz, null)

        //  start the body text generation by saving the start time
        //  to a local variable, then call the timed method; the
        //  actual code generated needs to depend on whether the
        //  timed method returns a value
        String type = oldMethod.getReturnType().getName()
        StringBuffer body = new StringBuffer()
        body.append("{\nlong start = System.currentTimeMillis();\n")
        if ("void" != type) {
            body.append(type + " result = ")
        }
        body.append(newName + "(\$\$);\n")

        //  finish body text generation with call to print the timing
        //  information, and return saved value (if not void)
        body.append("${getLog(newMethod.longName)}\n")
        if ("void" != type) {
            body.append("return result;\n")
        }
        body.append("}")

        //  replace the body of the interceptor method with generated
        //  code block and add it to class
        newMethod.setBody(body.toString())
        clazz.addMethod(newMethod)

        // print the generated code block just to show what was done
        // println "Interceptor method body:\n${body.toString()}"
    }

    private static void addTiming2(CtClass c, CtMethod method) {
        String beforeCommand = TimeUtil.class.name + ".push();"
        method.insertBefore(beforeCommand)

        String afterCommand = TimeUtil.class.name + ".pop();"
        method.insertAfter(afterCommand)
    }

    private static void addTiming3(CtClass c, CtMethod method) {
        method.insertBefore("""
            com.xiaobo.example.java_assist.TimeUtil.push(__start_time_list);
        """)
        method.insertAfter("""
            com.xiaobo.example.java_assist.TimeUtil.pop(__start_time_list, "${method.longName}", "${tagReal}", (long)$costBiggerThanReal);
        """)

//        method.insertBefore("""
//            if (java.lang.Thread.currentThread().getName().equals("main")) {
//                if (null == __start_time_list) {
//                    __start_time_list = new java.util.LinkedList();
//                }
//                ((java.util.LinkedList) __start_time_list).push((Object) java.lang.Long.valueOf(System.currentTimeMillis()));
//            }
//        """)
//        method.insertAfter("""
//            if (java.lang.Thread.currentThread().getName().equals("main")) {
//                java.lang.Long start_time = (Long) ((java.util.LinkedList) __start_time_list).pop();
//                long end_time = System.currentTimeMillis();
//
//                StackTraceElement[] elements = java.lang.Thread.currentThread().getStackTrace();
//                String callFrom = com.xiaobo.example.java_assist.TimeUtil.getCallFrom(elements[2]);
//
//                // 当 (end - start) 大于多少时才输出
//                long cost = end_time - start_time.longValue();
//                if (cost >= com.xiaobo.example.java_assist.TimeUtil.getCostBiggerThan()) {
//                    String lineNumber = elements[2].getLineNumber() != -1 ? ":" + elements[2].getLineNumber() : "";
//                    if (!lineNumber.equals("")) {
//                        callFrom = callFrom.substring(0, callFrom.length() - 1) + lineNumber + ")";
//                    }
//                    android.util.Log.d(com.xiaobo.example.java_assist.TimeUtil.getTag(), callFrom + " cost: " + cost + " ms");
//                }
//            }
//        """)
    }

}
