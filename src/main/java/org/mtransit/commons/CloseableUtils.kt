package org.mtransit.commons

import java.io.Closeable
import java.io.IOException
import java.io.Writer

object CloseableUtils {

    @JvmStatic
    fun closeQuietly(output: Writer?) {
        closeQuietly(output as Closeable?)
    }

    @JvmStatic
    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (ioe: IOException) {
            // ignore
        }
    }
}