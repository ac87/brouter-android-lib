package com.routing.brouter

import btools.router.OsmTrack

class ValidationResult(val success: Boolean, val exception: Throwable?) {
    constructor(success: Boolean) : this(success, null)
    constructor(exception: Throwable) : this(false, exception)
}

class RouteResult(val success: Boolean, val id: Int, val track: OsmTrack?, val exception: Throwable?) {
    constructor(alternateId: Int, track: OsmTrack) : this(true, alternateId, track, null)
    constructor(exception: Throwable) : this(false, 0, null, exception)
}
