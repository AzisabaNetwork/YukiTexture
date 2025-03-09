package net.azisaba.yukitexture.util

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullResult
import java.io.File

object GitUtil {
    fun update(targetFolder: File): Result<PullResult> {
        return try {
            val git = Git.open(targetFolder)
            return Result.success(git.pull().call())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pull(
        targetFolder: File,
        remoteUrl: String,
    ): Result<PullResult> {
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        } else {
            update(targetFolder).also {
                if (it.isSuccess) {
                    return it
                }
            }
            targetFolder.deleteRecursively()
            targetFolder.mkdirs()
        }
        try {
            val result =
                Git
                    .cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(targetFolder)
                    .call()
                    .pull()
                    .call()

            return if (result.isSuccessful) {
                Result.success(result)
            } else {
                Result.failure(RuntimeException("Failed to pull."))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
