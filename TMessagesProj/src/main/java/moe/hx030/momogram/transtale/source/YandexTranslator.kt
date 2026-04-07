package moe.hx030.momogram.transtale.source

import android.os.Build
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.telegram.messenger.FileLog
import moe.hx030.momogram.transtale.Translator
import moe.hx030.momogram.transtale.Translator.Companion.httpClient
import moe.hx030.momogram.utils.TelegramUtil
import moe.hx030.momogram.utils.applyIf
import moe.hx030.momogram.utils.applyIfNot
import moe.hx030.momogram.utils.applyUserAgent
import java.net.URLEncoder
import java.util.UUID

object YandexTranslator : Translator {

    val uuid = UUID.randomUUID().toString().replace("-", "")

    override suspend fun doTranslate(from: String, to: String, query: String): String {

        val uuid2 = UUID.randomUUID().toString().replace("-", "")

        val req = Request.Builder()
            .url("https://translate.yandex.net/api/v1/tr.json/translate?srv=android&uuid=$uuid&id=$uuid2-9-0")
            .applyUserAgent()
            .apply {
                val formBody = FormBody.Builder()
                    .add("text", query)
                    .add("lang", if (from == "auto") to else "$from-$to")
                    .build()

                post(formBody)
            }

        val response = httpClient.newCall(req.build()).execute()

        if (!response.isSuccessful) {
            val err = "HTTP ${response.code} : ${response.body.string()}"
            FileLog.e("Yandex translator error: $err, query: $query, stack: ${TelegramUtil.getStackTraceAsString(null)}")
            error(err)
        }

        val respObj = JSONObject(response.body.string())

        if (respObj.optInt("code", -1) != 200) error(respObj.toString(4))

        return respObj.getJSONArray("text").getString(0)

    }

}