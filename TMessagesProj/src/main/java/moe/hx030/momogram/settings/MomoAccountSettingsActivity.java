package moe.hx030.momogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.DocumentSelectActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import moe.hx030.momogram.helpers.SecretChatBackupManager;
import moe.hx030.momogram.ui.MessageHelper;
import moe.hx030.momogram.utils.AlertUtil;
import moe.hx030.momogram.utils.ShareUtil;
import moe.hx030.momogram.utils.TelegramUtil;

@SuppressLint("RtlHardcoded")
public class MomoAccountSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int rowCount;

    private int accountRow;
    private int deleteAccountRow;
    private int account2Row;

    private int secretChatHeaderRow;
    private int backupSecretChatRow;
    private int restoreSecretChatRow;
    private int secretChatShadowRow;

    private UndoView tooltip;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.Account));

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == deleteAccountRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setMessage(LocaleController.getString(R.string.TosDeclineDeleteAccount));
                builder.setTitle(LocaleController.getString(R.string.DeleteAccount));
                builder.setPositiveButton(LocaleController.getString(R.string.Deactivate), (dialog, which) -> {
                    AlertDialog.Builder builder12 = new AlertDialog.Builder(getParentActivity());
                    builder12.setMessage(LocaleController.getString(R.string.TosDeclineDeleteAccount));
                    builder12.setTitle(LocaleController.getString(R.string.DeleteAccount));

                    LinearLayout linearLayout = new LinearLayout(context);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);

                    EditTextBoldCursor editText = new EditTextBoldCursor(context);
                    editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
                    editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    editText.setHint("Type 'yes' in capital to continue.");
                    linearLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(10), 0));

                    builder12.setView(linearLayout);

                    builder12.setPositiveButton(LocaleController.getString(R.string.Deactivate), (dialogInterface, i) -> {

                        if (!editText.getText().toString().equals("YES")) return;

                        final AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
                        progressDialog.setCanCancel(false);
                        progressDialog.show();

                        // delete dialogs
                        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>(getMessagesController().getAllDialogs());
                        for (TLRPC.Dialog TLdialog : dialogs) {
                            if (TLdialog instanceof TLRPC.TL_dialogFolder) {
                                continue;
                            }
                            TLRPC.Peer peer = getMessagesController().getPeer((int) TLdialog.id);
                            if (peer.channel_id != 0) {
                                TLRPC.Chat chat = getMessagesController().getChat(peer.channel_id);
                                if (!chat.broadcast) {
                                    if (ChatObject.isChannel(chat) && chat.megagroup && ChatObject.canUserDoAction(chat, ChatObject.ACTION_DELETE_MESSAGES)) {
                                        getMessagesController().deleteUserChannelHistory(chat, UserConfig.getInstance(currentAccount).getCurrentUser(), null, 0);
                                    } else {
                                        MessageHelper.getInstance(currentAccount).deleteUserChannelHistoryWithSearch(null, TLdialog.id, getMessagesController().getUser(getUserConfig().clientUserId));
                                    }
                                }
                            }
                            if (peer.user_id != 0) {
                                getMessagesController().deleteDialog(TLdialog.id, 0, true);
                            }
                        }
                        // delete account
                        org.telegram.tgnet.tl.TL_account.deleteAccount req = new org.telegram.tgnet.tl.TL_account.deleteAccount();
                        req.reason = "Meow";
                        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                            try {
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            if (response instanceof TLRPC.TL_boolTrue) {
                                getMessagesController().performLogout(0);
                            } else if (error == null || error.code != -1000) {
                                String errorText = LocaleController.getString(R.string.ErrorOccurred);
                                if (error != null) {
                                    errorText += "\n" + error.text;
                                }
                                AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity());
                                builder1.setTitle(LocaleController.getString(R.string.AppName));
                                builder1.setMessage(errorText);
                                builder1.setPositiveButton(LocaleController.getString(R.string.OK), null);
                                builder1.show();
                            }
                        }));
                    });
                    builder12.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    AlertDialog dialog12 = builder12.create();
                    showDialog(dialog12);
                    TextView button = (TextView) dialog12.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                    }
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                builder.show();
                AlertDialog dialog = builder.create();
                showDialog(dialog);
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            } else if (position == backupSecretChatRow) {
                promptPasswordAndBackup();
            } else if (position == restoreSecretChatRow) {
                openFileAndPromptPassword();
            }
        });

        tooltip = new UndoView(context);
        frameLayout.addView(tooltip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        return fragmentView;
    }

    private void promptPasswordAndBackup() {
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Backup Secret Chats");
        builder.setMessage("Enter a password to encrypt your backup. This password will be required for restoration.");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), 0);

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setHint("Password");
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton("Binary (Efficient)", (dialog, which) -> startBackup(editText.getText().toString(), SecretChatBackupManager.FORMAT_BINARY));
        builder.setNeutralButton("JSON (Readable)", (dialog, which) -> startBackup(editText.getText().toString(), SecretChatBackupManager.FORMAT_JSON));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void startBackup(String password, int format) {
        if (TextUtils.isEmpty(password)) {
            AlertUtil.showSimpleAlert(getParentActivity(), "Password cannot be empty");
            return;
        }
        final AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
        progressDialog.setCanCancel(false);
        progressDialog.show();

        java.text.DateFormat df = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
        java.util.Date today = java.util.Calendar.getInstance().getTime();
        String ext = format == SecretChatBackupManager.FORMAT_BINARY ? ".tgscb" : ".json";
        File cacheFile = new File(ApplicationLoader.applicationContext.getCacheDir(), "SecretChat_" + df.format(today) + ext);

        SecretChatBackupManager.backup(currentAccount, password, cacheFile, format, new SecretChatBackupManager.BackupDelegate() {
            @Override
            public void onProgress(float progress) { }

            @Override
            public void onFinish(boolean success, String error, String logs) {
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    if (success) {
                        showLogsDialog(false, "Backup Successful", logs, () -> ShareUtil.shareFile(getParentActivity(), cacheFile));
                    } else {
                        showLogsDialog(false, "Backup Failed: " + error, logs, null);
                    }
                });
            }
        });
    }

    private void openFileAndPromptPassword() {
        DocumentSelectActivity fragment = new DocumentSelectActivity(false);
        fragment.setMaxSelectedFiles(1);
        fragment.setAllowPhoto(false);
        fragment.setDelegate(new DocumentSelectActivity.DocumentSelectActivityDelegate() {
            @Override
            public void didSelectFiles(DocumentSelectActivity activity, ArrayList<String> files, String caption, boolean notify, int scheduleDate) {
                activity.finishFragment();
                AndroidUtilities.runOnUIThread(() -> promptPasswordAndRestore(new File(files.get(0))));
                Log.d("030-r", "openFileAndPromptPassword didSelectFiles");
            }
            @Override public void didSelectPhotos(ArrayList<SendMessagesHelper.SendingMediaInfo> photos, boolean notify, int scheduleDate) { }
            @Override public void startDocumentSelectActivity() { }
        });
        presentFragment(fragment);
    }

    private void promptPasswordAndRestore(File file) {
        Log.d("030-r", "promptPasswordAndRestore");
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Restore Secret Chats");
        builder.setMessage("Enter the password used to encrypt this backup.");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), 0);

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setHint("Password");
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            final AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
            progressDialog.setCanCancel(false);
            progressDialog.show();

            SecretChatBackupManager.restore(currentAccount, editText.getText().toString(), file, new SecretChatBackupManager.BackupDelegate() {
                @Override public void onProgress(float progress) { }
                @Override public void onFinish(boolean success, String error, String logs) {
                    AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                        showLogsDialog(success, success ? "Restore Successful" : ("Restore Failed: " + error), logs, null);
                    });
                }
            });
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
        Log.d("030-r", "promptPasswordAndRestore show dialog");
    }

    private void showLogsDialog(boolean restart, String title, String logs, Runnable onDismiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);

        ScrollView scrollView = new ScrollView(getParentActivity());
        TextView textView = new TextView(getParentActivity());
        textView.setText(logs);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(12);
        textView.setPadding(AndroidUtilities.dp(15), AndroidUtilities.dp(10), AndroidUtilities.dp(15), AndroidUtilities.dp(10));
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            if (onDismiss != null) onDismiss.run();
            if (restart) TelegramUtil.restartApp(false);
        });
        showDialog(builder.create());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        rowCount = 0;

        accountRow = rowCount++;
        deleteAccountRow = rowCount++;
        account2Row = rowCount++;

        secretChatHeaderRow = rowCount++;
        backupSecretChatRow = rowCount++;
        restoreSecretChatRow = rowCount++;
        secretChatShadowRow = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{EmptyCell.class, TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, TextDetailSettingsCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        return themeDescriptions;
    }


    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    if (position == account2Row || position == secretChatShadowRow) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == deleteAccountRow) {
                        textCell.setText(LocaleController.getString(R.string.DeleteAccount), false);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText2));
                    } else if (position == backupSecretChatRow) {
                        textCell.setText("Backup Secret Chats", false);
                    } else if (position == restoreSecretChatRow) {
                        textCell.setText("Restore Secret Chats", false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == accountRow) {
                        headerCell.setText(LocaleController.getString(R.string.Account));
                    } else if (position == secretChatHeaderRow) {
                        headerCell.setText("Secret Chats");
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case 1:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 5:
                    view = new NotificationsCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 6:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 7:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == account2Row || position == secretChatShadowRow) {
                return 1;
            } else if (position == deleteAccountRow || position == backupSecretChatRow || position == restoreSecretChatRow) {
                return 2;
            } else if (position == accountRow || position == secretChatHeaderRow) {
                return 4;
            }
            return 3;
        }
    }
}
