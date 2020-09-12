package com.routing.brouter

enum class TurnInstructionMode(val value: Int) {
    NONE(0),
    AUTO(1),
    LOCUS(2),
    OSMAND(3),
    COMMENT(4),
    GPSIES(5)
}