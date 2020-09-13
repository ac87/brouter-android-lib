package com.routing.brouter

/**
 * Enum of Profiles provided with the Library
 *
 * @property file the file name from within 'profiles.zip'
 */
enum class BundledProfile(val file: String) {
    CAR_FAST("car-fast"),
    CAR_ECO("car-eco"),
    BIKE_TREKKING("trekking"),
    BIKE_FAST("fastbike"),
    HIKING("hiking")
}
