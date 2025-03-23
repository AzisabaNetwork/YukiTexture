package net.azisaba.yukitexture.git

import org.eclipse.jgit.api.PullResult
import java.io.File

interface IGitTool {
    fun update(targetFolder: File): Result<PullResult>

    fun pull(
        targetFolder: File,
        remoteUrl: String,
    ): Result<PullResult>
}
