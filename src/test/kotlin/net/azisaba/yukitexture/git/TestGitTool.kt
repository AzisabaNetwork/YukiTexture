package net.azisaba.yukitexture.git

import org.eclipse.jgit.api.PullResult
import java.io.File

class TestGitTool : IGitTool {
    override fun update(targetFolder: File): Result<PullResult> = Result.success(PullResult())

    override fun pull(
        targetFolder: File,
        remoteUrl: String,
    ): Result<PullResult> {
        TODO("Not yet implemented")
    }
}
