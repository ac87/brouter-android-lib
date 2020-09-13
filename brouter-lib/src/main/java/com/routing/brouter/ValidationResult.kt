package com.routing.brouter

import btools.router.OsmTrack

class ValidationResult(val success: Boolean, val exception: Throwable?) {
    constructor(success: Boolean) : this(success, null)
    constructor(exception: Throwable) : this(false, exception)
}

class RouteResult(val success: Boolean, val track: OsmTrack?, val exception: Throwable?) {
    constructor(track: OsmTrack) : this(true, track, null)
    constructor(exception: Throwable) : this(false, null, exception)
}
