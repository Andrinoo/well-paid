package com.wellpaid.core.model.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Manifest JSON servido em `{API_BASE_URL}android-update.json`. */
@Serializable
data class AndroidAppUpdateManifestDto(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    @SerialName("apk_url") val apkUrl: String,
    @SerialName("release_notes") val releaseNotes: String? = null,
)
