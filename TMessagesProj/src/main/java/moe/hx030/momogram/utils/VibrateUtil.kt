package moe.hx030.momogram.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import org.telegram.messenger.ApplicationLoader
import moe.hx030.momogram.MomoConfig

object VibrateUtil {

    lateinit var vibrator: Vibrator

    @JvmStatic
    @JvmOverloads
    fun vibrate(time: Long = 200L) {

        if (MomoConfig.disableVibration.Bool()) return

        if (!::vibrator.isInitialized) {

            vibrator = ApplicationLoader.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        }

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            runCatching {

                val effect = VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE)

                vibrator.vibrate(effect, null)

            }

        } else {

            runCatching {

                vibrator.vibrate(time)

            }

        }

    }

    @JvmStatic
    fun disableHapticFeedback(view: View?) {

        if (view == null) return

        view.isHapticFeedbackEnabled = false

        (view as? ViewGroup)?.children?.forEach(::disableHapticFeedback)

    }

}