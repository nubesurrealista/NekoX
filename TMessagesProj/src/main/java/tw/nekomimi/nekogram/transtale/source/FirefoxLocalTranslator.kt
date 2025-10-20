package tw.nekomimi.nekogram.transtale.source

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.InternalCoroutinesApi
import org.telegram.ui.LaunchActivity
import tw.nekomimi.nekogram.transtale.Translator
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

import dev.davidv.translator.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import java.util.concurrent.atomic.AtomicBoolean

object FirefoxLocalTranslator : Translator {

    @OptIn(InternalCoroutinesApi::class)
    override suspend fun doTranslate(from: String, to: String, query: String): String {

        if (!isBound.get()) {
            bind(true)
        }
        val svc = translationService ?: throw RuntimeException(LocaleController.getString(R.string.FirefoxAidlSvcFailed))

        return suspendCoroutine {
            svc.translate(query, from, to, object : ITranslationCallback.Stub() {
                override fun onTranslationResult(translatedText: String) {
                    it.resume(translatedText)
                }

                override fun onTranslationError(errorMessage: TranslationError) {
                    Log.e("030-tx", "ff err: ${errorMessage.type} ${errorMessage.message}")
                    val msg =
                        if (errorMessage != null && !"null".equals(errorMessage.message))
                            "${ErrorEnum.from(errorMessage.type.toInt())!!.name} - ${errorMessage.message}"
                        else ErrorEnum.from(errorMessage.type.toInt())!!.name
                    it.resumeWithException(RuntimeException(msg))
                }
            })
        }
    }
     private val TAG = "TranslatorClient"

     /** The package name of the app providing the translation service. */
     private const val SERVICE_APP_PACKAGE = "dev.davidv.translator"

     private var translationService: ITranslationService? = null
     private val isBound = AtomicBoolean(false)
     private val sync = Mutex()

     private val connection = object : ServiceConnection {
         override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
             Log.d(TAG, "Service connected")
             translationService = ITranslationService.Stub.asInterface(service)
             isBound.set(true)
         }

         override fun onServiceDisconnected(name: ComponentName?) {
             Log.d(TAG, "Service disconnected, attempting to rebind.")
             translationService = null
             isBound.set(false)
             AndroidUtilities.runOnUIThread( {
                 CoroutineScope(Dispatchers.IO).launch {
                     bind(false)
                 }
             }, 500)
         }
     }

     suspend fun bind(block: Boolean) {
         if (isBound.get()) return
         sync.lock()
         val intent = Intent("dev.davidv.translator.ITranslationService")
         intent.setPackage(SERVICE_APP_PACKAGE)
         try {
             if (block) {
                 LaunchActivity.instance.applicationContext.awaitBindService(
                     intent,
                     Context.BIND_AUTO_CREATE
                 )
             } else {
                 LaunchActivity.instance.applicationContext.bindService(
                     intent,
                     connection,
                     Context.BIND_AUTO_CREATE
                 )
             }
         } catch (e: SecurityException) {
             Log.e(TAG, "Failed to bind to service. Is the other app installed and does it have the correct service declaration?", e)
         } finally {
             sync.unlock()
         }
     }

     fun unbind() {
         if (isBound.get()) {
             LaunchActivity.instance.unbindService(connection)
             isBound.set(false)
             translationService = null
         }
     }

    suspend fun Context.awaitBindService(intent: Intent, flags: Int = Context.BIND_AUTO_CREATE): IBinder =
        suspendCancellableCoroutine { cont ->
            if (!bindService(intent, connection, flags)) {
                cont.resumeWithException(IllegalStateException("bindService returned false"))
            }

            // Unbind automatically if coroutine is cancelled
            cont.invokeOnCancellation { unbindService(connection) }
        }

    enum class ErrorEnum(val value: Int) {
        COULD_NOT_DETECT_LANGUAGE(ErrorType.COULD_NOT_DETECT_LANGUAGE.toInt()),
        DETECTED_BUT_UNAVAILABLE(ErrorType.DETECTED_BUT_UNAVAILABLE.toInt()),
        UNEXPECTED(ErrorType.UNEXPECTED.toInt());

        companion object {
            fun from(value: Int): ErrorEnum? =
                values().find { it.value == value }
        }
    }

}
