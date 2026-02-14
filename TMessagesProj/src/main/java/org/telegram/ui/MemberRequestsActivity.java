package org.telegram.ui;

import android.content.Context;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MemberRequestsController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Delegates.MemberRequestsDelegate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import moe.hx030.momogram.util.ModUtil;

public class MemberRequestsActivity extends BaseFragment {

    public static final int searchMenuItem = 0;
    public static final int dismissMenuItem = 69;

    private final MemberRequestsDelegate delegate;
    private final long chatId;

    public MemberRequestsActivity(long chatId) {
        this.chatId = chatId;
        delegate = new MemberRequestsDelegate(this, getLayoutContainer(), chatId, true) {
            @Override
            protected void onImportersChanged(String query, boolean fromCache, boolean fromHide) {
                if (fromHide) {
                    actionBar.setSearchFieldText("");
                } else {
                    super.onImportersChanged(query, fromCache, fromHide);
                }
            }
        };
    }

    @Override
    public View createView(Context context) {
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(delegate.isChannel ? LocaleController.getString(R.string.SubscribeRequests) : LocaleController.getString(R.string.MemberRequests));

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(searchMenuItem, R.drawable.outline_header_search)
                .setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override
                    public void onSearchExpand() {
                        super.onSearchExpand();
                        delegate.setSearchExpanded(true);
                    }
                    @Override
                    public void onSearchCollapse() {
                        super.onSearchCollapse();
                        delegate.setSearchExpanded(false);
                        delegate.setQuery(null);
                    }
                    @Override
                    public void onTextChanged(EditText editText) {
                        super.onTextChanged(editText);
                        delegate.setQuery(editText.getText().toString());
                    }
                });
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.Search));
        searchItem.setVisibility(View.GONE);

        ActionBarMenuItem dismissAllItem = menu.addItem(dismissMenuItem, R.drawable.msg_clear)
                .setIsSearchField(false);

        dismissAllItem.setOnClickListener((__) -> {
            new AlertDialog.Builder(context)
                    .setTitle(LocaleController.getString(R.string.MemberRequests))
                    .setMessage(LocaleController.getString(R.string.ConfirmDismissAllJoinRequests))
                    .setPositiveButton(LocaleController.getString(R.string.OK), (___, ____) -> {
                        ModUtil.dismissAllJoinRequests(currentAccount, chatId);
                        AndroidUtilities.runOnUIThread(() -> finishFragment(true), 200);
                    })
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), (___, ____) -> {})
                    .show();
        });
        // ban all
        dismissAllItem.setOnLongClickListener((__) -> {
            new AlertDialog.Builder(context)
                    .setTitle(LocaleController.getString(R.string.MemberRequests))
                    .setMessage(LocaleController.getString(R.string.ConfirmBanAllJoinRequests))
                    .setPositiveButton(LocaleController.getString(R.string.OK), (___, ____) -> {
                        final AlertDialog progressDlg = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
                        Utilities.stageQueue.postRunnable(() -> ModUtil.banAllJoinRequests(currentAccount, chatId, () -> {
                            progressDlg.dismiss();
                            finishFragment(true);
                        }));
                        progressDlg.setCanCancel(false);
                        progressDlg.setProgress(0);
                        progressDlg.show();
                    })
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), (___, ____) -> {})
                    .show();
            return true;
        });

        FrameLayout rootLayout = delegate.getRootLayout();
        delegate.loadMembers();

        return fragmentView = rootLayout;
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        return delegate.onBackPressed(invoked);
    }
}
