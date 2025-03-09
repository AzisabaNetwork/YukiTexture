package net.azisaba.yukitexture

import net.azisaba.yukitexture.util.GitUtil
import java.io.File

fun main() {
    GitUtil
        .pull(
            File("/Users/sysnote8/mcsrv/yttest/plugins/YukiTexture/temp/resourcepacks"),
            "https://github.com/azisaba/resourcepacks.git",
        ).fold({
            println("success!!")
        }) {
            println("failure $it")
        }
}
