package moe.hx030.momogram.transtale.source

import okhttp3.Request
import org.json.JSONObject
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.transtale.Translator
import moe.hx030.momogram.transtale.Translator.Companion.httpClient
import java.net.URLEncoder

object LingvaTranslator : Translator {

    var randomPathMap = hashMapOf<String, String>()

    override suspend fun doTranslate(from: String, to: String, query: String): String {
        val processedQuery = URLEncoder.encode(query, "utf-8").replace("+", " ")
        val instance = MomoConfig.customLingvaInstance.String().trim('/')
        if (instance.isNullOrBlank()) error(LocaleController.getString(R.string.LingvaInstanceNotConfigured))

        if (!randomPathMap.containsKey(instance)) {
            val get = Request.Builder().url(instance).build()
            val res = httpClient.newCall(get).execute()
            if (!res.isSuccessful) {
                error("ERROR ${res.code}: ${res.body.string()}")
            }
            try {
                val path = res.body.string().split("/_buildManifest.js")[0].split("/").last()
                randomPathMap[instance] = path
            } catch (ex: Exception) {
                FileLog.e("lingva err: failed to get path for translation api", ex)
                error(ex)
            }
        }

        val path = randomPathMap[instance]

        var req = Request.Builder().url("$instance/_next/data/$path/$from/$to/${processedQuery}.json").build()

        val res = httpClient.newCall(req).execute()

        if (res.code != 200) {
            FileLog.e("lingva err: $instance/_next/data/$path/$from/$to/${processedQuery}.json")
            FileLog.e("lingva err: HTTP ${res.code} : ${res.body.string()}")
            error("HTTP ${res.code} : ${res.body.string()}")
        }

        val respObj = JSONObject(res.body.string())

        if (respObj.optString("error", "").isNotBlank()) {
            FileLog.e("lingva err: " + respObj.toString(2))
            error(respObj.toString(4))
        }

        return respObj.getJSONObject("pageProps").getString("translation")

    }

}