package moe.hx030.momogram.util;

import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import tw.nekomimi.nekogram.NekoConfig;

public class FilterUtils {

    public static boolean filterPM(int currentAccount, MessageObject messageObject, TLObject diff) {
        long chatId = messageObject.getChatId(), senderId = messageObject.getSenderId();
        ContactsController contactsController = ContactsController.getInstance(currentAccount);
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        MessagesStorage messagesStorage = MessagesStorage.getInstance(currentAccount);
        NotificationsController notificationsController = NotificationsController.getInstance(currentAccount);

        if (NekoConfig.ignoreBlocked.Bool() && messagesController.blockedPeers.indexOfKey(senderId) >= 0) {
            return true;
        }

        if (!NekoConfig.autoArchiveAndMute.Bool() || senderId <= 0 || chatId != 0 || !messageObject.isFromUser()) {
            return false;
        }

        if (NekoConfig.debugAntiSpam.Bool())
            Log.d("030-debugspam", String.format("PM?, id=%d, chat=%d, isSvc=%s, isContact=%s",
                    senderId, messageObject.getChatId(), UserObject.isService(senderId),
                    contactsController.isContact(senderId)));

        TLRPC.User currentUser = messagesStorage.getUserSync(chatId);
        final TLRPC.UserFull[] userFull = new TLRPC.UserFull[1];
        final CountDownLatch lock = new CountDownLatch(1);
        if (currentUser == null) {
            ArrayList<TLRPC.User> users = null;
//            return false;
            if (diff != null) {
                try {
                    Object us = ReflectUtil.getFieldValue(diff, "users");
                    if (us instanceof ArrayList arr && !arr.isEmpty() && arr.get(0) instanceof TLRPC.User)
                        users = (ArrayList<TLRPC.User>) us;

                    for (TLRPC.User u : users) {
                        if (u.id == senderId) {
                            currentUser = u;
                            break;
                        }
                    }
                } catch (Exception ex) {
                    Log.e("030-filter", String.format("failed to get %d from current update data, skipping", senderId), ex);
                    return false;
                }
            }
        }

        if (currentUser != null && !UserObject.isService(senderId) && !contactsController.isContact(senderId)) {
            if (MessagesStorage.getInstance(currentAccount).isExistingChat(chatId != 0 ? chatId : senderId)) return false;

            if (currentUser.bot) return false; // bots can't send first msg
            final ArrayList<Long> list = new ArrayList<>(1);
            list.add(senderId);
            if (NekoConfig.autoArchiveAndMuteNoCommonGroupOnly.Bool()) {
                if (userFull[0] == null) {
                    messagesController.loadFullUser(currentUser, messagesStorage.classGuid, true, uf -> {
                        if (uf != null && uf.common_chats_count > 0) return;
                        Log.d("030-spam", "no common group => archive & mute " + senderId);
                        archiveAndMute(messagesController, notificationsController, list, senderId);
                    });
                } else {
                    TLRPC.UserFull uf = userFull[0];
                    if (uf.common_chats_count > 0) return false;
                    Log.d("030-spam", "no common group => archive & mute " + senderId);
                    archiveAndMute(messagesController, notificationsController, list, senderId);
                }
            } else {
                Log.d("030-spam", "archive & mute " + senderId);
                archiveAndMute(messagesController, notificationsController, list, senderId);
            }
        } else if (currentUser == null) {
            Log.e("030-filter", String.format("cannot fetch currentUser for %d %d, skipping...", senderId, chatId));
        }

        return false;
    }

    public static void archiveAndMute(MessagesController messagesController, NotificationsController notificationsController, ArrayList<Long> list, long id) {
        AndroidUtilities.runOnUIThread(() -> {
            messagesController.addDialogToFolder(list, 1, -1, null, 0);
            notificationsController.setDialogNotificationsSettings(id, 0, NotificationsController.SETTING_MUTE_FOREVER);
        });
    }

}
