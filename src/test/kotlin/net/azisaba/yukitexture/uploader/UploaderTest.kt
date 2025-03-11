package net.azisaba.yukitexture.uploader

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual

class UploaderTest :
    FunSpec({
        test("register and get correctly") {
            val testUploaderName = "testUploader"
            val uploader = TestUploader()

            // register test uploader
            UploaderManager.registerUploader(testUploaderName, uploader)

            // check was registered correctly
            UploaderManager.getUploader(testUploaderName) shouldBeEqual uploader
        }
    })
