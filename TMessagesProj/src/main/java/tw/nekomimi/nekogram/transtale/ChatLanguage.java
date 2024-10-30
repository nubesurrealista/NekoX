package tw.nekomimi.nekogram.transtale;

import android.util.Log;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.common.mapper.NitriteMapper;
import org.dizitart.no2.repository.annotations.Id;
import org.dizitart.no2.repository.annotations.Index;

import tw.nekomimi.nekogram.utils.StrUtil;

// @Index("chatId")
@Index(fields = {"chatId"})
public class ChatLanguage {

    // @Id
    @Id
    public long chatId;

    public String language;

    public Boolean alwaysTranslateBeforeSend;

    public ChatLanguage() {
    }

    public ChatLanguage(long chatId, String language, boolean alwaysTranslateBeforeSend) {
        this.chatId = chatId;
        this.language = language;
        this.alwaysTranslateBeforeSend = alwaysTranslateBeforeSend;
    }

    // @Override
    public Document write(NitriteMapper mapper) {
        Document document = Document.createDocument();
        document.put("chatId", chatId);
        document.put("language", language);
        document.put("alwaysTranslateBeforeSend", alwaysTranslateBeforeSend);
        return document;
    }

    // @Override
    public void read(NitriteMapper mapper, Document document) {
        chatId = ((long) document.get("chatId"));
        language = ((String) document.get("language"));
        if (document.containsKey("alwaysTranslateBeforeSend")) {
            alwaysTranslateBeforeSend = (Boolean) document.get("alwaysTranslateBeforeSend");
        } else {
            alwaysTranslateBeforeSend = false;
        }
    }

}
