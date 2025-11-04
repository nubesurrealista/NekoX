package tw.nekomimi.nekogram.transtale

import android.os.Build
import android.util.Log
import org.dizitart.no2.filters.FluentFilter
import org.dizitart.no2.repository.ObjectRepository
import org.telegram.messenger.LocaleController
import tw.nekomimi.nekogram.NekoConfig
import tw.nekomimi.nekogram.database.mkDatabase
import tw.nekomimi.nekogram.utils.StrUtil
import tw.nekomimi.nekogram.utils.UIUtil
import java.util.*
import kotlin.collections.HashMap

class TranslateDb(val code: String) {

    var conn: ObjectRepository<TransItem?>? = db?.getRepository(TransItem::class.java, code)

    companion object {

        val db = mkDatabase("translate_caches")

        val repo = HashMap<Locale, TranslateDb>()
        val chat = db?.getRepository(ChatLanguage::class.java, "chat")
        val ccTarget = db?.getRepository(ChatCCTarget::class.java, "opencc")

        @JvmStatic fun getChatLanguage(chatId: Long, default: Locale): Locale? {
            if (Build.VERSION.SDK_INT < 26) return null
            if (chat == null) return null
            val cursor = chat.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach { return it.language.code2Locale }
            return default
//            return if (cursor.isEmpty) default else cursor.first().language.code2Locale

        }

        @JvmStatic
        fun getChatLanguage(chatId: Long): ChatLanguage? {
            if (Build.VERSION.SDK_INT < 26) return null
            if (chat == null) return null
            val cursor = chat.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach { return it }
            return null
        }

        @JvmStatic
        fun saveChatLanguage(chatId: Long, locale: Locale) = UIUtil.runOnIoDispatcher {
            if (chat == null) return@runOnIoDispatcher
            Log.d(StrUtil.get030Tag(TranslateDb), "saveLang: ${locale.locale2code}")
            val lang = getChatLanguage(chatId)
            val alwaysTranslateBeforeSend = lang?.alwaysTranslateBeforeSend

            chat.update(ChatLanguage(chatId, locale.locale2code, alwaysTranslateBeforeSend == true), true)

        }

        @JvmStatic
        fun getTranslateBeforeSend(chatId: Long): Boolean {
            if (Build.VERSION.SDK_INT < 26) return false
            if (chat == null) return false
            val cursor = chat.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach {
                return it.alwaysTranslateBeforeSend
            }
            return false
        }

        @JvmStatic
        fun setTranslateBeforeSend(chatId: Long, value: Boolean) = UIUtil.runOnIoDispatcher {
            if (Build.VERSION.SDK_INT < 26) return@runOnIoDispatcher
            if (chat == null) return@runOnIoDispatcher
            val cursor = chat.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach {
                it.alwaysTranslateBeforeSend = value
                chat.update(it, true)
                return@runOnIoDispatcher
            }
            chat.update(ChatLanguage(chatId, "en", value), true)
        }

        @JvmStatic
        fun getChatCCTarget(chatId: Long, default: String?): String? {
            if (Build.VERSION.SDK_INT < 26) return null
            if (ccTarget == null) return null
            val cursor = ccTarget.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach { return it as String? }
            return default
//            return ccTarget.find(FluentFilter.where("chatId").eq(chatId)).firstOrDefault()?.ccTarget
//                    ?: default

        }

        @JvmStatic
        fun saveChatCCTarget(chatId: Long, target: String) = UIUtil.runOnIoDispatcher {
            if (Build.VERSION.SDK_INT < 26) return@runOnIoDispatcher
            if (ccTarget == null) return@runOnIoDispatcher
            ccTarget.update(ChatCCTarget(chatId, target), true)

        }

        @JvmStatic
        fun currentTarget(): TranslateDb? {
            if (Build.VERSION.SDK_INT < 26) return null
            return NekoConfig.translateToLang.String()?.transDbByCode
                ?: LocaleController.getInstance().currentLocale.transDb
        }

        @JvmStatic
        fun currentTargetLocale(): Locale {
            return NekoConfig.translateToLang.String()?.code2Locale
                ?: LocaleController.getInstance().currentLocale
        }

        @JvmStatic
        fun forLocale(locale: Locale): TranslateDb? {
            if (Build.VERSION.SDK_INT < 26) return null
            return locale.transDb
        }

        @JvmStatic
        fun currentInputTarget() = NekoConfig.translateInputLang.String().transDbByCode

        @JvmStatic
        fun clearAll() {
            if (Build.VERSION.SDK_INT < 26) return
            if (db == null) return
            db.listRepositories()
                    .filter { it  != "chat" }
                    .map { db.getCollection(it) }
                    .forEach { it.drop() }

            repo.clear()

        }

    }

    fun clear() = synchronized(this) {
        if (Build.VERSION.SDK_INT < 26) return@synchronized
        conn?.drop()
    }

    fun contains(text: String): Boolean = synchronized(this) {
        if (Build.VERSION.SDK_INT < 26) return false
        return (conn?.find(FluentFilter.where("text").eq(text))?.count() ?: 0) > 0
    }

    fun save(text: String, trans: String) = synchronized<Unit>(this) {
        if (Build.VERSION.SDK_INT < 26) return
        conn?.update(TransItem(text, trans), true)
    }

    fun query(text: String): String? = synchronized(this) {
        if (NekoConfig.ignoreTranslatorCache.Bool() || Build.VERSION.SDK_INT < 26) return null
        val cursor = conn?.find(FluentFilter.where("text").eq(text))
        cursor?.forEach { return it?.trans }
        return null // conn.find(FluentFilter.where("text").eq(text)).firstOrDefault()?.trans
    }

}