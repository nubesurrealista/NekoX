package tw.nekomimi.nekogram.transtale.source

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.NekoConfig
import tw.nekomimi.nekogram.transtale.Translator
import tw.nekomimi.nekogram.transtale.Translator.Companion.httpClient

object DeepLXTranslator : Translator {

    var defaultUrl = "https://dplx.xi-xu.me/deepl" // credit: https://github.com/xixu-me/DeepLX
    val targetLanguages = listOf("DE", "EN", "ES", "FR", "IT", "JA", "NL", "PL", "PT", "RU", "ZH")

    // val client = DeepLTranslatorRaw()

    override suspend fun doTranslate(from: String, to: String, query: String): String {

        if (to !in targetLanguages) {

            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))

        }

        var url = NekoConfig.customDeepLXInstance.String()
        if (url.isNullOrBlank()) url = defaultUrl

        val body = JSONObject()
        body.put("text", query)
        body.put("target_lang", to)
        body.put("source_lang", from)

        var req = Request.Builder()
            .header("Content-Type", "application/json")
            .url(url)
            .apply {
                post(body.toString().toRequestBody("application/json".toMediaType()))
            }

        val response = httpClient.newCall(req.build()).execute()

        if (response.code != 200) {

            error("HTTP ${response.code} : ${response.body.string()}")

        }

        var respObj = JSONObject(response.body.string())

        if (respObj.isNull("data")) error(respObj.toString(4))

        return respObj.getString("data")
    }
}
