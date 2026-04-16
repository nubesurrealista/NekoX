package moe.hx030.momogram.config.cell;

import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.RecyclerListView;

import moe.hx030.momogram.config.CellGroup;
import moe.hx030.momogram.config.ConfigItem;

public class ConfigCellTextDetail extends AbstractConfigCell {
    public final ConfigItem bindConfig;
    private final String title;
    private final String hint;
    public final RecyclerListView.OnItemClickListener onItemClickListener;

    public ConfigCellTextDetail(ConfigItem bind, RecyclerListView.OnItemClickListener onItemClickListener, String hint) {
        int strId = bind.getId();
        this.bindConfig = bind;
        this.title = (strId != 0) ? LocaleController.getString(strId) : LocaleController.getString(bind.getKey());
        this.hint = hint == null ? "" : hint;
        this.onItemClickListener = onItemClickListener;
    }

    public int getType() {
        return CellGroup.ITEM_TYPE_TEXT_DETAIL;
    }

    public boolean isEnabled() {
        return false;
    }

    public void onBindViewHolder(RecyclerView.ViewHolder holder) {
        TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
        cell.setTextAndValue(title, StringUtils.isNotBlank(bindConfig.String()) ? bindConfig.String() : hint, cellGroup.needSetDivider(this));
    }
}
