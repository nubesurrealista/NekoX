package moe.hx030.momogram.parts

import org.telegram.messenger.MessageObject
import org.telegram.messenger.TranslateController
import org.telegram.tgnet.TLRPC
import moe.hx030.momogram.MomoConfig
import moe.hx030.momogram.transtale.TranslateDb
import moe.hx030.momogram.transtale.code2Locale

fun postPollTrans(messageObject: MessageObject, media: TLRPC.TL_messageMediaPoll, poll: TLRPC.TL_poll) {
    messageObject.translated = true
    poll.translatedQuestion = media.poll.translatedQuestion
    poll.answers.forEach { answer ->
        val ans = media.poll.answers.find { it != null && it.text == answer.text }
        if (ans != null && answer != null)
            answer.translatedText = ans.translatedText
        else if (answer != null) {
            // workaround for null stuff
            val db = TranslateDb.forLocale(MomoConfig.translateToLang.String().code2Locale)
            val txtFromDb = db?.query(answer.text.text)
            val txt = txtFromDb ?: translatedTexts[MomoConfig.translateToLang.String().code2Locale]?.get(answer.text.text)
            answer.translatedText = txt + " | " + answer.text.text
        }
    }
}

@JvmName("generateTranslatedPoll")
fun MessageObject.generateTranslatedPoll(): Boolean {
    val media = MessageObject.getMedia(messageOwner)
    if (media is TLRPC.TL_messageMediaToDo) return generateTranslatedToDo()

    val poll = (media as TLRPC.TL_messageMediaPoll)
    val translatedPoll = TranslateController.PollText()
    translatedPoll.question = poll.poll.translatedQuestion.toTextWithEntities()
    translatedPoll.answers = ArrayList()
    poll.poll.answers.forEach {
        val translatedAns = TLRPC.PollAnswer()
        translatedAns.translatedText = it.translatedText
        translatedAns.text = it.translatedText.toTextWithEntities()
        translatedAns.option = it.option
        translatedPoll.answers.add(translatedAns)
    }
    translated = true
    messageOwner.translatedPoll = translatedPoll
    return translated
}

fun MessageObject.generateTranslatedToDo(): Boolean {
    val todo = (MessageObject.getMedia(messageOwner) as TLRPC.TL_messageMediaToDo)
    val translatedPoll = TranslateController.PollText()
    translatedPoll.question = todo.todo.translatedTitle
    translatedPoll.answers = ArrayList()
    todo.todo.list.forEach {
        val translatedAns = TLRPC.PollAnswer()
        translatedAns.translatedText = it.translatedTitle.text
        translatedAns.text = it.translatedTitle
        translatedAns.option = ByteArray(1)
        translatedAns.option[0] = it.id.toByte()
        translatedPoll.answers.add(translatedAns)
    }
    translated = true
    messageOwner.translatedPoll = translatedPoll
    return translated
}

object PollTransUpdates {
    @JvmStatic
    fun generateTranslatedPoll(messageObject: MessageObject): Boolean {
        return messageObject.generateTranslatedPoll()
    }
}
