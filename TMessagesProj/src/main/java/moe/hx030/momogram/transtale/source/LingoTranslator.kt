package moe.hx030.momogram.transtale.source

import android.os.SystemClock
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import moe.hx030.momogram.transtale.Translator
import moe.hx030.momogram.transtale.Translator.Companion.httpClient
import moe.hx030.momogram.utils.applyUserAgent

object LingoTranslator : Translator {

    override suspend fun doTranslate(from: String, to: String, query: String): String {

        if (to !in listOf("zh", "en", "es", "fr", "ja", "ru")) {

            error(LocaleController.getString(R.string.TranslateApiUnsupported))

        }

        val response = httpClient.newCall(Request.Builder().url("https://api.interpreter.caiyunai.com/v1/translator")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-Authorization", "token 9sdftiq37bnv410eon2l") // 白嫖
                .applyUserAgent()
                .post(FormBody.Builder().apply {
                    add("source", query)
                    add("trans_type", "${from}2$to")
                    add("request_id", SystemClock.elapsedRealtime().toString())
                    add("detect", "true")
                }.build())
            .build()
        ).execute()

        if (response.code != 200) {

            error("HTTP ${response.code} : ${response.body.string()}")

        }

        return JSONObject(response.body.string()).getString("target")

    }

}