package com.routing.brouter

import android.content.Context
import btools.router.OsmNodeNamed
import btools.router.OsmTrack
import btools.router.RoutingContext
import btools.router.RoutingEngine
import java.io.*
import java.lang.IllegalArgumentException
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
    fun initialise(context: Context, baseDir: String) {
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
     * Generates a route from the given parameters
     * @see RoutingParams.Builder
     *
     * @param params route parameter
     * @return The track generated
     */
    @Throws(Exception::class)
    fun generateRoute(params: RoutingParams): OsmTrack? {

        if (params.lats.isEmpty() || params.lons.isEmpty())
            throw IllegalArgumentException("lats or lons (To/From/Via) must be set")

        if (params.profileFileName.isNullOrEmpty() && params.remoteProfile.isNullOrEmpty())
            throw IllegalArgumentException("profileFileName or remoteProfile must be set")

        val baseDir = params.baseDirectory
        if (baseDir.isEmpty()) throw IllegalArgumentException("baseDirectory must be set")

        // check segments folder exists, this should be handled by the app.
        val segmentsDir = File(baseDir, "$BROUTER_ROOT_DIR/$SEGMENTS_SUB_DIR")
        if (!segmentsDir.exists()) throw Exception("'Segments' directory doesn't exist")
        val segmentsPath = segmentsDir.toString()

        // check profiles folder, if this doesn't exist initialise hasn't been called.
        val profilesDir = File(baseDir, "$BROUTER_ROOT_DIR/$PROFILES_SUB_DIR")
        if (!profilesDir.exists()) throw Exception("'Profiles' directory doesn't exist - Call initialise at least once")
        val profilesPath = profilesDir.toString()

        val profileName: String = if (params.remoteProfile != null) "remote" else params.profileFileName!!
        val profilePath = "$baseDir/$BROUTER_ROOT_DIR/$PROFILES_SUB_DIR/$profileName.brf"
        val rawTrackPath = "$baseDir/$BROUTER_ROOT_DIR/$MODES_SUB_DIR/${profileName}_rawtrack.dat"

        if (params.remoteProfile != null) {
            val remoteProfile = params.remoteProfile
            val errMsg = storeRemoteProfile(profilesPath, remoteProfile!!)
            if (errMsg != null) throw Exception("Remote Profile exception: $errMsg")
        }

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
            throw Exception("Routing error: " + cr.errorMessage)
        }
        return cr.foundTrack
    }

    private fun storeRemoteProfile(profilePath: String, remoteProfile: String): String? {
        // store profile only if not identical (to preserve timestamp)
        val profileBytes = remoteProfile.toByteArray()
        val profileFile = File(profilePath)
        try {
            if (!Util.fileEqual(profileBytes, profileFile)) {
                var os: OutputStream? = null
                try {
                    os = FileOutputStream(profileFile)
                    os.write(profileBytes)
                } finally {
                    if (os != null) try {
                        os.close()
                    } catch (io: IOException) {
                    }
                }
            }
        } catch (e: Exception) {
            return "error caching remote profile: $e"
        }
        return null
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
            n.ilon = ((lons[i] + 180.0) * 1000000.0 + 0.5).toInt()
            n.ilat = ((lats[i] + 90.0) * 1000000.0 + 0.5).toInt()
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
            n.ilon = ((lons[i] + 180.0) * 1000000.0 + 0.5).toInt()
            n.ilat = ((lats[i] + 90.0) * 1000000.0 + 0.5).toInt()
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