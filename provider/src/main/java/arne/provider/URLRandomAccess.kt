package arne.provider

import java.io.Closeable

abstract class URLRandomAccess : Closeable {
    abstract val length: Long
    abstract fun read(offset: Long, size: Int, data: ByteArray): Int
    abstract override fun close()
}