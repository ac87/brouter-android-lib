package com.routing.brouter

import android.content.Context
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.math.max

object Util {

    @Throws(Exception::class)
    fun fileEqual(fileBytes: ByteArray, file: File): Boolean {
        if (!file.exists()) {
            return false
        }
        val nbytes = fileBytes.size
        var pos = 0
        val blen = 8192
        val buf = ByteArray(blen)
        var inputStream: InputStream? = null
        return try {
            inputStream = FileInputStream(file)
            while (pos < nbytes) {
                val len = inputStream.read(buf, 0, blen)
                if (len <= 0) return false
                if (pos + len > nbytes) return false
                for (j in 0 until len) {
                    if (fileBytes[pos++] != buf[j]) {
                        return false
                    }
                }
            }
            true
        } finally {
            if (inputStream != null) try {
                inputStream.close()
            } catch (io: IOException) {
            }
        }
    }

    fun buildDoubleArray(from: Double, to: Double, vias: List<Double>): DoubleArray {
        val lats = DoubleArray(vias.size + 2)
        lats[0] = from
        for (i in vias.indices) lats[i + 1] = vias[i]
        lats[1 + max(vias.size, 0)] = to
        return lats
    }

    fun buildDoubleArray(doubleList: List<Double>): DoubleArray {
        val result = DoubleArray(doubleList.size)
        for (i in doubleList.indices) result[i] = doubleList[i]
        return result
    }

    fun copyAssetFile(context: Context, assetFile: String?, outputFile: String?) {
        try {
            val inputStream = context.assets.open(assetFile!!)
            val outputStream = FileOutputStream(outputFile)
            copyFile(inputStream, outputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
    }

    fun unzip(zipFile: InputStream, destination: String): Boolean {
        val buffer = ByteArray(1024)
        try {
            val zin = ZipInputStream(zipFile)
            var ze: ZipEntry?
            while (zin.nextEntry.also { ze = it } != null) {
                val f = File(destination, ze!!.name)
                if (!f.exists()) {
                    val success = f.createNewFile()
                    if (!success) {
                        continue
                    }
                    val fout = FileOutputStream(f)
                    var count: Int
                    while (zin.read(buffer).also { count = it } != -1) {
                        fout.write(buffer, 0, count)
                    }
                    zin.closeEntry()
                    fout.close()
                }
            }
            zin.close()
            return true
        } catch (e: java.lang.Exception) {
            return false
        }
    }
}