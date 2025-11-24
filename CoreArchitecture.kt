/**
 * 🚀 Core Architecture: Safe API Calling with Koin & Coroutines
 * Stack: Kotlin | Koin | Flow | Result Pattern
 * Author: Trinadh Thatakula
 */

// 1. type-safe Result wrapper (The "Senior" way to handle data)
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Error(val exception: Throwable, val message: String? = null) : DataResult<Nothing>
    data object Loading : DataResult<Nothing>
}

// 2. The Extension Function (Your specialty)
// Safely executes a network call and catches exceptions automatically
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    apiCall: suspend () -> T
): DataResult<T> = withContext(dispatcher) {
    try {
        DataResult.Success(apiCall())
    } catch (e: Exception) {
        // Log to Crashlytics/Loki here
        DataResult.Error(e, e.localizedMessage ?: "Unknown Error")
    }
}

// 3. Koin Module Definition (Showing you understand DI)
val networkModule = module {
    single { 
        HttpClient(Android) {
            install(ContentNegotiation) { json() }
            install(Logging) { level = LogLevel.ALL }
        }
    }
    // Injecting the Repository with the safe caller
    factory<AuthRepository> { AuthRepositoryImpl(client = get()) }
}
