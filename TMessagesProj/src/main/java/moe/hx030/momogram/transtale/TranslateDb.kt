package moe.hx030.momogram.transtale

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import org.dizitart.no2.filters.FluentFilter
import org.dizitart.no2.repository.ObjectRepository
import org.telegram.messenger.LocaleController
import org.telegram.ui.LaunchActivity
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.database.mkDatabase
import moe.hx030.momogram.utils.StrUtil
import moe.hx030.momogram.utils.UIUtil
import java.util.*
import kotlin.collections.HashMap
import androidx.core.content.edit

class TranslateDb(val code: String) {

    var conn: ObjectRepository<TransItem?>? = db?.getRepository(TransItem::class.java, code)

    companion object {

        val db = mkDatabase("translate_caches")

        val repo = HashMap<Locale, TranslateDb>()
        val chat = db?.getRepository(ChatLanguage::class.java, "chat")
        val ccTarget = db?.getRepository(ChatCCTarget::class.java, "opencc")

        lateinit var fallbackPrefs: SharedPreferences

        @JvmStatic
        fun getChatLanguage(chatId: Long, default: Locale): Locale? {
            if (Build.VERSION.SDK_INT < 26) return getChatLanguageFallback(chatId, default)
            if (chat == null) return default
            val cursor = chat.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach { return it.language.code2Locale }
            return default
//            return if (cursor.isEmpty) default else cursor.first().language.code2Locale
        }

        @JvmStatic
        fun getChatLanguage(chatId: Long): ChatLanguage? {
            if (chat == null) return null
            if (Build.VERSION.SDK_INT < 26) {
                val loc = getChatLanguageFallback(chatId, null)
                return ChatLanguage(chatId, loc?.locale2code, false)
            }
            val cursor = chat.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach { return it }
            return null
        }

        @JvmStatic
        private fun getChatLanguageFallback(chatId: Long, default: Locale?): Locale? {
            initFallbackPrefs()
            return fallbackPrefs.getString("chat_lang_$chatId", default?.locale2code)?.code2Locale
        }

        @JvmStatic
        fun saveChatLanguage(chatId: Long, locale: Locale) = UIUtil.runOnIoDispatcher {
            if (chat == null) return@runOnIoDispatcher
            if (Build.VERSION.SDK_INT < 26) {
                return@runOnIoDispatcher saveChatLanguageFallback(chatId, locale)
            }
            Log.d(StrUtil.get030Tag(TranslateDb), "saveLang: ${locale.locale2code}")
            val lang = getChatLanguage(chatId)
            val alwaysTranslateBeforeSend = lang?.alwaysTranslateBeforeSend

            chat.update(ChatLanguage(chatId, locale.locale2code, alwaysTranslateBeforeSend == true), true)

        }

        @JvmStatic
        fun saveChatLanguageFallback(chatId: Long, locale: Locale) = UIUtil.runOnIoDispatcher {
            Log.d(StrUtil.get030Tag(TranslateDb), "saveLang: ${locale.locale2code}")
            val lang = getChatLanguage(chatId)

            // chat.update(ChatLanguage(chatId, locale.locale2code, alwaysTranslateBeforeSend == true), true)
            fallbackPrefs.edit { putString("chat_lang_$chatId", locale.locale2code) }

        }

        @JvmStatic
        fun getTranslateBeforeSend(chatId: Long): Boolean {
            if (chat == null) return false
            val cursor = chat.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach {
                return it.alwaysTranslateBeforeSend
            }
            return false
        }

        @JvmStatic
        fun setTranslateBeforeSend(chatId: Long, value: Boolean) = UIUtil.runOnIoDispatcher {
            if ((Build.VERSION.SDK_INT < 26) || (chat == null)) return@runOnIoDispatcher
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
            if ((Build.VERSION.SDK_INT < 26) || (ccTarget == null)) return null
            val cursor = ccTarget.find(FluentFilter.where("chatId").eq(chatId))
            cursor.forEach { return it as String? }
            return default
//            return ccTarget.find(FluentFilter.where("chatId").eq(chatId)).firstOrDefault()?.ccTarget
//                    ?: default

        }

        @JvmStatic
        fun saveChatCCTarget(chatId: Long, target: String) = UIUtil.runOnIoDispatcher {
            if ((Build.VERSION.SDK_INT < 26) || (ccTarget == null)) return@runOnIoDispatcher
            ccTarget.update(ChatCCTarget(chatId, target), true)

        }

        @JvmStatic
        fun currentTarget(): TranslateDb? {
            if (Build.VERSION.SDK_INT < 26) return null
            return MomoConfig.translateToLang.String()?.transDbByCode
                ?: LocaleController.getInstance().currentLocale.transDb
        }

        @JvmStatic
        fun currentTargetLocale(): Locale {
            return MomoConfig.translateToLang.String()?.code2Locale
                ?: LocaleController.getInstance().currentLocale
        }

        @JvmStatic
        fun forLocale(locale: Locale): TranslateDb? {
            if (Build.VERSION.SDK_INT < 26) return null
            return locale.transDb
        }

        @JvmStatic
        fun currentInputTarget() = MomoConfig.translateInputLang.String().transDbByCode

        @JvmStatic
        fun clearAll() {
            if (Build.VERSION.SDK_INT < 26 && TranslateDb::fallbackPrefs.isInitialized) {
                initFallbackPrefs()
                fallbackPrefs.edit { clear() }
            }

            if (db == null) return
            db.listRepositories()
                    .filter { it  != "chat" }
                    .map { db.getCollection(it) }
                    .forEach { it.drop() }

            repo.clear()

        }

        @JvmStatic
        private fun initFallbackPrefs() {
            if (Build.VERSION.SDK_INT >= 26 || TranslateDb::fallbackPrefs.isInitialized) return
            fallbackPrefs = LaunchActivity.instance.getSharedPreferences("nitrites_fallback", Context.MODE_PRIVATE)
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
        if (MomoConfig.ignoreTranslatorCache.Bool() || Build.VERSION.SDK_INT < 26) return null
        val cursor = conn?.find(FluentFilter.where("text").eq(text))
        cursor?.forEach { return it?.trans }
        return null // conn.find(FluentFilter.where("text").eq(text)).firstOrDefault()?.trans
    }

}