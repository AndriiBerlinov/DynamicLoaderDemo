package com.example.dynamicloaderdemo

import android.content.Context
import android.os.Build
import dalvik.system.DexClassLoader
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModuleRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("module_prefs", Context.MODE_PRIVATE)

    fun isAlreadyExecuted(): Boolean = prefs.getBoolean(KEY_RAN, false)

    fun getLastResultOrNull(): String? = prefs.getString(KEY_LAST_RESULT, null)

    fun markExecuted(result: String) {
        prefs.edit()
            .putBoolean(KEY_RAN, true)
            .putString(KEY_LAST_RESULT, result)
            .apply()
    }

    fun downloadModuleJar(moduleUrl: String): File {
        val outFile = File(context.filesDir, "module.jar")
        if (outFile.exists()) outFile.delete()

        val connection = (URL(moduleUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = true
        }

        connection.inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return outFile
    }

    fun runModuleFromJar(jarFile: File): String {
        val optDir = File(context.codeCacheDir, "dex_opt").apply { mkdirs() }

        val loader = DexClassLoader(
            jarFile.absolutePath,
            optDir.absolutePath,
            null,
            context.classLoader
        )

        val clazz = loader.loadClass("com.example.dynamicmodule.ModuleApi")
        val instance = clazz.getDeclaredConstructor().newInstance()

        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val method = clazz.getMethod("getDisplayString", String::class.java)

        return method.invoke(instance, deviceName) as String
    }

    fun deleteJar(jarFile: File) {
        if (jarFile.exists()) jarFile.delete()
    }

    companion object {
        private const val KEY_RAN = "module_ran"
        private const val KEY_LAST_RESULT = "module_last_result"
    }
}
