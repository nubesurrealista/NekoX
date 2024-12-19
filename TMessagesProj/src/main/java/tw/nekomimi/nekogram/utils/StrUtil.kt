package tw.nekomimi.nekogram.utils

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.URLSpanNoUnderline
import tw.nekomimi.nekogram.NekoConfig
import java.util.regex.Matcher
import java.util.regex.Pattern

object StrUtil {

    private val urlPattern = Pattern.compile("@[a-zA-Z\\d_]{1,32}")

    @JvmStatic
    fun setText(fragment: BaseFragment?, textView: TextView, text: String) {

        var stringBuilder: SpannableStringBuilder? = null

        if (fragment != null) {

            try {
                val matcher: Matcher = urlPattern.matcher(text)
                while (matcher.find()) {
                    if (stringBuilder == null) {
                        stringBuilder = SpannableStringBuilder(text)
                        textView.movementMethod = AndroidUtilities.LinkMovementMethodMy()
                    }
                    var start = matcher.start()
                    val end = matcher.end()
                    if (text.get(start) != '@') {
                        start++
                    }
                    val url: URLSpanNoUnderline = object : URLSpanNoUnderline(text.subSequence(start + 1, end).toString()) {
                        override fun onClick(widget: View) {
                            fragment.messagesController.openByUserName(url, fragment, 1)
                        }
                    }
                    stringBuilder.setSpan(url, start, end, 0)
                }
            } catch (e: Exception) {
                FileLog.e(e)
            }
        }

        textView.text = stringBuilder ?: text
    }

    @JvmStatic
    fun getSubString(text: String, left: String?, right: String?): String {
        var llen: Int
        if (left == null || left.isEmpty()) {
            llen = 0
        } else {
            llen = text.indexOf(left)
            if (llen > -1) {
                llen += left.length
            } else {
                llen = 0
            }
        }
        var rlen = text.indexOf(right!!, llen)
        if (rlen < 0 || right.isEmpty()) {
            rlen = text.length
        }
        return text.substring(llen, rlen)
    }

    @JvmStatic
    fun get030Tag(obj: Any): String {
        return "030-${obj.javaClass.simpleName}"
    }

    @JvmStatic
    fun getAppName(): String {
        if (NekoConfig.useOldName.Bool())
            return LocaleController.getString(R.string.NekoX)

        return LocaleController.getString(R.string.Momogram)
    }

    @JvmStatic
    fun getShortAppName(): String {
        if (NekoConfig.useOldName.Bool())
            return LocaleController.getString(R.string.AppNameShort)

        return LocaleController.getString(R.string.Momogram)
    }

}