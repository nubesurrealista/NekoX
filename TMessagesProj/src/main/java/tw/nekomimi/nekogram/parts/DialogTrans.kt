package tw.nekomimi.nekogram.parts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tw.nekomimi.nekogram.transtale.TranslateDb
import tw.nekomimi.nekogram.transtale.Translator
import tw.nekomimi.nekogram.utils.AlertUtil
import tw.nekomimi.nekogram.utils.UIUtil
import tw.nekomimi.nekogram.utils.uDismiss
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean

fun startTrans(ctx: Context, text: String) {

    val dialog = AlertUtil.showProgress(ctx)

    val canceled = AtomicBoolean(false)

    dialog.setOnCancelListener {

        canceled.set(true)

    }

    dialog.show()

    fun update(message: String) {

        UIUtil.runOnUIThread(Runnable { dialog.setMessage(message) })

    }

    GlobalScope.launch(Dispatchers.IO) {

        val target = TranslateDb.currentTarget() ?: translatedTexts[TranslateDb.currentTargetLocale()]

        if ( if (target is TranslateDb) target.contains(text) else (target as HashMap<*, *>).contains(text) ) {

            val result = if (target is TranslateDb) target.query(text) else (target as HashMap<*, *>)[text]

            dialog.uDismiss()

            AlertUtil.showCopyAlert(ctx, (result as String?) ?: "")

            return@launch

        }

        runCatching {

            val result = Translator.translate(TranslateDb.currentTargetLocale(), text)

            if (!canceled.get()) {

                dialog.uDismiss()

                AlertUtil.showCopyAlert(ctx, result)

            }

        }.onFailure {
            Log.e("nx-trans", "error occurred when translating", it)

            dialog.uDismiss()

            if (!canceled.get()) {

                AlertUtil.showTransFailedDialog(ctx, it is UnsupportedOperationException, it.message
                        ?: it.javaClass.simpleName, it) {

                    startTrans(ctx, text)

                }

            }

        }

    }

}