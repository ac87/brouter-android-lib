package com.routing.brouter

import android.content.Context
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.math.max

object Util {

    fun filenameForSegment(lat: Double, lon: Double): String {
        val latDegree = latitudeDoubleToInt(lat) / 1000000
        val lonDegree = longitudeDoubleToInt(lon) / 1000000
        val latMod5 = latDegree % 5
        val lonMod5 = lonDegree % 5
        val segLon: Int = lonDegree - 180 - lonMod5
        val segLat: Int = latDegree - 90 - latMod5

        val sLon = if (segLon < 0)
            "W${-segLon}"
        else
            "E$segLon"

        val sLat = if (segLat < 0)
            "S${-segLat}"
        else
            "N$segLat"

        return sLon + "_" + sLat
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

    internal fun latitudeDoubleToInt(latitude: Double) =
            ((latitude + 90.0) * 1000000.0 + 0.5).toInt()

    internal fun longitudeDoubleToInt(longitude: Double) =
            ((longitude + 180.0) * 1000000.0 + 0.5).toInt()

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