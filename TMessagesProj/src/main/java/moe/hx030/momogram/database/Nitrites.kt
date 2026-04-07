package moe.hx030.momogram.database

import android.os.Build
import android.util.Log
import org.dizitart.no2.Nitrite
import org.dizitart.no2.mvstore.MVStoreModule
import org.telegram.messenger.ApplicationLoader
import moe.hx030.momogram.transtale.ChatCCTarget
import moe.hx030.momogram.transtale.ChatCCTargetConverter
import moe.hx030.momogram.transtale.ChatLanguageConverter
import moe.hx030.momogram.transtale.TransItemConverter
import moe.hx030.momogram.utils.FileUtil
import java.io.File

@JvmOverloads
fun mkDatabase(name: String, delete: Boolean = false): Nitrite? {
    if (Build.VERSION.SDK_INT < 26) return null

    val file = File("${ApplicationLoader.getDataDirFixed()}/databases/$name.db")
    FileUtil.initDir(file.parentFile!!)
    if (delete) {
        file.deleteRecursively()
    }

    fun create(): Nitrite {
        val storeModule: MVStoreModule = MVStoreModule.withConfig()
            .filePath(file)
            .compress(true)
            .build()

//        val db = Nitrite.builder()
//            .loadModule(storeModule)
//            .openOrCreate()
        val nitrite = Nitrite.builder().loadModule(storeModule)
                .registerEntityConverter(ChatCCTargetConverter())
                .registerEntityConverter(ChatLanguageConverter())
                .registerEntityConverter(TransItemConverter())
                .openOrCreate()!!

        val test = nitrite.openSharedPreference("shared_preferences")
        test.connection.close()

        return nitrite
    }

    runCatching {
        return create()
    }.onFailure {
        file.deleteRecursively()
    }

    return create()

}

fun Nitrite.openSharedPreference(name: String) = DbPref(getCollection(name))
