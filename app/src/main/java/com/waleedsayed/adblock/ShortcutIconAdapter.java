package com.waleedsayed.adblock;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ShortcutIconAdapter extends RecyclerView.Adapter<ShortcutIconAdapter.ViewHolder> {

    public static class IconOption {
        public final String label;
        public final Drawable drawable;
        public final boolean isTransparent;

        public IconOption(String label, Drawable drawable, boolean isTransparent) {
            this.label = label;
            this.drawable = drawable;
            this.isTransparent = isTransparent;
        }
    }

    private final List<IconOption> options;
    private int selectedIndex = 0;

    public ShortcutIconAdapter(List<IconOption> options) {
        this.options = options;
    }

    public IconOption getSelectedOption() {
        if (options.isEmpty()) {
            return null;
        }
        return options.get(selectedIndex);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortcut_icon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IconOption option = options.get(position);
        holder.tvIconLabel.setText(option.label);
        if (option.isTransparent) {
            holder.ivIconPreview.setImageDrawable(null);
        } else {
            holder.ivIconPreview.setImageDrawable(option.drawable);
        }
        holder.itemContainer.setBackgroundColor(
                position == selectedIndex ? Color.parseColor("#332196F3") : Color.TRANSPARENT
        );
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                selectedIndex = pos;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout itemContainer;
        final ImageView ivIconPreview;
        final TextView tvIconLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemContainer = itemView.findViewById(R.id.itemContainer);
            ivIconPreview = itemView.findViewById(R.id.ivIconPreview);
            tvIconLabel = itemView.findViewById(R.id.tvIconLabel);
        }
    }
}
