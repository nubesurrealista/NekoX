package moe.hx030.momogram.util.res

import android.os.Parcel
import android.os.Parcelable
import java.io.InputStream
import java.net.URL

class ClassPathResource(
    private val path: String,
    private val classLoader: ClassLoader? = null
) : Resource {

    private val loader: ClassLoader = classLoader ?: Thread.currentThread().contextClassLoader!!

    override fun getStream(): InputStream =
        loader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")

    override fun getUrl(): URL =
        loader.getResource(path)
            ?: throw IllegalArgumentException("Resource not found: $path")

    fun exists(): Boolean = loader.getResource(path) != null

    override fun getName(): String = path.substringAfterLast('/')

    fun getPath(): String = path
}
