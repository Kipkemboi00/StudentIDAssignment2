package au.edu.vu.studentidassignment2.config

/** The only place to configure the API and optional class-location default. */
object AppConfig {
    const val BASE_URL = "https://nit3213apinew.onrender.com/"
    // Allow for slow hosted-service startup; a live probe took 139 seconds to respond.
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 180L
    const val CALL_TIMEOUT_SECONDS = 210L

    // No student location was supplied. Keep null to require an explicit choice.
    // Set to ClassLocation.FOOTSCRAY, ClassLocation.SYDNEY or ClassLocation.BR if known.
    val defaultLocation: ClassLocation? = null
}

enum class ClassLocation(val apiPath: String) {
    FOOTSCRAY("footscray"),
    SYDNEY("sydney"),
    BR("br")
}
