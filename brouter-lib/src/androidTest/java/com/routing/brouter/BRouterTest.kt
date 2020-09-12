package com.routing.brouter

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.*

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.File

@RunWith(AndroidJUnit4::class)
class BRouterTest {

    @Before
    fun initBRouter() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dir: File = appContext.getExternalFilesDir(null)!!

        BRouter.initialise(appContext, dir)

        val outputDir = BRouter.segmentsFolderPath(dir)
        File(outputDir).mkdirs()

        val segmentsAssetDir = "segments"
        val segmentAssets = appContext.assets.list(segmentsAssetDir)
        for (segmentAsset in segmentAssets!!) Util.copyAssetFile(appContext, "$segmentsAssetDir/$segmentAsset", "$outputDir/$segmentAsset")
    }

    @Test
    fun generateRouteTest() {

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = appContext.getExternalFilesDir(null)!!

        val params = RoutingParams.Builder(dir)
                .profile(Profile.TREKKING)
                .from(54.543592, -2.950076)
                .addVia(54.530371, -3.004975)
                .to(54.542671, -2.966995)
                .build()

        val track = BRouter.generateRoute(params)

        assertThat(track, not(nullValue()))
        assertThat(track!!.distance, `is`(9146))
    }
}