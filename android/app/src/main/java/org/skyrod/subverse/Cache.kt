package org.skyrod.subverse

import android.content.Context
import java.io.File

class Cache(private val context: Context) {
    private val dataDir = File(context.getExternalFilesDir(null), "data_store")

    fun init() {
        val initializedFlag = File(dataDir, ".initialized")
        if (!initializedFlag.exists()) {
            copyAssetsToDataDir("cache")  // copies everything under this assets subfolder
            initializedFlag.createNewFile()
        }
    }

    fun getPath(): File {
        return this.dataDir
    }

    private fun copyAssetsToDataDir(assetPath: String) {
        context.assets.list(assetPath)?.forEach { name ->
            val destFile = File(dataDir, name)
            // Create parent directories if they don't exist
            destFile.parentFile?.mkdirs()

            context.assets.open("$assetPath/$name").use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}