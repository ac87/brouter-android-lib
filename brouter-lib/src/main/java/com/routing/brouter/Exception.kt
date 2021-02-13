package com.routing.brouter

import org.jetbrains.annotations.NotNull

public class SegmentMissingException(@NotNull val fileName: String, val latitude: Double?, val longitude: Double?) :
    Throwable("Segment file $fileName doesn't exist for $latitude,$longitude}")
