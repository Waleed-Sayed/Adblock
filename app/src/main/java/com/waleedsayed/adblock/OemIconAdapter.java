package com.waleedsayed.adblock;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class OemIconAdapter extends RecyclerView.Adapter<OemIconAdapter.ViewHolder> {

    public interface OnIconClickListener {
        void onIconClick(OemIconItem item);
    }

    private final List<OemIconItem> items = new ArrayList<>();
    private final OnIconClickListener listener;
    private int selectedPosition = -1;

    public OemIconAdapter(List<OemIconItem> initialItems, OnIconClickListener listener) {
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
        this.listener = listener;
    }

    public void updateItems(List<OemIconItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public OemIconItem getSelectedItem() {
        if (selectedPosition >= 0 && selectedPosition < items.size()) {
            return items.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_oem_icon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OemIconItem item = items.get(position);
        holder.tvIconLabel.setText(item.name);
        holder.ivIconPreview.setImageDrawable(item.drawable);

        String badgeText;
        int badgeBg;
        if ("samsung".equals(item.brandKey)) {
            badgeText = "One UI";
            badgeBg = Color.parseColor("#0072DE");
        } else if ("xiaomi".equals(item.brandKey)) {
            badgeText = "HyperOS";
            badgeBg = Color.parseColor("#0288D1");
        } else {
            badgeText = "MagicOS";
            badgeBg = Color.parseColor("#00ACC1");
        }
        holder.tvBrandBadge.setText(badgeText);
        holder.tvBrandBadge.setBackgroundColor(badgeBg);

        holder.itemView.setBackgroundColor(
                position == selectedPosition ? Color.parseColor("#3334D399") : Color.TRANSPARENT
        );

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                selectedPosition = pos;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onIconClick(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivIconPreview;
        public final TextView tvIconLabel;
        public final TextView tvBrandBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIconPreview = itemView.findViewById(R.id.ivIconPreview);
            tvIconLabel = itemView.findViewById(R.id.tvIconLabel);
            tvBrandBadge = itemView.findViewById(R.id.tvBrandBadge);
        }
    }
}
