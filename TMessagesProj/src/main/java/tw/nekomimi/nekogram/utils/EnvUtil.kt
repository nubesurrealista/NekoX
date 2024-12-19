package tw.nekomimi.nekogram.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.NekoConfig
import java.io.File
import java.util.*

object EnvUtil {

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    val rootDirectories: List<File> by lazy {

        try {
            val mStorageManager = ApplicationLoader.applicationContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            (mStorageManager.javaClass.getMethod("getVolumePaths").invoke(mStorageManager) as Array<String>).map { File(it) }
        } catch (e:  Throwable) {
            AndroidUtilities.getRootDirs()
        }

    }

    @JvmStatic
    val availableDirectories
        get() = LinkedList<File>().apply {

            add(File(ApplicationLoader.getDataDirFixed(), "files/media"))
            add(File(ApplicationLoader.getDataDirFixed(), "cache/media"))

            rootDirectories.forEach {

                add(File(it, "Android/data/" + ApplicationLoader.applicationContext.packageName + "/files"))
                add(File(it, "Android/data/" + ApplicationLoader.applicationContext.packageName + "/cache"))

            }

            if (Build.VERSION.SDK_INT < 30) {
                add(Environment.getExternalStoragePublicDirectory(StrUtil.getShortAppName()))
            }

        }.map { it.path }.toTypedArray()

    // This is the only media path of NekoX, don't use other!
    @JvmStatic
    fun getTelegramPath(): File {
        var defaultIndex = if (availableDirectories.size > 3) 2 else 0

        if (NekoConfig.cachePath.String() == "") {
            // https://github.com/NekoX-Dev/NekoX/issues/284
            NekoConfig.cachePath.setConfigString(availableDirectories[defaultIndex]);
        }
        var telegramPath = File(NekoConfig.cachePath.String())
        if (telegramPath.isDirectory || telegramPath.mkdirs()) {
            return telegramPath
        } else {
            NekoConfig.cachePath.setConfigString(availableDirectories[defaultIndex])
        }

        // fallback

        telegramPath = ApplicationLoader.applicationContext.getExternalFilesDir(null) ?: File(ApplicationLoader.getDataDirFixed(), "cache/files")

        if (telegramPath.isDirectory || telegramPath.mkdirs()) {

            return telegramPath

        }

        telegramPath = File(ApplicationLoader.getDataDirFixed(), "cache/files")

        if (!telegramPath.isDirectory) telegramPath.mkdirs();

        return telegramPath;

    }

    @JvmStatic
    var isWaydroid: Boolean? = null

    @JvmStatic
    fun checkIsWaydroid(): Boolean {
        if (isWaydroid != null) return isWaydroid!!

        val waydroidToolsVersion = AndroidUtilities.getSystemProperty("waydroid.tools_version")
        val fingerprint = AndroidUtilities.getSystemProperty("ro.build.fingerprint")
        isWaydroid = waydroidToolsVersion != null || fingerprint.contains("waydroid")
        return isWaydroid!!
    }

    @JvmStatic
    fun doTest() {

        FileLog.d("rootDirectories: ${rootDirectories.size}")

        rootDirectories.forEach { FileLog.d(it.path) }

    }

}