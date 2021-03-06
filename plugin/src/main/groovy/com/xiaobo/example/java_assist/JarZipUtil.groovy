package com.xiaobo.example.java_assist

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * 参考：http://blog.xebia.com/powerful-groovy/
 * 参考：https://github.com/timyates/groovy-common-extensions
 *
 * Created by hp on 2016/4/13.
 */
class JarZipUtil {

    /**
     * 将该jar包解压到指定目录。
     *
     * @param jarPath jar包的绝对路径
     * @param destDirPath jar包解压后的保存路径
     * @return 返回该jar包中包含的所有class的完整类名类名集合，其中一条数据如：com.aitski.hotpatch.Xxxx.class
     */
    static List unzipJar(String jarPath, String destDirPath) {
        List list = new ArrayList()
        if (jarPath.endsWith('.jar')) {

            JarFile jarFile = new JarFile(jarPath)
            Enumeration<JarEntry> jarEntrys = jarFile.entries()
            while (jarEntrys.hasMoreElements()) {
                JarEntry jarEntry = jarEntrys.nextElement()
                if (jarEntry.directory) {
                    continue
                }
                String entryName = jarEntry.getName()
                if (entryName.endsWith('.class')) {
                    String className = entryName.replace('\\', '.').replace('/', '.')
                    list.add(className)
                }
                String outFileName = destDirPath + "/" + entryName
                File outFile = new File(outFileName)
                outFile.getParentFile().mkdirs()
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                FileOutputStream fileOutputStream = new FileOutputStream(outFile)
                fileOutputStream << inputStream
                fileOutputStream.close()
                inputStream.close()
            }
            jarFile.close()
        }
        return list
    }

    /**
     * 将该jar包解压到指定目录，如果jar包同一个路径下同时有a.class和A.class存在，那么解压会有问题！
     * 如Baidu_Cloud_IMSDK-2.4.1.jar包下的zhida目录下。此时返回空，不对该jar做处理。
     */
    static List unzipJarWithCheck(String jarPath, String destDirPath) {
        List list = new ArrayList()
        if (jarPath.endsWith('.jar')) {

            JarFile jarFile = new JarFile(jarPath)
            Enumeration<JarEntry> jarEntrys = jarFile.entries()
            while (jarEntrys.hasMoreElements()) {
                JarEntry jarEntry = jarEntrys.nextElement()
                if (jarEntry.directory) {
                    continue
                }
                String entryName = jarEntry.getName()
                if (entryName.endsWith('.class')) {
                    String className = entryName.replace('\\', '.').replace('/', '.')
                    for (String s : list) {
                        if (s.toLowerCase() == className.toLowerCase()) {
                            return null
                        }
                    }
                    list.add(className)
                }
                String outFileName = destDirPath + "/" + entryName
                File outFile = new File(outFileName)
                outFile.getParentFile().mkdirs()
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                FileOutputStream fileOutputStream = new FileOutputStream(outFile)
                fileOutputStream << inputStream
                fileOutputStream.close()
                inputStream.close()
            }
            jarFile.close()
        }
        return list
    }

    /**
     * 重新打包jar
     *
     * @param packagePath 将这个目录下的所有文件打包成jar
     * @param destPath 打包好的jar包的绝对路径
     */
    static void zipJar(String packagePath, String destPath) {
        File file = new File(packagePath)
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(destPath))
        file.eachFileRecurse { File f ->
            if (!f.directory) {
                String entryName = f.getAbsolutePath().substring(packagePath.length() + 1)
                outputStream.putNextEntry(new ZipEntry(entryName))
                InputStream inputStream = new FileInputStream(f)
                outputStream << inputStream
                inputStream.close()
                outputStream.closeEntry()
            }
        }
        outputStream.close()
    }

}