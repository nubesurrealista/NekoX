package moe.hx030.momogram.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.NumberPicker
import moe.hx030.momogram.ui.BottomBuilder
import moe.hx030.momogram.ui.PopupBuilder
import moe.hx030.momogram.MomoConfig
import org.apache.commons.lang3.StringUtils
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.UserObject
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.AvatarDrawable
import org.telegram.ui.Components.BackupImageView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.LaunchActivity
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object AlertUtil {

    @JvmStatic
    fun copyAndAlert(text: String) {

        AndroidUtilities.addToClipboard(text)

        AlertUtil.showToast(LocaleController.getString(R.string.TextCopied))

    }

    @JvmStatic
    fun copyLinkAndAlert(text: String) {

        AndroidUtilities.addToClipboard(text)

        AlertUtil.showToast(LocaleController.getString(R.string.LinkCopied))

    }

    @JvmStatic
    fun call(number: String) {

        runCatching {

            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+" + number))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ApplicationLoader.applicationContext.startActivity(intent)

        }.onFailure {

            showToast(it)

        }

    }

    @JvmStatic
    fun showToast(e: Throwable) = showToast(e.message ?: e.javaClass.simpleName)

    @JvmStatic
    fun showToast(e: TLRPC.TL_error?) {
        if (e == null) return
        showToast("${e.code}: ${e.text}")
    }

    @JvmStatic
    fun showToast(text: String) = UIUtil.runOnUIThread(Runnable {
        Toast.makeText(
                ApplicationLoader.applicationContext,
                text.takeIf { it.isNotBlank() }
                        ?: "喵 !",
                Toast.LENGTH_LONG
        ).show()
    })

    @JvmStatic
    fun showSimpleAlert(ctx: Context?, error: Throwable) {

        showSimpleAlert(ctx, null, error.message ?: error.javaClass.simpleName)

    }

    @JvmStatic
    @JvmOverloads
    fun showSimpleAlert(ctx: Context?, text: String, listener: ((AlertDialog.Builder) -> Unit)? = null) {

        showSimpleAlert(ctx, null, text, listener)

    }

    @JvmStatic
    @JvmOverloads
    fun showSimpleAlert(ctx: Context?, title: String?, text: String, listener: ((AlertDialog.Builder) -> Unit)? = null) = UIUtil.runOnUIThread(Runnable {

        if (ctx == null) return@Runnable

        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(title ?: StrUtil.getAppName())
        builder.setMessage(text)

        builder.setPositiveButton(LocaleController.getString(R.string.OK)) { _, _ ->

            builder.dismissRunnable?.run()
            listener?.invoke(builder)

        }

        builder.show()

    })

    @JvmStatic
    fun showCopyAlert(ctx: Context, text: String) = UIUtil.runOnUIThread(Runnable {

        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(LocaleController.getString(R.string.Translate))
        builder.setMessage(text)

        builder.setNegativeButton(LocaleController.getString(R.string.Copy)) { _, _ ->

            AndroidUtilities.addToClipboard(text)

            AlertUtil.showToast(LocaleController.getString(R.string.TextCopied))

            builder.dismissRunnable.run()

        }

        builder.setPositiveButton(LocaleController.getString(R.string.OK)) { _, _ ->

            builder.dismissRunnable.run()

        }

        builder.show()

    })

    @JvmOverloads
    @JvmStatic
    fun showProgress(ctx: Context, text: String = LocaleController.getString(R.string.Loading)): AlertDialog {

        return AlertDialog.Builder(ctx, AlertDialog.ALERT_TYPE_MESSAGE).apply {

            setMessage(text)

        }.create()

    }

    fun showInput(ctx: Context, title: String, hint: String, onInput: (AlertDialog.Builder, String) -> String) = UIUtil.runOnUIThread(Runnable {

        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(title)

        builder.setView(EditTextBoldCursor(ctx).apply {

            setHintText(hint)

        })

    })

    @JvmStatic
    @JvmOverloads
    fun showConfirm(ctx: Context, title: String, text: String? = null, icon: Int, button: String, red: Boolean, listener: Runnable) = UIUtil.runOnUIThread(Runnable {

        /*

        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(title)

        builder.setMessage(AndroidUtilities.replaceTags(text))
        builder.setPositiveButton(button, listener)

        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null)
        val alertDialog = builder.show()

        if (red) {

            (alertDialog.getButton(DialogInterface.BUTTON_POSITIVE) as TextView?)?.setTextColor(Theme.getColor(Theme.key_dialogTextRed2))

        }

         */

        val builder = BottomBuilder(ctx)

        if (text != null) {

            builder.addTitle(title, text)

        } else {

            builder.addTitle(title)

        }

        builder.addItem(button, icon, red) {

            listener.run()

        }

        builder.addCancelItem()

        builder.show()

    })

    @JvmStatic
    fun showTransFailedDialog(ctx: Context, noRetry: Boolean, message: String, exception: Throwable?, retryRunnable: Runnable) = UIUtil.runOnUIThread(Runnable {
        if (exception != null) {
            Log.e("nx-trans-failed", "exception occurred..", exception)
        } else {
            Log.e("nx-trans-failed", "error occurred, $message")
        }
        ctx.setTheme(R.style.Theme_TMessages)

        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(LocaleController.getString(R.string.TranslateFailed))

        builder.setMessage(message)

        val reference = AtomicReference<AlertDialog>()

        builder.setNeutralButton(LocaleController.getString(R.string.ChangeTranslateProvider)) {

            _, _ ->

            val view = reference.get().getButton(AlertDialog.BUTTON_NEUTRAL)

            val popup = PopupBuilder(view, true)

            val items = LinkedList<String>()

            items.addAll(arrayOf(
                    LocaleController.getString(R.string.ProviderGoogleTranslate),
                    LocaleController.getString(R.string.ProviderGoogleTranslateCN),
                    LocaleController.getString(R.string.ProviderYandexTranslate),
                    LocaleController.getString(R.string.ProviderLingocloud),
                    LocaleController.getString(R.string.ProviderMicrosoftTranslator),
                    LocaleController.getString(R.string.ProviderYouDao),
                    LocaleController.getString(R.string.ProviderDeepLXTranslate),
                    LocaleController.getString(R.string.ProviderTelegramAPI),
                    LocaleController.getString(R.string.ProviderLingva),
                    LocaleController.getString(R.string.ProviderFirefox)
            ))

            popup.setItems(items.toTypedArray()) { item, _ ->

                reference.get().dismiss()

                MomoConfig.translationProvider.setConfigInt(item + 1)

                retryRunnable.run()

            }

            popup.show()

        }

        if (noRetry) {

            builder.setPositiveButton(LocaleController.getString(R.string.Cancel)) { _, _ ->

                reference.get().dismiss()

            }

        } else {

            builder.setPositiveButton(LocaleController.getString(R.string.Retry)) { _, _ ->

                reference.get().dismiss()

                retryRunnable.run()

            }

            builder.setNegativeButton(LocaleController.getString(R.string.Cancel)) { _, _ ->

                reference.get().dismiss()

            }

        }

        reference.set(builder.create().apply {

            setDismissDialogByButtons(false)
            show()

        })

    })

    fun showTimePicker(ctx: Context, title: String, callback: (Long) -> Unit) {

        ctx.setTheme(R.style.Theme_TMessages)

        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(title)

        builder.setView(LinearLayout(ctx).apply {

            orientation = LinearLayout.HORIZONTAL

            addView(NumberPicker(ctx).apply {

                minValue = 0
                maxValue = 60

            }, LinearLayout.LayoutParams(-2, -2).apply {

                weight = 1F

            })

            addView(NumberPicker(ctx).apply {

                minValue = 0
                maxValue = 60

            }, LinearLayout.LayoutParams(-2, -2).apply {

                weight = 1F

            })

        })

    }

    @JvmStatic
    fun showSecretChatConfirmation(user: TLRPC.User?, accept: Runnable) {
        val fragment = LaunchActivity.getSafeLastFragment()
        val context = fragment.parentActivity
        val userButton = FrameLayout(context)
        userButton.setBackground(
            Theme.createRadSelectorDrawable(
                Theme.getColor(
                    Theme.key_listSelector,
                    fragment.resourceProvider
                ), 0, 12
            )
        )

        val imageView = BackupImageView(context)
        imageView.setRoundRadius(dp(17F))
        val avatarDrawable = AvatarDrawable()
        avatarDrawable.setInfo(user)
        imageView.setForUserOrChat(user, avatarDrawable)
        userButton.addView(
            imageView,
            LayoutHelper.createFrame(
                34F,
                34F,
                Gravity.LEFT or Gravity.CENTER_VERTICAL,
                13F,
                0F,
                0F,
                0F
            )
        )

        val titleText = TextView(context)
        titleText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, fragment.resourceProvider))
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16F)
        titleText.ellipsize = TextUtils.TruncateAt.END
        titleText.isSingleLine = true
        titleText.text = if (user == null) LocaleController.getString(R.string.UnknownUser) else UserObject.getUserName(user)
        userButton.addView(
            titleText, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT.toFloat(),
                Gravity.FILL_HORIZONTAL or Gravity.TOP, 59F, 6F, 16F, 0F
            )
        )

        val subtitleText = TextView(context)
        subtitleText.setTextColor(
            Theme.getColor(
                Theme.key_dialogTextGray2,
                fragment.resourceProvider
            )
        )
        subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13F)
        subtitleText.text = UserObject.getPublicUsername(user)
        if (StringUtils.isBlank(subtitleText.text)) subtitleText.text = "N/A"
        else subtitleText.text = "@${subtitleText.text}"
        userButton.addView(
            subtitleText, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT.toFloat(),
                Gravity.FILL_HORIZONTAL or Gravity.TOP, 59F, 27F, 16F, 0F
            )
        )

        val builder = AlertDialog.Builder(LaunchActivity.instance)
        builder.setTitle(LocaleController.getString(R.string.NewSecretChat))
        builder.setView(userButton)
        builder.setPositiveButton(LocaleController.getString(R.string.Accept)) { dlg, w -> accept.run() }
        builder.setNegativeButton(LocaleController.getString(R.string.Ignore), null)
        builder.show()
    }

}