package net.simplifiedcoding.instadownload.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

abstract class BaseRepository {

    suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                Resource.Success(apiCall.invoke())
            } catch (e: Exception) {
                when (e) {
                    is HttpException -> Resource.Failure(
                        false,
                        e.code(),
                        e.response()?.errorBody()?.string()
                    )
                    else -> Resource.Failure(true, 0, null)
                }
            }
        }
    }
}