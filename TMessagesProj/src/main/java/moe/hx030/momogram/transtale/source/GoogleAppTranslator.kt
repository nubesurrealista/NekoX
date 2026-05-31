package moe.hx030.momogram.transtale.source

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Request
import org.apache.commons.lang3.StringUtils
import org.json.JSONObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.transtale.TransUtils
import moe.hx030.momogram.transtale.Translator
import moe.hx030.momogram.transtale.Translator.Companion.httpClient
import org.telegram.messenger.TranslateController
import org.telegram.messenger.Utilities
import org.telegram.ui.Components.TranslateAlert2

object GoogleAppTranslator : Translator {

    override suspend fun doTranslate(from: String, to: String, query: String): String {

        if (MomoConfig.translationProvider.Int() != 2 && StringUtils.isNotBlank(
                MomoConfig.googleCloudTranslateKey.String())) return GoogleCloudTranslator.doTranslate(from, to, query)

        if (to !in targetLanguages) {

            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))

        }

        val altResult: String = suspendCancellableCoroutine { cont ->
            TranslateAlert2.alternativeTranslate(query, from, to) { s, err ->
                if (!err) cont.resumeWith(Result.success(s))
                else cont.resumeWith(Result.success(""))
            }
        }
        if (!StringUtils.isBlank(altResult)) return altResult

        val url = "https://translate.google." + (if (MomoConfig.translationProvider.Int() == 2) "cn" else "com") + "/translate_a/single?dj=1" +
                "&q=" + TransUtils.encodeURIComponent(query) +
                "&sl=auto" +
                "&tl=" + to +
                "&ie=UTF-8&oe=UTF-8&client=at&dt=t&otf=2"

        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "GoogleTranslate/6.14.0.04.343003216 (Linux; U; Android 10; Redmi K20 Pro)")

        val response = httpClient.newCall(req.build()).execute()

        if (response.code != 200) {

            error("HTTP ${response.code} : ${response.body.string()}")

        }

        val result = StringBuilder()

        val array = JSONObject(response.body.string()).getJSONArray("sentences")
        for (index in 0 until array.length()) {
            result.append(array.getJSONObject(index).getString("trans"))
        }

        return result.toString()
    }

    private val targetLanguages = listOf(
            "sq", "ar", "am", "az", "ga", "et", "eu", "be", "bg", "is", "pl", "bs", "fa",
            "af", "da", "de", "ru", "fr", "tl", "fi", "fy", "km", "ka", "gu", "kk", "ht",
            "ko", "ha", "nl", "ky", "gl", "ca", "cs", "kn", "co", "hr", "ku", "la", "lv",
            "lo", "lt", "lb", "ro", "mg", "mt", "mr", "ml", "ms", "mk", "mi", "mn", "bn",
            "my", "hmn", "xh", "zu", "ne", "no", "pa", "pt", "ps", "ny", "ja", "sv", "sm",
            "sr", "st", "si", "eo", "sk", "sl", "sw", "gd", "ceb", "so", "tg", "te", "ta",
            "th", "tr", "cy", "ur", "uk", "uz", "es", "iw", "el", "haw", "sd", "hu", "sn",
            "hy", "ig", "it", "yi", "hi", "su", "id", "jw", "en", "yo", "vi", "zh-TW", "zh-CN", "zh")
}
