package moe.hx030.momogram.transtale.source

import android.os.Build
import okhttp3.FormBody
import okhttp3.Request
import org.apache.commons.lang3.StringUtils
import org.json.JSONObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.transtale.Translator
import moe.hx030.momogram.transtale.Translator.Companion.httpClient

object GoogleCloudTranslator : Translator {

    override suspend fun doTranslate(from: String, to: String, query: String): String {

        if (to !in targetLanguages) {

            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))

        }

        if (StringUtils.isBlank(MomoConfig.googleCloudTranslateKey.String())) error("Missing Cloud Translate Key")


        val sdk25up = Build.VERSION.SDK_INT > 25
        var req = Request.Builder()
            .url("https://translation.googleapis.com/language/translate/v2")
            .apply {
                post(FormBody.Builder()
                    .add("q", query)
                    .add("target", to)
                    .add("format", "text")
                    .add("key", MomoConfig.googleCloudTranslateKey.String())
                    .apply {
                        if (from != "auto") add("source", from)
                }.build())
            }

        val response = httpClient.newCall(req.build()).execute()

        if (response.code != 200) {

            error("HTTP ${response.code} : ${response.body.string()}")

        }

        var respObj = JSONObject(response.body.string())

        if (respObj.isNull("data")) error(respObj.toString(4))

        respObj = respObj.getJSONObject("data")

        val respArr = respObj.getJSONArray("translations")

        if (respArr.length() == 0) error("Empty translation result")

        return respArr.getJSONObject(0).getString("translatedText")

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