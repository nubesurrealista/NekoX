package tw.nekomimi.nekogram.transtale.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.transtale.Translator
import tw.nekomimi.nekogram.transtale.deepl.DeepLTranslatorRaw

object DeepLTranslator : Translator {

    val targetLanguages = listOf(
        "AR", "BG", "CS", "DA", "DE", "EL", "EN", "ES", "ET", "FI", "FR",
        "HE", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL",
        "PT", "RO", "RU", "SK", "SL", "SV", "TH", "TR", "UK", "VI", "ZH"
    )

    val client = DeepLTranslatorRaw()

    override suspend fun doTranslate(from: String, to: String, query: String): String {
        if (to !in targetLanguages) {
            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))
        }

        return withContext(Dispatchers.IO) {
            client.translate(query, if (from.isEmpty()) "AUTO" else from, to)
        }
    }
}
