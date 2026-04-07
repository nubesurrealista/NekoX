package moe.hx030.momogram.utils

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.Arrays
import moe.hx030.momogram.MomoConfig

object BufferUtil {
    @JvmStatic
    fun clear(buffer: Buffer?) {
        if (buffer == null || !buffer.hasArray() || buffer.isReadOnly || !MomoConfig.bufferCleaner.Bool()) return

        buffer.clear()

        when (buffer.javaClass) {
            ByteBuffer::class.java -> Arrays.fill(buffer.array() as ByteArray, 0)
            FloatBuffer::class.java -> Arrays.fill(buffer.array() as FloatArray, 0.0f)
            IntBuffer::class.java -> Arrays.fill(buffer.array() as IntArray, 0)
            StringBuffer::class.java -> Arrays.fill(buffer.array() as Array<*>, null)
        }
    }
}
