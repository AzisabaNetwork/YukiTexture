package net.azisaba.yukitexture.config

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable

@Serializable
data class YukiTextureConfig(
    val packUrl: String = "https://packs.example.com/resourcepack.zip",
    val dontApplyTexture: List<String> = listOf("child-server-1", "child-server-2"),
    val redis: RedisConfig = RedisConfig(),
    val merger: MergerConfig = MergerConfig(),
    val uploader: UploaderConfig = UploaderConfig(),
)

@Serializable
data class RedisConfig(
    val host: String = "host",
    val port: Int = 6379,
    val user: String = "user",
    val password: String = "password",
)

@Serializable
data class MergerConfig(
    @YamlComment(
        "このマージシステムは、アップローダーが有効化されていると自動的に使用されます。",
        "サポートしているターゲットは、現在フォルダのみです。",
        "優先度は上の方が高くなります。(ファイルの競合時に優先的に使用されます。)",
        "詳しくは、YukiTextureのREADMEを参照してください。",
    )
    val mergeTargets: Map<String, String> =
        mapOf(
            "testpack" to "???/packs/resourcepack.zip",
            "git-lifepack" to "https://github.com/azisaba/resourcepacks.git:~/life",
        ),
    // TODO: implement this
//    @YamlComment(
//        "packUrlで登録しているリソースパックを追加するかを選ぶことができます。(優先度は自動的に一番上になります。)",
//    )
//    val addNormalPack: Boolean = false,
)

@Serializable
data class UploaderConfig(
    val useUploader: Boolean = false,
    @YamlComment(
        "現在、s3のみがサポートされています。",
    )
    val uploaderType: String = "s3",
)
