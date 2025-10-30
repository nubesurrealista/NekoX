package moe.hx030.momogram.util;

import android.text.TextUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.cc.CCConverter;
import tw.nekomimi.nekogram.cc.CCTarget;

public class FilterUtils {

    private final static HashSet<Long> pendingIds = new HashSet<>();

    public enum Result {
        Blocked,
        Passed,
        Pending
    }

    public static Result filterPM(int currentAccount, MessageObject messageObject, TLObject diff) {
        return filterPM(currentAccount, messageObject.getChatId(), messageObject.getSenderId(), messageObject.isFromUser(), diff);
    }

    public static Result filterPM(int currentAccount, Long chatId, Long senderId, Boolean isFromUser, TLObject diff) {
        if (senderId == null) return Result.Passed;
        if (chatId == null) chatId = 0L;
        if (isFromUser == null) isFromUser = true;

        ContactsController contactsController = ContactsController.getInstance(currentAccount);
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        MessagesStorage messagesStorage = MessagesStorage.getInstance(currentAccount);
        NotificationsController notificationsController = NotificationsController.getInstance(currentAccount);

        if (NekoConfig.ignoreBlocked.Bool() && messagesController.blockedPeers.indexOfKey(senderId) >= 0) {
            return Result.Blocked;
        }

        if (!NekoConfig.autoArchiveAndMute.Bool() || senderId <= 0 || chatId != 0 || !isFromUser) {
            return Result.Passed;
        }

        if (NekoConfig.debugAntiSpam.Bool())
            Log.d("030-debugspam", String.format("PM?, id=%d, chat=%d, isSvc=%s, isContact=%s",
                    senderId, chatId, UserObject.isService(senderId),
                    contactsController.isContact(senderId)));

        TLRPC.User currentUser = messagesStorage.getUserSync(chatId);

        if (!UserObject.isService(senderId) && !contactsController.isContact(senderId)) {
            if (MessagesStorage.getInstance(currentAccount).isExistingChat(senderId)) return Result.Passed;

            if (currentUser == null) {
                ArrayList<TLRPC.User> users = null;
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
                    }
                }
                // pending if not in the same slice of update diff
                if (currentUser == null) {
                    pendingIds.add(senderId);
                    return Result.Pending;
                }
            }

            if (currentUser.bot) return Result.Passed; // bots can't send first msg
            final ArrayList<Long> list = new ArrayList<>(1);
            list.add(senderId);
            if (NekoConfig.autoArchiveAndMuteNoCommonGroupOnly.Bool()) {
                messagesController.loadFullUser(currentUser, messagesStorage.classGuid, true, uf -> {
                    if (uf != null && uf.common_chats_count > 0) return;
                    Log.d("030-spam", "no common group => archive & mute " + senderId);
                    archiveAndMute(messagesController, notificationsController, list, senderId);
                });
            } else {
                Log.d("030-spam", "archive & mute " + senderId);
                archiveAndMute(messagesController, notificationsController, list, senderId);
            }
        } else if (currentUser == null) {
            Log.e("030-filter", String.format("cannot fetch currentUser for %d %d, skipping...", senderId, chatId));
            pendingIds.add(senderId);
            return Result.Pending;
        }

        return Result.Passed;
    }

    public static void checkPendingIds(int currentAccount) {
        if (pendingIds.isEmpty()) return;
        Log.d("030-filter", String.format("checkPendingIds: count=%d", pendingIds.size()));
        List<Long> pending = List.of(pendingIds.toArray(new Long[0]));
        pendingIds.clear();
        for (Long id : pending) {
            if (filterPM(currentAccount, null, id, null, null) == Result.Pending) {
                pendingIds.add(id);
            }
        }
    }

    public static void archiveAndMute(MessagesController messagesController, NotificationsController notificationsController, ArrayList<Long> list, long id) {
        AndroidUtilities.runOnUIThread(() -> {
            messagesController.addDialogToFolder(list, 1, -1, null, 0);
            notificationsController.setDialogNotificationsSettings(id, 0, NotificationsController.SETTING_MUTE_FOREVER);
        });
    }

    private static Set<CCTarget> CCTargets;
    public static boolean checkName(Pattern regex, String firstname, String lastname, boolean useOpenCC) {
        if (regex == null) return false;
        if (regex.matcher(firstname).find()) return true;
        if (!TextUtils.isEmpty(lastname) && regex.matcher(lastname).find()) return true;
        if (useOpenCC) {
            if (CCTargets == null) CCTargets = Set.of(CCTarget.TC, CCTarget.SC);

            for (CCTarget target : CCTargets) {
                CCConverter conv = CCConverter.get(target);
                if (regex.matcher(conv.convert(firstname)).find()) return true;
                if (!TextUtils.isEmpty(lastname) && regex.matcher(conv.convert(firstname)).find()) return true;
            }
        }
        return false;
    }

    public static boolean checkString(Pattern regex, String str, boolean useOpenCC) {
        if (regex == null || TextUtils.isEmpty(str)) return false;
        if (regex.matcher(str).find()) return true;
        if (useOpenCC) {
            if (CCTargets == null) CCTargets = Set.of(CCTarget.TC, CCTarget.SC);

            for (CCTarget target : CCTargets) {
                CCConverter conv = CCConverter.get(target);
                if (regex.matcher(conv.convert(str)).find()) return true;
            }
        }
        return false;
    }

}
