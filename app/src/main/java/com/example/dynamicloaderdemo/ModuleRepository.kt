package com.example.dynamicloaderdemo

import android.content.Context
import android.os.Build
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class ModuleRepository(private val context: Context) {

    private val prefs =
        context.getSharedPreferences("module_prefs", Context.MODE_PRIVATE)

    fun isAlreadyExecuted(): Boolean =
        prefs.getBoolean(KEY_RAN, false)

    fun getLastResultOrNull(): Pair<String, String>? {
        val device = prefs.getString(KEY_LAST_DEVICE, null) ?: return null
        val date = prefs.getString(KEY_LAST_DATE, null) ?: return null
        return device to date
    }

    fun markExecuted(deviceLine: String, dateLine: String) {
        prefs.edit()
            .putBoolean(KEY_RAN, true)
            .putString(KEY_LAST_DEVICE, deviceLine)
            .putString(KEY_LAST_DATE, dateLine)
            .apply()
    }

    fun downloadModuleJarBytes(moduleUrl: String): ByteArray {
        val tmpDir = File(context.cacheDir, "module_tmp").apply { mkdirs() }
        val tmpFile = File(tmpDir, "module.jar")
        if (tmpFile.exists()) tmpFile.delete()

        val connection = (URL(moduleUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Android")
        }

        val code = connection.responseCode
        if (code !in 200..299) {
            throw RuntimeException("Download failed: HTTP $code")
        }

        connection.inputStream.use { input ->
            tmpFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (tmpFile.length() < 1024) {
            tmpFile.delete()
            throw RuntimeException("Downloaded file too small")
        }

        val actualHash = sha256Hex(tmpFile)
        if (!actualHash.equals(EXPECTED_SHA256, ignoreCase = true)) {
            tmpFile.delete()
            throw RuntimeException("Hash mismatch. Module integrity check failed.")
        }

        val bytes = tmpFile.readBytes()
        tmpFile.delete()

        return bytes
    }

    fun runModuleFromJarBytes(jarBytes: ByteArray): String {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        return if (Build.VERSION.SDK_INT >= 26) {
            val dexBytes = extractClassesDex(jarBytes)

            val loader = InMemoryDexClassLoader(
                ByteBuffer.wrap(dexBytes),
                context.classLoader
            )

            invokeModule(loader, deviceName)

        } else {
            val dir = File(context.codeCacheDir, "legacy_dex").apply { mkdirs() }
            val jarFile = File(dir, "module.jar")
            jarFile.writeBytes(jarBytes)

            val optDir = File(context.codeCacheDir, "dex_opt").apply { mkdirs() }

            val loader = DexClassLoader(
                jarFile.absolutePath,
                optDir.absolutePath,
                null,
                context.classLoader
            )

            try {
                invokeModule(loader, deviceName)
            } finally {
                jarFile.delete()
            }
        }
    }

    private fun invokeModule(loader: ClassLoader, deviceName: String): String {
        val clazz =
            loader.loadClass("com.example.dynamicmodule.ModuleApi")
        val instance = clazz.getDeclaredConstructor().newInstance()
        val method =
            clazz.getMethod("getDisplayString", String::class.java)
        return method.invoke(instance, deviceName) as String
    }

    private fun extractClassesDex(jarBytes: ByteArray): ByteArray {
        ZipInputStream(ByteArrayInputStream(jarBytes)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.name == "classes.dex") {
                    return zis.readBytes()
                }
            }
        }
        throw RuntimeException("classes.dex not found in module.jar")
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_RAN = "module_ran"
        private const val KEY_LAST_DEVICE = "module_last_device"
        private const val KEY_LAST_DATE = "module_last_date"
        private const val EXPECTED_SHA256 =
            "CC0B3CC852D088109E390D1EB0AE0806D2B1CB740B90D4B9F93099CBE42CD55C"
    }
}
