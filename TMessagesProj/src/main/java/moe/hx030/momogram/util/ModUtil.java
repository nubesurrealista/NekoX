package moe.hx030.momogram.util;

import android.text.TextUtils;
import android.util.Log;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.cc.CCConverter;
import tw.nekomimi.nekogram.cc.CCTarget;

public class ModUtil {

    private static ArrayDeque<Long> bannedUserIds;
    private static final AtomicInteger banned = new AtomicInteger(0), dismissed = new AtomicInteger(0);

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
                dismissed.addAndGet(1);
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
                banned.addAndGet(1);
            } else {
                boolean match = NekoConfig.autoDismissRegexPattern.matcher(u.first_name).find();
                Log.d("030-filterJoinReq", String.format("passed, DA=%s regex=%s match=%s first_name=%s", u.deleted, regex, match, u.first_name));
                finalImporters.add(i);
            }
        }
        importers.importers = finalImporters;
        importers.count -= (oldSize - finalImporters.size());
        Log.d("030-filterJoinReq", String.format("after | count=%d size=%d", importers.count, importers.importers.size()));
        if (banned.get() > 0 || dismissed.get() > 0) {
            scheduleShowStats();
        }
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

    private static final Runnable showStats = () -> {
        int ban = banned.get(), dismiss = dismissed.get();
        banned.set(0);
        dismissed.set(0);
        BaseFragment frag = LaunchActivity.getLastFragment();
        if (frag == null) return;
        String msg;
        if (ban > 0 && dismiss > 0) {
            msg = LocaleController.formatString(R.string.AutoReqStats, ban, dismiss);
        } else if (ban > 0) {
            msg = LocaleController.formatString(R.string.AutoReqStatsBanned, ban);
        } else {
            msg = LocaleController.formatString(R.string.AutoReqStatsDismissed, dismiss);
        }
        BulletinFactory.of(frag).createSimpleBulletin(
                frag.getContext().getResources().getDrawable(R.drawable.profile_info), msg)
                .show(true);
    };
    private static void scheduleShowStats() {
        ApplicationLoader.applicationHandler.removeCallbacks(showStats);
        ApplicationLoader.applicationHandler.postDelayed(showStats, 1000);
    }
}
