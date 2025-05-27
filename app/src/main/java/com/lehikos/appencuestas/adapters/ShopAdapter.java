package com.lehikos.appencuestas.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.models.ShopItem;

import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {
    private final List<ShopItem> items;
    private final OnShopItemClickListener listener;
    private final Context context;

    public interface OnShopItemClickListener {
        void onItemClick(ShopItem item);
        void onBuyClick(ShopItem item);
        void onEquipClick(ShopItem item);
    }

    public ShopAdapter(Context context, List<ShopItem> items, OnShopItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        ShopItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ShopViewHolder extends RecyclerView.ViewHolder {
        private final ImageView itemImage;
        private final TextView itemName;
        private final TextView itemDescription;
        private final TextView itemPrice;
        private final MaterialButton actionButton;

        ShopViewHolder(View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemName = itemView.findViewById(R.id.item_name);
            itemDescription = itemView.findViewById(R.id.item_description);
            itemPrice = itemView.findViewById(R.id.item_price);
            actionButton = itemView.findViewById(R.id.item_action_button);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(items.get(position));
                }
            });

            actionButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ShopItem item = items.get(position);
                    if (item.isPurchased()) {
                        listener.onEquipClick(item);
                    } else {
                        listener.onBuyClick(item);
                    }
                }
            });
        }

        void bind(ShopItem item) {
            itemImage.setImageResource(item.getImageResId());
            itemName.setText(item.getName());
            itemDescription.setText(item.getDescription());
            itemPrice.setText(context.getString(R.string.shop_item_price, item.getPrice()));

            if (item.isPurchased()) {
                if (item.isEquipped()) {
                    actionButton.setText(R.string.shop_item_equipped);
                    actionButton.setEnabled(false);
                } else {
                    actionButton.setText(R.string.shop_item_equip);
                    actionButton.setEnabled(true);
                }
            } else {
                actionButton.setText(R.string.shop_item_buy);
                actionButton.setEnabled(true);
            }
        }
    }
} 