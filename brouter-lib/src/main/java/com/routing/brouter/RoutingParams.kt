package com.routing.brouter

import java.util.*

class RoutingParams private constructor(
    var baseDirectory: String,
    var lats: DoubleArray,
    var lons: DoubleArray,
    var profileFileName: String?,
    var remoteProfile: String?,
    val noGoLats: DoubleArray?,
    val noGoLons: DoubleArray?,
    val noGoRadis: DoubleArray?,
    var startDirection: Int,
    var turnInstructionMode: Int,
    var maxRunningTime: Long
) {

    private constructor(builder: Builder) : this(builder.baseDirectory,
            Util.buildDoubleArray(builder.fromLat, builder.toLat, builder.viaLats),
            Util.buildDoubleArray(builder.fromLon, builder.toLon, builder.viaLons),
            builder.profile.file, builder.remoteProfile,
            Util.buildDoubleArray(builder.noGoLats), Util.buildDoubleArray(builder.noGoLons),
            Util.buildDoubleArray(builder.noGoRadis),
            builder.startDirection, builder.turnInstructionMode.value, builder.maxRunningTime)

    class Builder(val baseDirectory: String) {
        var fromLat = 0.0
        var fromLon = 0.0
        var toLat = 0.0
        var toLon = 0.0
        var profile = Profile.CAR_FAST
        var remoteProfile: String? = null
        val viaLats: MutableList<Double> = ArrayList()
        val viaLons: MutableList<Double> = ArrayList()
        val noGoLats: MutableList<Double> = ArrayList()
        val noGoLons: MutableList<Double> = ArrayList()
        val noGoRadis: MutableList<Double> = ArrayList()
        var turnInstructionMode: TurnInstructionMode = TurnInstructionMode.NONE
        var startDirection = 0
        var maxRunningTime: Long = 60000

        fun from(latitude: Double, longitude: Double) = apply { fromLat = latitude; fromLon = longitude }

        fun to(latitude: Double, longitude: Double) = apply { toLat = latitude; toLon = longitude }

        fun addVia(latitude: Double, longitude: Double) = apply { viaLats.add(latitude); viaLons.add(longitude) }

        fun addNoGo(latitude: Double, longitude: Double, radius: Double) = apply { noGoLats.add(latitude); noGoLons.add(longitude); noGoRadis.add(radius) }

        fun profile(profile: Profile) = apply { this.profile = profile }

        fun remoteProfile(remoteProfile: String?) = apply { this.remoteProfile = remoteProfile }

        fun startDirection(startDirection: Int) = apply { this.startDirection = startDirection }

        fun turnInstructions(turnInstructionMode: TurnInstructionMode) = apply { this.turnInstructionMode = turnInstructionMode }

        fun maxRunningTime(maxRunningTime: Long) = apply { this.maxRunningTime = maxRunningTime }

        fun build() = RoutingParams(this)
    }
}
