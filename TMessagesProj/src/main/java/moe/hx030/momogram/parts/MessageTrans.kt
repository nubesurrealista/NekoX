package moe.hx030.momogram.parts

import android.util.Log
import kotlinx.coroutines.*
import org.telegram.messenger.MessageObject
import org.telegram.messenger.TranslateController
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ChatActivity
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.transtale.TranslateDb
import moe.hx030.momogram.transtale.Translator
import moe.hx030.momogram.transtale.code2Locale
import moe.hx030.momogram.utils.AlertUtil
import moe.hx030.momogram.utils.UIUtil
import moe.hx030.momogram.utils.uDismiss
import moe.hx030.momogram.utils.uUpdate
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

fun MessageObject.toRawString(): String {

    var content: String

    if (messageOwner.media is TLRPC.TL_messageMediaPoll) {

        val poll = (messageOwner.media as TLRPC.TL_messageMediaPoll).poll
        content = poll.question.text
        content += "\n"

        for (answer in poll.answers) {

            content += "\n- "
            content += answer.text

        }

    } else {

        content =
            if (messageOwner.decrypted) messageOwner.decryptedMessage
            else messageOwner.message

    }

    return content

}

val STATUS_UNFINISHED = 0
val STATUS_SKIPPED_NO_TEXT = 1
val STATUS_DONE = 2

fun MessageObject.translateFinished(locale: Locale): Int {

    val db = TranslateDb.forLocale(locale)
    val map = if (db != null) null else translatedTexts

    val hideOriginalText = MomoConfig.hideOriginalTextAfterTranslate.Bool()

    if (isPoll) {

        val pool = (messageOwner.media as TLRPC.TL_messageMediaPoll).poll

        val question = map?.get(locale)?.get(pool.question.text) ?: db?.query(pool.question.text)
        if (question == null) return STATUS_UNFINISHED

        pool.translatedQuestion = if (hideOriginalText) question else (pool.question.text + "\n\n--------\n\n" + question)

        val translatedPoll = TranslateController.PollText()
        translatedPoll.question = pool.translatedQuestion.toTextWithEntities()
        translatedPoll.answers = ArrayList()

        pool.answers.forEach {

            val answer = map?.get(locale)?.get(it.text.text) ?: db?.query(it.text.text)
            if (answer == null) return STATUS_UNFINISHED

            it.translatedText = if (hideOriginalText) answer else (answer + " | " + it.text.text)

            val translatedAns = TLRPC.PollAnswer()
            translatedAns.translatedText = it.translatedText
            translatedAns.text = it.translatedText.toTextWithEntities()
            translatedAns.option = it.option
            translatedAns.unshuffled_index = it.unshuffled_index
            translatedPoll.answers.add(translatedAns)

        }
        translated = true
        messageOwner.translatedPoll = translatedPoll
    } else if (isTodo) {
        val todo = (messageOwner.media as TLRPC.TL_messageMediaToDo).todo
        val title = map?.get(locale)?.get(todo.title.text) ?: db?.query(todo.title.text)
        if (title == null) return STATUS_UNFINISHED

        todo.translatedTitle = if (hideOriginalText) title.toTextWithEntities() else (title + "\n\n--------\n\n" + todo.title.text).toTextWithEntities()

        val translatedPoll = TranslateController.PollText()
        translatedPoll.question = todo.translatedTitle
        translatedPoll.answers = ArrayList()

        todo.list.forEach {

            val answer = map?.get(locale)?.get(it.title.text) ?: db?.query(it.title.text)
            if (answer == null) return STATUS_UNFINISHED

            it.translatedTitle = if (hideOriginalText) answer.toTextWithEntities() else (answer + " | " + it.title.text).toTextWithEntities()

            val translatedAns = TLRPC.PollAnswer()
            translatedAns.translatedText = it.translatedTitle.text
            translatedAns.text = it.translatedTitle
            translatedAns.option = ByteArray(1)
            translatedAns.option[0] = it.id.toByte()
            translatedPoll.answers.add(translatedAns)

        }
        translated = true
        messageOwner.translatedPoll = translatedPoll
    } else {

        var originalText =
            if (messageOwner.decrypted) messageOwner.decryptedMessage
            else messageOwner.message

        val text = map?.get(locale)?.get(originalText)
            ?: Translator.maybeStripCOT(db?.query(originalText.takeIf { !it.isNullOrBlank() } ?: return STATUS_SKIPPED_NO_TEXT))
            ?: return STATUS_UNFINISHED

        messageOwner.translatedMessage =
            if (hideOriginalText) text
            else "$originalText\n\n--------\n\n$text"

    }

    return STATUS_DONE

}

@JvmName("translateMessages")
fun ChatActivity.translateMessages1() = translateMessages()
@JvmName("translateMessages")
fun ChatActivity.translateMessagesCb(callback: Runnable?) = translateMessages(callback = callback)

@JvmName("translateMessages")
fun ChatActivity.translateMessages2(target: Locale) = translateMessages(target)

@JvmName("translateMessages")
fun ChatActivity.translateMessages3(messages: List<MessageObject>) = translateMessages(messages = messages)

val translatedTexts = HashMap<Locale, HashMap<String, String>>()

fun ChatActivity.translateMessages(target: Locale = MomoConfig.translateToLang.String().code2Locale
                                   , messages: List<MessageObject> = messageForTranslate?.let { listOf(it) }
        ?: selectedObjectGroup?.messages
        ?: emptyList(), callback: Runnable? = null) {

    Log.d("nx-trans", Thread.currentThread().stackTrace.contentToString())

    // TODO: Fix file group

    if (messages.all { it.messageOwner.translated }) {

        messages.forEach { messageObject ->

            messageObject.messageOwner.translated = false

            if (messageObject.messageOwner.originalEntities != null) {
                messageObject.messageOwner.entities = messageObject.messageOwner.originalEntities
            }

            messageHelper.resetMessageContent(dialogId, messageObject)

        }

        return

    }

    val status = AlertUtil.showProgress(parentActivity)

    val cancel = AtomicBoolean()

    status.setOnCancelListener {
        cancel.set(true)
    }

    status.show()

    val deferreds = LinkedList<Deferred<Unit>>()
    val taskCount = AtomicInteger(messages.size)
    val transPool = newFixedThreadPoolContext(5, "Message Trans Pool")

    suspend fun next() {
        val index = taskCount.decrementAndGet()
        if (index == 0) {
            status.uDismiss()
            callback?.run()
        } else if (messages.size > 1) {
            status.uUpdate("${messages.size - index} / ${messages.size}")
        }
    }

    GlobalScope.launch(Dispatchers.IO) {

        messages.forEachIndexed { _, selectedObject ->

            val state = selectedObject.translateFinished(target)

            if (state == STATUS_SKIPPED_NO_TEXT) {
                next()
                return@forEachIndexed
            } else if (state == STATUS_DONE) {
                next()

                withContext(Dispatchers.Main) {
                    selectedObject.messageOwner.translated = true
                    messageHelper.resetMessageContent(dialogId, selectedObject)
                }

                return@forEachIndexed

            }

            deferreds.add(async(transPool) trans@{

                val db = TranslateDb.forLocale(target)
                val map = if (db != null) null else translatedTexts
                val hideOriginalText = MomoConfig.hideOriginalTextAfterTranslate.Bool()

                if (selectedObject.isPoll) {

                    val pool = (selectedObject.messageOwner.media as TLRPC.TL_messageMediaPoll).poll

                    var question = map?.get(target)?.get(pool.question.text) ?: db?.query(pool.question.text)

                    if (question == null) {

                        if (cancel.get()) return@trans

                        runCatching {

                            question = Translator.translate(target, pool.question.text)

                        }.onFailure {
                            Log.e("nx-trans", "error occurred when translating", it)

                            status.uDismiss()

                            val parentActivity = parentActivity

                            if (parentActivity != null && !cancel.get()) {

                                AlertUtil.showTransFailedDialog(parentActivity, it is UnsupportedOperationException, it.message
                                        ?: it.javaClass.simpleName, it) {

                                    translateMessages(target, messages)

                                }

                            }

                            return@trans

                        }

                    }

                    pool.translatedQuestion = if (hideOriginalText) question else (pool.question.text + "\n\n--------\n\n" + question)

                    val translatedPoll = TranslateController.PollText()
                    translatedPoll.question = pool.translatedQuestion.toTextWithEntities()
                    translatedPoll.answers = ArrayList()

                    pool.answers.forEach {

                        var answer = map?.get(target)?.get(it.text.text) ?: db?.query(it.text.text)

                        if (answer == null) {

                            if (cancel.get()) return@trans

                            runCatching {

                                answer = Translator.translate(target, it.text.text)

                            }.onFailure { e ->

                                status.uDismiss()

                                val parentActivity = parentActivity

                                if (parentActivity != null && !cancel.get()) {

                                    AlertUtil.showTransFailedDialog(parentActivity, e is UnsupportedOperationException, e.message
                                            ?: e.javaClass.simpleName, e) {

                                        translateMessages(target, messages)

                                    }

                                }

                                return@trans

                            }

                        }

                        val original = if (it is TLRPC.PollAnswer) it.text.text else it.text
                        it.translatedText = if (hideOriginalText) "$answer" else "$answer | $original"

                        val translatedAns = TLRPC.PollAnswer()
                        translatedAns.translatedText = it.translatedText
                        translatedAns.text = it.translatedText.toTextWithEntities()
                        translatedAns.option = it.option
                        translatedPoll.answers.add(translatedAns)
                    }
                    selectedObject.translated = true
                    selectedObject.messageOwner.translatedPoll = translatedPoll
                } else if (selectedObject.isTodo) {
                    val todo = (selectedObject.messageOwner.media as TLRPC.TL_messageMediaToDo).todo

                    var title = map?.get(target)?.get(todo.title.text) ?: db?.query(todo.title.text)

                    if (title == null) {

                        if (cancel.get()) return@trans

                        runCatching {

                            title = Translator.translate(target, todo.title.text)

                        }.onFailure {
                            Log.e("nx-trans", "error occurred when translating", it)

                            status.uDismiss()

                            val parentActivity = parentActivity

                            if (parentActivity != null && !cancel.get()) {

                                AlertUtil.showTransFailedDialog(parentActivity, it is UnsupportedOperationException, it.message
                                    ?: it.javaClass.simpleName, it) {

                                    translateMessages(target, messages)

                                }

                            }

                            return@trans

                        }

                    }

                    todo.translatedTitle = if (hideOriginalText) title!!.toTextWithEntities() else (todo.title.text + "\n\n--------\n\n" + title).toTextWithEntities()

                    val translatedPoll = TranslateController.PollText()
                    translatedPoll.question = todo.translatedTitle
                    translatedPoll.answers = ArrayList()

                    todo.list.forEach {

                        var item = map?.get(target)?.get(it.title.text) ?: db?.query(it.title.text)

                        if (item == null) {

                            if (cancel.get()) return@trans

                            runCatching {

                                item = Translator.translate(target, it.title.text)

                            }.onFailure { e ->

                                status.uDismiss()

                                val parentActivity = parentActivity

                                if (parentActivity != null && !cancel.get()) {

                                    AlertUtil.showTransFailedDialog(parentActivity, e is UnsupportedOperationException, e.message
                                        ?: e.javaClass.simpleName, e) {

                                        translateMessages(target, messages)

                                    }

                                }

                                return@trans

                            }

                        }

                        val original = if (it is TLRPC.TodoItem) it.title.text else ""
                        it.translatedTitle = if (hideOriginalText || original.isNullOrBlank()) "$item".toTextWithEntities() else "$item | $original".toTextWithEntities()

                        val translatedAns = TLRPC.PollAnswer()
                        translatedAns.translatedText = it.translatedTitle.text
                        translatedAns.text = it.translatedTitle
                        translatedAns.option = ByteArray(1)
                        translatedAns.option[0] = it.id.toByte()
                        translatedPoll.answers.add(translatedAns)
                    }
                    selectedObject.translated = true
                    selectedObject.messageOwner.translatedPoll = translatedPoll
                }

                if (selectedObject.messageOwner.decrypted or !StringUtils.isBlank(selectedObject.messageOwner.message)) {

                    var originalText =
                        if (selectedObject.messageOwner.decrypted)
                            selectedObject.messageOwner.decryptedMessage
                        else
                            selectedObject.messageOwner.message

                    var textWithEntities = TLRPC.TL_textWithEntities()
                    textWithEntities.text = originalText
                    textWithEntities.entities = selectedObject.messageOwner.entities

                    var state = TranslateController.preprocessEntities(textWithEntities)
                    originalText = state.maskedText

                    var text = map?.get(target)?.get(originalText) ?: db?.query(originalText)

                    if (text == null) {

                        runCatching {

                            text = Translator.translate(target, originalText)

                        }.onFailure {

                            status.uDismiss()

                            val parentActivity = parentActivity

                            if (parentActivity != null && !cancel.get()) {

                                AlertUtil.showTransFailedDialog(parentActivity, it is UnsupportedOperationException, it.message
                                        ?: it.javaClass.simpleName, it) {

                                    translateMessages(target, messages)

                                }

                            }

                            return@trans

                        }


                    }
                    textWithEntities = TranslateController.postprocessEntities(text, state)

                    if (MomoConfig.hideOriginalTextAfterTranslate.Bool()) {
                        selectedObject.messageOwner.originalEntities = selectedObject.messageOwner.entities
                        selectedObject.messageOwner.translatedEntities = textWithEntities.entities
                        selectedObject.messageOwner.entities = textWithEntities.entities
                    }

                    selectedObject.messageOwner.translatedMessage =
                        if (MomoConfig.hideOriginalTextAfterTranslate.Bool()) textWithEntities.text
                        else "$originalText\n\n--------\n\n${textWithEntities.text}"

                }

                if (!cancel.get()) {

                    selectedObject.messageOwner.translated = true

                    next()

                    withContext(Dispatchers.Main) {

                        messageHelper.resetMessageContent(dialogId, selectedObject)

                    }

                } else return@trans

            })
        }

        deferreds.awaitAll()
        transPool.cancel()

        messages.forEach { it.translateFinished(target) }

        UIUtil.runOnUIThread {

            if (!cancel.get()) status.uDismiss()

        }

    }

}

fun String.toTextWithEntities(): TLRPC.TL_textWithEntities {
    val ret = TLRPC.TL_textWithEntities()
    ret.text = this
    ret.entities = ArrayList()
    return ret
}

//class MessageTrans {
//    companion object {
//
//        @JvmStatic
//        fun getTranslatedTexts() : HashMap<Locale, HashMap<String, String>> {
//            return translatedTexts
//        }
//    }
//}