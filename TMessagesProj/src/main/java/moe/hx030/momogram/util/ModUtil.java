package moe.hx030.momogram.util;

import android.text.TextUtils;
import android.util.Log;

import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.cc.CCConverter;
import tw.nekomimi.nekogram.cc.CCTarget;

public class ModUtil {

    private static ArrayDeque<Long> bannedUserIds;

    public static TLRPC.TL_messages_chatInviteImporters filterJoinRequests(int currentAccount, long chatId, TLRPC.TL_messages_chatInviteImporters importers) {
        if (importers == null || !NekoConfig.autoDismissJoinReq.Bool()) {
            Log.d("030-filterJoinReq", String.format("importers=%s autoDismiss=%s", importers != null , NekoConfig.autoDismissJoinReq.Bool()));
            return importers;
        }
        if (bannedUserIds == null) bannedUserIds = new ArrayDeque<>(60);
        final boolean bio = NekoConfig.autoDismissJoinReqBio.Bool();
        final boolean dummy = NekoConfig.autoDismissDummy.Bool();
        final boolean regex = NekoConfig.autoDismissRegexPattern != null;
        final boolean useOpenCC = NekoConfig.autoDismissNameUseOpenCC.Bool();
        int oldSize = importers.importers.size();
        Log.d("030-filterJoinReq", String.format("b4 | count=%d size=%d", importers.count, importers.importers.size()));

        Map<Long, TLRPC.User> currentUsers = new HashMap<>(importers.users.size());
        for (TLRPC.User u : importers.users) {
            currentUsers.put(u.id, u);
        }

        final ArrayList<TLRPC.TL_chatInviteImporter> finalImporters = new ArrayList<>();
        for (TLRPC.TL_chatInviteImporter i : importers.importers) {
            TLRPC.User u = currentUsers.get(i.user_id);
            if (u == null) continue;
            if (dummy && TextUtils.isEmpty(u.username) && !ImageLocation.isUserHasPhoto(u)) {
                dismissJoinRequest(currentAccount, chatId, i, u);
            } else if (u.deleted || (regex &&
                    FilterUtils.checkName(NekoConfig.autoDismissRegexPattern, u.first_name, u.last_name, useOpenCC)) ||
                    (bio && FilterUtils.checkString(NekoConfig.autoDismissRegexPattern, i.about, useOpenCC))) {

                if (bannedUserIds.contains(u.id)) continue;
                bannedUserIds.add(u.id);

                dismissJoinRequest(currentAccount, chatId, i, u);
                MessagesController.getInstance(currentAccount).banUserFromChat(chatId, u, (response, error) -> {
                    if (error != null) {
                        Log.e("030-filterJoinReq", String.format("ban err %d: %s", error.code, error.text));
                    } else {
                        Log.d("030-filterJoinReq", String.format("banned %d %s", u.id, u.first_name));
                    }
                });
            } else {
                boolean match = NekoConfig.autoDismissRegexPattern.matcher(u.first_name).find();
                Log.d("030-filterJoinReq", String.format("passed, DA=%s regex=%s match=%s first_name=%s", u.deleted, regex, match, u.first_name));
                finalImporters.add(i);
            }
        }
        importers.importers = finalImporters;
        importers.count -= (oldSize - finalImporters.size());
        Log.d("030-filterJoinReq", String.format("after | count=%d size=%d", importers.count, importers.importers.size()));
        return importers;
    }

    private static void dismissJoinRequest(int currentAccount, long chatId, TLRPC.TL_chatInviteImporter i, TLRPC.User u) {
        TLRPC.TL_messages_hideChatJoinRequest req = new TLRPC.TL_messages_hideChatJoinRequest();
        req.approved = false;
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(-chatId);
        req.user_id = MessagesController.getInstance(currentAccount).getInputUser(u);
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                Log.e("030-filterJoinReq", String.format("dismiss err %d: %s", error.code, error.text));
            }
        });
        Log.d("030-filterJoinReq", String.format("send dismiss req for %s %d (DA=%s)", u.first_name, i.user_id, u.deleted));
    }

}
