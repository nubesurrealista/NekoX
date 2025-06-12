package tw.nekomimi.nekogram.utils

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtil {

    @JvmStatic
    fun makeZip(zipFile: File, vararg contents: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            contents.forEach { file ->
                if (!file.exists()) return@forEach
                file.parentFile?.let { zipRecursively(zos, file, it) }
            }
        }
    }

    private fun zipRecursively(
        zos: ZipOutputStream,
        file: File,
        baseDir: File,
    ) {
        val entryName = getEntryName(file, baseDir)

        if (file.isDirectory) {
            val children = file.listFiles()
            if (children.isNullOrEmpty()) {
                // empty directory
                zos.putNextEntry(ZipEntry("$entryName/"))
                zos.closeEntry()
            } else {
                for (child in children) {
                    zipRecursively(zos, child, baseDir)
                }
            }
        } else {
            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    bis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    private fun getEntryName(file: File, baseDir: File): String {
        val basePath = baseDir.absolutePath
        var entryName = file.absolutePath.substring(basePath.length).replace(File.separatorChar, '/')
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1)
        }
        return entryName
    }

    fun read(input: InputStream, path: String): ByteArray {

        ZipInputStream(input).use { zip ->

            while (true) {

                val entry = zip.nextEntry ?: break

                if (entry.name == path) return zip.readBytes()

            }

        }

        error("path not found")

    }

    @JvmStatic
    fun unzip(input: InputStream, output: File) {

        ZipInputStream(input).use { zip ->

            while (true) {

                val entry = zip.nextEntry ?: break

                val entryFile = File(output, entry.name)

                if (entry.isDirectory) {

                    entryFile.mkdirs()

                } else {

                    entryFile.outputStream().use {

                        zip.copyTo(it)

                    }

                }

            }

            zip.close()

        }

    }

}