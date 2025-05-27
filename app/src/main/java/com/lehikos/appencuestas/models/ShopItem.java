package com.lehikos.appencuestas.models;

public class ShopItem {
    public enum ItemType {
        AVATAR,
        FRAME
    }

    private String id;
    private String name;
    private String description;
    private int price;
    private int imageResId;
    private ItemType type;
    private boolean isPurchased;
    private boolean isEquipped;

    public ShopItem(String id, String name, String description, int price, int imageResId, ItemType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageResId = imageResId;
        this.type = type;
        this.isPurchased = false;
        this.isEquipped = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public int getImageResId() {
        return imageResId;
    }

    public ItemType getType() {
        return type;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }

    public boolean isEquipped() {
        return isEquipped;
    }

    public void setEquipped(boolean equipped) {
        isEquipped = equipped;
    }
} 