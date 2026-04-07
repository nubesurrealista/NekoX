package moe.hx030.momogram.config.cell;

import androidx.recyclerview.widget.RecyclerView;

import moe.hx030.momogram.config.CellGroup;

public class ConfigCellDivider extends AbstractConfigCell {

    public int getType() {
        return CellGroup.ITEM_TYPE_DIVIDER;
    }

    public boolean isEnabled() {
        return false;
    }

    public void onBindViewHolder(RecyclerView.ViewHolder holder) {
    }
}
