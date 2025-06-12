package moe.hx030.momogram.util.res

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL

class FileResource(private val file: File) : Resource {

    constructor(path: String) : this(File(path))

    override fun getStream(): InputStream = FileInputStream(file)

    fun getFile(): File = file

    fun exists(): Boolean = file.exists()

    override fun getName(): String = file.name
    override fun getUrl(): URL = file.toURI().toURL()

    fun getPath(): String = file.absolutePath
}
