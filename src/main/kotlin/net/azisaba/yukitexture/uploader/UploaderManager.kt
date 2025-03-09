package net.azisaba.yukitexture.uploader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.function.Consumer

object UploaderManager {
    private val uploaderMap = mutableMapOf<String, IUploader>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun registerUploader(
        uploaderName: String,
        uploader: IUploader,
    ) {
        uploaderMap[uploaderName] = uploader
    }

    fun unregisterUploader(uploaderName: String) {
        uploaderMap.remove(uploaderName)
    }

    private fun getUploader(uploaderName: String): IUploader =
        uploaderMap[uploaderName] ?: error("This uploader wasn't registered. name: $uploaderName")

    private fun upload(
        uploaderName: String,
        resourcePackZip: File,
        callBackFunc: Consumer<Result<String>>,
    ) {
        coroutineScope.launch {
            callBackFunc.accept(getUploader(uploaderName).upload(resourcePackZip.toPath()))
        }
    }

    suspend fun getUrl(uploaderName: String): Result<String> {
        getUploader(uploaderName).also { uploader ->
            if (uploader.lastUpdatedAt() == -1L) error("This uploader wasn't uploaded resource pack yet.")

            return uploader.getUrl()
        }
    }
}
