package tw.nekomimi.nekogram.utils

import java.io.File
import java.io.InputStream
import java.io.OutputStream

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

}