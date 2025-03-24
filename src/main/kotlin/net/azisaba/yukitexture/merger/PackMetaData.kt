package net.azisaba.yukitexture.merger

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackData(
    @SerialName("pack_format") val packFormat: Int,
    val description: String,
)

@Serializable
data class PackMetaData(
    val pack: PackData,
)
