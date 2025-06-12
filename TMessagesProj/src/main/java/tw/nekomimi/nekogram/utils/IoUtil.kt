package tw.nekomimi.nekogram.utils

import moe.hx030.momogram.util.res.ClassPathResource
import moe.hx030.momogram.util.res.FileResource
import moe.hx030.momogram.util.res.Resource
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.regex.Pattern

object IoUtil {

    @JvmStatic
    fun copy(inS: InputStream, outS: OutputStream) = inS.copyTo(outS)

    @JvmStatic
    fun copy(inS: InputStream, outF: File) {

        outF.parentFile?.also { FileUtil.initDir(it) }

        FileUtil.delete(outF)

        outF.createNewFile()

        outF.outputStream().use {

            inS.copyTo(it)

        }

    }

    @JvmStatic
    fun copy(process: Process, outF: File) {

        outF.parentFile?.also { FileUtil.initDir(it) }

        FileUtil.delete(outF)

        outF.createNewFile()

        outF.outputStream().use {

            process.inputStream.copyTo(it, 512)

        }

    }

    @JvmStatic
    fun deleteRecursively(path: File): Long {
        var claimedSpace = 0L
        val children = path.listFiles() ?: return 0
        for (child in children) {
            if (child.isDirectory) {
                claimedSpace += deleteRecursively(child)
                child.delete()
            } else {
                val size = child.length() / 1024
                if (child.delete())
                    claimedSpace += size
            }
        }
        return claimedSpace
    }

    @JvmStatic
    fun getResourceObj(path: String): Resource {
        if (StringUtils.isNotBlank(path)) {
            if (path.startsWith("file:") || isAbsolutePath(path)) {
                return FileResource(path)
            }
        }
        return ClassPathResource(path)
    }

    val PATTERN_PATH_ABSOLUTE: Pattern = Pattern.compile("^[a-zA-Z]:([/\\\\].*)?", Pattern.DOTALL)

    @JvmStatic
    fun isAbsolutePath(path: String): Boolean {
        if (StringUtils.isEmpty(path)) {
            return false
        }

        return '/' == path[0] || PATTERN_PATH_ABSOLUTE.matcher(path).lookingAt()
    }

    @JvmStatic
    fun exec(vararg args: String?): Process {
        val process: Process
        try {
            process = ProcessBuilder(*args).redirectErrorStream(true).start()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return process
    }
}