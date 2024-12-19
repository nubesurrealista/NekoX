package tw.nekomimi.nekogram.transtale;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.common.mapper.EntityConverter;
import org.dizitart.no2.common.mapper.NitriteMapper;

public class ChatLanguageConverter implements EntityConverter<ChatLanguage> {
    @Override
    public Class<ChatLanguage> getEntityType() {
        return ChatLanguage.class;
    }

    @Override
    public Document toDocument(ChatLanguage entity, NitriteMapper nitriteMapper) {
        return Document.createDocument()
                .put("chatId", entity.chatId)
                .put("language", entity.language)
                .put("alwaysTranslateBeforeSend", entity.alwaysTranslateBeforeSend);
    }

    @Override
    public ChatLanguage fromDocument(Document document, NitriteMapper nitriteMapper) {
        Long chatId = (Long) nitriteMapper.tryConvert(document.get("chatId", Long.class), Long.class);
        String language = (String) nitriteMapper.tryConvert(document.get("language", String.class), String.class);
        Boolean alwaysTranslateBeforeSend = (Boolean) nitriteMapper.tryConvert(document.get("alwaysTranslateBeforeSend", Boolean.class), Boolean.class);

        // ???
        if (chatId == null) chatId = 0L;
        if (language == null) language = "en";
        if (alwaysTranslateBeforeSend == null) alwaysTranslateBeforeSend = false;

        return new ChatLanguage(chatId, language, alwaysTranslateBeforeSend);
    }
}
