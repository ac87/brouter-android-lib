package com.routing.brouter

import android.content.Context
import btools.router.OsmNodeNamed
import btools.router.RoutingContext
import btools.router.RoutingEngine
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*

object BRouter {

    private const val BROUTER_ROOT_DIR = "brouter"
    private const val SEGMENTS_SUB_DIR = "segments"
    private const val PROFILES_SUB_DIR = "profiles"
    private const val MODES_SUB_DIR = "modes"

    /**
     * Initialise BRouter's filesystem, creates the folders and copies the profiles
     *
     * @param context context
     * @param baseDir base directory for the app i.e. Context.getExternalFilesDir(null)
     */
    fun initialise(context: Context, baseDir: File) {
        // make directories
        val routerDir = File(baseDir, BROUTER_ROOT_DIR)
        if (!routerDir.exists()) routerDir.mkdir()
        val modesDir = File(routerDir, MODES_SUB_DIR)
        if (!modesDir.exists()) modesDir.mkdir()
        val profilesDir = File(routerDir, PROFILES_SUB_DIR)
        if (!profilesDir.exists()) profilesDir.mkdir()
        // extract profiles from assets
        Util.unzip(context.assets.open("profiles.zip"), profilesDir.toString())
    }

    /**
     * Get the path for the Segments directory given a base directory, this must be created
     * by the app and the segment files managed within
     *
     * @param baseDir base directory for the app i.e. Context.getExternalFilesDir(null)
     * @return the folder path
     */
    fun segmentsFolderPath(baseDir: File): String {
        return File(baseDir, "$BROUTER_ROOT_DIR/$SEGMENTS_SUB_DIR").toString()
    }

    /**
     * Get the path for the Profiles directory given a base directory
     * this is used for copying custom profiles to be used
     *
     * @param baseDir base directory for the app i.e. Context.getExternalFilesDir(null)
     * @return the folder path
     */
    fun profilesFolderPath(baseDir: File): String {
        return File(baseDir, "$BROUTER_ROOT_DIR/$PROFILES_SUB_DIR").toString()
    }

    /**
     * Generates a route from the given parameters
     * @see RoutingParams.Builder
     *
     * @param params route parameter
     * @return The track generated
     */
    fun generateRoute(params: RoutingParams): RouteResult {

        if (!params.validated) {
            val validationResult = validateParams(params)
            if (!validationResult.success)
                return RouteResult(validationResult.exception!!)
        }

        val baseDir = params.baseDirectory
        val segmentsPath = File(baseDir, "$BROUTER_ROOT_DIR/$SEGMENTS_SUB_DIR").toString()

        val profileName: String
        val profilePath: String
        if (params.customProfileFilePath != null) {
            profilePath = params.customProfileFilePath!!
            profileName = File(profilePath).nameWithoutExtension
        } else {
            profileName = params.bundledProfileFileName!!
            profilePath = "$baseDir/$BROUTER_ROOT_DIR/$PROFILES_SUB_DIR/$profileName.brf"
        }
        val rawTrackPath = "$baseDir/$BROUTER_ROOT_DIR/$MODES_SUB_DIR/${profileName}_rawtrack.dat"

        val nogos = readNogos(params)
        RoutingContext.prepareNogoPoints(nogos)

        val rc = RoutingContext()
        rc.rawTrackPath = rawTrackPath
        rc.localFunction = profilePath
        rc.turnInstructionMode = params.turnInstructionMode
        rc.startDirection = params.startDirection
        rc.nogopoints = nogos

        val waypoints = readPositions(params)
        try {
            writeTimeoutData(rc, baseDir, profileName, waypoints, nogos)
        } catch (e: Exception) {
        }

        val cr = RoutingEngine(null, null, segmentsPath, waypoints, rc)
        // stops the gpx being printed to system out, typo - should be quiet
        cr.quite = true
        cr.doRun(params.maxRunningTime)

        // store new reference track if any
        // (can exist for timed-out search)
        if (cr.foundRawTrack != null) {
            try {
                cr.foundRawTrack.writeBinary(rawTrackPath)
            } catch (e: Exception) {
            }
        }
        if (cr.errorMessage != null) {
            // datafile W5_N50.rd5 not found
            // TODO check the lat longs input can be routed to/from
            /*if (cr.errorMessage.contains("position not mapped in existing datafile")) {
                // the location isn't a road or track, this depends on the profile!
            }*/
            return RouteResult(Exception("Routing error: " + cr.errorMessage))
        }
        return RouteResult(cr.foundTrack)
    }

    /**
     * Validate parameters and files exist that are required
     *
     * @param params Routing parameters
     */
    fun validateParams(params: RoutingParams): ValidationResult {

        if (params.lats.isEmpty() || params.lons.isEmpty())
            return ValidationResult(IllegalArgumentException("lats or lons (To/From/Via) must be set"))

        if (params.bundledProfileFileName.isNullOrEmpty() && params.customProfileFilePath.isNullOrEmpty())
            return ValidationResult(IllegalArgumentException("bundledProfileFileName or customProfileFilePath must be set"))

        if (!params.customProfileFilePath.isNullOrEmpty() && !File(params.customProfileFilePath!!).exists())
            return ValidationResult(IllegalArgumentException("customProfileFilePath file does not exist"))

        val baseDir = params.baseDirectory
        if (baseDir.isEmpty()) return ValidationResult(IllegalArgumentException("baseDirectory must be set"))

        // check segments folder exists, this should be handled by the app.
        val segmentsDir = File(baseDir, "$BROUTER_ROOT_DIR/$SEGMENTS_SUB_DIR")
        if (!segmentsDir.exists()) return ValidationResult(Exception("'Segments' directory doesn't exist"))

        // check the segment files exist for the lat lons
        var i = 0
        while (i < params.lats.size && i < params.lons.size) {
            val fileName = Util.filenameForSegment(params.lats[i], params.lons[i])
            if (!File(segmentsDir, "$fileName.rd5").exists())
                return ValidationResult(Exception("Segment file $fileName doesn't exist for ${params.lats[i]},${params.lons[i]}"))
            i++
        }

        // check profiles folder, if this doesn't exist initialise hasn't been called.
        val profilesDir = File(baseDir, "$BROUTER_ROOT_DIR/$PROFILES_SUB_DIR")
        if (!profilesDir.exists()) return ValidationResult(Exception("'Profiles' directory doesn't exist - Call initialise at least once"))

        params.validated()
        return ValidationResult(true)
    }

    private fun readPositions(params: RoutingParams): List<OsmNodeNamed> {
        val wplist: MutableList<OsmNodeNamed> = ArrayList()
        val lats = params.lats
        val lons = params.lons
        require(!(lats.size < 2 || lons.size < 2)) { "we need two lat/lon points at least!" }
        var i = 0
        while (i < lats.size && i < lons.size) {
            val n = OsmNodeNamed()
            n.name = "via$i"
            n.ilon = Util.longitudeDoubleToInt(lons[i])
            n.ilat = Util.latitudeDoubleToInt(lats[i])
            wplist.add(n)
            i++
        }
        wplist[0].name = "from"
        wplist[wplist.size - 1].name = "to"
        return wplist
    }

    private fun readNogos(params: RoutingParams): List<OsmNodeNamed> {
        val nogoList: MutableList<OsmNodeNamed> = ArrayList()
        val lats = params.noGoLats
        val lons = params.noGoLons
        val radi = params.noGoRadis
        if (lats == null || lons == null || radi == null) return nogoList
        var i = 0
        while (i < lats.size && i < lons.size && i < radi.size) {
            val n = OsmNodeNamed()
            n.name = "nogo" + radi[i].toInt()
            n.ilon = Util.longitudeDoubleToInt(lons[i])
            n.ilat = Util.latitudeDoubleToInt(lats[i])
            n.isNogo = true
            n.nogoWeight = Double.NaN
            nogoList.add(n)
            i++
        }
        return nogoList
    }

    @Throws(Exception::class)
    private fun writeTimeoutData(rc: RoutingContext, baseDir: String, profileName: String, waypoints: List<OsmNodeNamed>, nogos: List<OsmNodeNamed>) {
        val timeoutFile = "$baseDir/$BROUTER_ROOT_DIR/$MODES_SUB_DIR/timeoutdata.txt"
        val bw = BufferedWriter(FileWriter(timeoutFile))
        bw.write(profileName)
        bw.write("\n")
        bw.write(rc.rawTrackPath)
        bw.write("\n")
        writeWaypointList(bw, waypoints)
        writeWaypointList(bw, nogos)
        bw.close()
    }

    @Throws(Exception::class)
    private fun writeWaypointList(bw: BufferedWriter, waypoints: List<OsmNodeNamed>) {
        bw.write("""
    ${waypoints.size}

    """.trimIndent())
        for (wp in waypoints) {
            bw.write(wp.toString())
            bw.write("\n")
        }
    }
}