package com.wellpaid.core.network

import com.wellpaid.core.model.update.AndroidAppUpdateManifestDto
import retrofit2.http.GET

/** Sem autenticação — usa cliente HTTP sem interceptor de token. */
interface AppUpdateApi {
    @GET("android-update.json")
    suspend fun getManifest(): AndroidAppUpdateManifestDto
}
