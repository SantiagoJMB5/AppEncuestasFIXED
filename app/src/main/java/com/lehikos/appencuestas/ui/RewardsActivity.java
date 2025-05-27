package com.lehikos.appencuestas.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.RewardManager;
import com.lehikos.appencuestas.adapters.ShopAdapter;
import com.lehikos.appencuestas.models.ShopItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardsActivity extends BaseActivity implements ShopAdapter.OnShopItemClickListener {
    private static final String TAG = "RewardsActivity";
    private static final String VISIBILITY_STATE_PREFIX = "reward_visibility_";
    private static final String REVEALED_STATE_PREFIX = "reward_revealed_";
    private static final String RELOAD_STATE_FLAG = "reload_state";
    private static final String CURRENCY_KEY = "user_currency";
    private static final String PURCHASED_ITEMS_KEY = "purchased_items";
    private static final String EQUIPPED_AVATAR_KEY = "equipped_avatar";
    private static final String EQUIPPED_FRAME_KEY = "equipped_frame";

    private SharedPreferences prefs;
    private RewardManager rewardManager;
    private List<RewardItemView> rewardItemViews;
    private RecyclerView shopRecyclerView;
    private ShopAdapter shopAdapter;
    private List<ShopItem> shopItems;
    private TextView currencyAmountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Iniciado");

        prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        rewardManager = new RewardManager(prefs);
        rewardItemViews = new ArrayList<>();

        // Inicializar items de la tienda
        initializeShopItems();

        // Configurar RecyclerView de la tienda
        shopRecyclerView = findViewById(R.id.shop_items_recycler_view);
        shopRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        shopAdapter = new ShopAdapter(this, shopItems, this);
        shopRecyclerView.setAdapter(shopAdapter);

        // Configurar visualización de la moneda
        currencyAmountText = findViewById(R.id.currency_amount);
        updateCurrencyDisplay();

        // Mostrar recompensas en la barra horizontal
        mostrarRecompensasEnBarra();

        setSelectedNavigationItem(R.id.Btn_rewards);
        Log.d(TAG, "onCreate: Finalizado");
    }

    private void initializeShopItems() {
        shopItems = new ArrayList<>();
        
        // Agregar avatares
        shopItems.add(new ShopItem("avatar1", getString(R.string.shop_item_avatar1_title), getString(R.string.shop_item_avatar1_description), 100, R.drawable.avatar_basic, ShopItem.ItemType.AVATAR));
        shopItems.add(new ShopItem("avatar2", getString(R.string.shop_item_avatar2_title), getString(R.string.shop_item_avatar2_description), 100, R.drawable.avatar_basic2, ShopItem.ItemType.AVATAR));
        shopItems.add(new ShopItem("avatar3", getString(R.string.shop_item_avatar3_title), getString(R.string.shop_item_avatar3_description), 250, R.drawable.avatar_epic, ShopItem.ItemType.AVATAR));
        shopItems.add(new ShopItem("avatar4", getString(R.string.shop_item_avatar4_title), getString(R.string.shop_item_avatar4_description), 500, R.drawable.avatar_legendary, ShopItem.ItemType.AVATAR));
        
        // Agregar marcos
        shopItems.add(new ShopItem("frame1", getString(R.string.shop_item_frame1_title), getString(R.string.shop_item_frame1_description), 50, R.drawable.frame_silver, ShopItem.ItemType.FRAME));
        shopItems.add(new ShopItem("frame2", getString(R.string.shop_item_frame2_title), getString(R.string.shop_item_frame2_description), 150, R.drawable.frame_gold, ShopItem.ItemType.FRAME));
        shopItems.add(new ShopItem("frame3", getString(R.string.shop_item_frame3_title), getString(R.string.shop_item_frame3_description), 300, R.drawable.frame_legendary, ShopItem.ItemType.FRAME));

        // Cargar estados de compra y equipamiento
        loadItemStates();
    }

    private void loadItemStates() {
        String purchasedItems = prefs.getString(PURCHASED_ITEMS_KEY, "");
        String equippedAvatar = prefs.getString(EQUIPPED_AVATAR_KEY, "");
        String equippedFrame = prefs.getString(EQUIPPED_FRAME_KEY, "");

        for (ShopItem item : shopItems) {
            item.setPurchased(purchasedItems.contains(item.getId()));
            if (item.getType() == ShopItem.ItemType.AVATAR) {
                item.setEquipped(item.getId().equals(equippedAvatar));
            } else {
                item.setEquipped(item.getId().equals(equippedFrame));
            }
        }
    }

    private void saveItemStates() {
        StringBuilder purchasedItems = new StringBuilder();
        String equippedAvatar = "";
        String equippedFrame = "";

        for (ShopItem item : shopItems) {
            if (item.isPurchased()) {
                purchasedItems.append(item.getId()).append(",");
            }
            if (item.isEquipped()) {
                if (item.getType() == ShopItem.ItemType.AVATAR) {
                    equippedAvatar = item.getId();
                } else {
                    equippedFrame = item.getId();
                }
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PURCHASED_ITEMS_KEY, purchasedItems.toString());
        editor.putString(EQUIPPED_AVATAR_KEY, equippedAvatar);
        editor.putString(EQUIPPED_FRAME_KEY, equippedFrame);
        editor.apply();
    }

    private void updateCurrencyDisplay() {
        int currency = prefs.getInt(CURRENCY_KEY, 0);
        currencyAmountText.setText(String.valueOf(currency));
    }

    private void addCurrency(int amount) {
        int currentCurrency = prefs.getInt(CURRENCY_KEY, 0);
        int newCurrency = currentCurrency + amount;
        prefs.edit().putInt(CURRENCY_KEY, newCurrency).apply();
        updateCurrencyDisplay();
        showCurrencyEarnedAnimation(amount);
    }

    private void showCurrencyEarnedAnimation(int amount) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, 
            getString(R.string.reward_currency_earned, amount), 
            Snackbar.LENGTH_SHORT);
        
        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(getResources().getColor(R.color.accent_color));
        
        snackbar.show();
    }

    @Override
    public void onItemClick(ShopItem item) {
        // Mostrar detalles o vista previa del item
        Toast.makeText(this, item.getDescription(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBuyClick(ShopItem item) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            Toast.makeText(this, getString(R.string.login_required_shop), Toast.LENGTH_LONG).show();
            return;
        }
        int currentCurrency = prefs.getInt(CURRENCY_KEY, 0);
        if (currentCurrency >= item.getPrice()) {
            // Descontar moneda
            int newCurrency = currentCurrency - item.getPrice();
            prefs.edit().putInt(CURRENCY_KEY, newCurrency).apply();
            updateCurrencyDisplay();

            // Mark as purchased locally
            item.setPurchased(true);
            saveItemStates(); // Save purchased state locally
            
            // Save purchase to Firebase
            saveShopItemToFirebase(item);

            // Perform action based on item type
            handlePurchasedItemAction(item);

            // Remove item from the list and update adapter
            shopItems.remove(item);
            shopAdapter.notifyDataSetChanged();

            Toast.makeText(this, R.string.shop_item_purchase_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.shop_item_insufficient_funds, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEquipClick(ShopItem item) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            Toast.makeText(this, getString(R.string.login_required_equip), Toast.LENGTH_LONG).show();
            return;
        }
        // Desequipar item actual del mismo tipo
        for (ShopItem shopItem : shopItems) {
            if (shopItem.getType() == item.getType() && shopItem.isEquipped()) {
                shopItem.setEquipped(false);
            }
        }

        // Equipar nuevo item
        item.setEquipped(true);
        saveItemStates();
        shopAdapter.notifyDataSetChanged();

        Toast.makeText(this, R.string.shop_item_equip_success, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_rewards;
    }

    private void mostrarRecompensasEnBarra() {
        LinearLayout rewardsContainer = findViewById(R.id.rewards_slots_container);
        rewardsContainer.removeAllViews();

        List<RewardManager.RewardData> rewards = rewardManager.getAllRewards();
        int currentLevel = rewardManager.getCurrentLevel();

        for (RewardManager.RewardData reward : rewards) {
            // Skip if reward is already claimed
            if (rewardManager.isRewardRemoved(reward.key)) {
                continue;
            }

            View rewardView = getLayoutInflater().inflate(R.layout.reward_slot_item, rewardsContainer, false);
            
            // Set reward data
            ImageView iconView = rewardView.findViewById(R.id.reward_icon);
            TextView nameView = rewardView.findViewById(R.id.reward_name);
            TextView levelView = rewardView.findViewById(R.id.reward_level);
            View lockOverlay = rewardView.findViewById(R.id.lock_overlay);
            ImageView lockIcon = rewardView.findViewById(R.id.lock_icon);

            iconView.setImageResource(reward.iconResId);
            nameView.setText(reward.getTitle(this));
            levelView.setText(getString(R.string.level_required, reward.streakThreshold));

            boolean isLocked = currentLevel < reward.streakThreshold;

            lockOverlay.setVisibility(isLocked ? View.VISIBLE : View.GONE);
            lockIcon.setVisibility(isLocked ? View.VISIBLE : View.GONE);

            if (!isLocked) {
                rewardView.setOnClickListener(v -> claimReward(reward, rewardView));
            }

            rewardsContainer.addView(rewardView);
        }

        // Update next reward info
        updateNextRewardInfo();
    }

    private void updateNextRewardInfo() {
        TextView nextRewardText = findViewById(R.id.next_reward_text);
        TextView nextRewardRequirement = findViewById(R.id.next_reward_requirement);
        int currentLevel = rewardManager.getCurrentLevel();
        int nextLevel = rewardManager.getNextRewardThreshold(currentLevel);

        if (nextLevel > currentLevel) {
            RewardManager.RewardData nextReward = null;
            for (RewardManager.RewardData reward : rewardManager.getAllRewards()) {
                if (reward.streakThreshold == nextLevel) {
                    nextReward = reward;
                    break;
                }
            }

            if (nextReward != null) {
                nextRewardText.setText(getString(R.string.next_reward_format, nextReward.getTitle(this)));
                nextRewardRequirement.setText(getString(R.string.level_required, nextLevel));
            }
        } else {
            nextRewardText.setText(R.string.all_rewards_claimed);
            nextRewardRequirement.setVisibility(View.GONE);
        }
    }

    private void claimReward(RewardManager.RewardData reward, View rewardView) {
        // Mostrar animación de reclamación
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(rewardView, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(rewardView, "scaleY", 1f, 1.2f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(rewardView, "alpha", 1f, 0f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    // Marcar recompensa como reclamada localmente
                    rewardManager.markRewardAsRemoved(reward.key);
                    
                    // Remover la vista del contenedor
                    ViewGroup parent = (ViewGroup) rewardView.getParent();
                    if (parent != null) {
                        parent.removeView(rewardView);
                    }
                    
                    // Agregar gemas
                    int gemReward = calculateGemReward(reward);
                    addCurrency(gemReward);
                    
                    // Mostrar animación de partículas de gemas
                    showGemParticlesAnimation(rewardView, gemReward);
                    
                    // Actualizar información de la siguiente recompensa
                    updateNextRewardInfo();

                    // Guardar en Firebase
                    saveRewardToFirebase(reward, gemReward);

                } catch (Exception e) {
                    Log.e(TAG, "Error durante el proceso de reclamación de recompensa: " + reward.key, e);
                }
            }
        });
        
        animatorSet.start();
    }

    private int calculateGemReward(RewardManager.RewardData reward) {
        // Base gem reward on reward level
        if ("novice".equals(reward.key)) {
            return 50; // Specific reward for novice
        } else {
            return reward.streakThreshold * 10; // Existing calculation for others
        }
    }

    private void showGemParticlesAnimation(View sourceView, int gemAmount) {
        // Create gem text view
        TextView gemText = new TextView(this);
        gemText.setText("+" + gemAmount);
        gemText.setTextColor(getResources().getColor(R.color.accent_color));
        gemText.setTextSize(20);
        
        // Add gem text to parent view
        ViewGroup parent = (ViewGroup) sourceView.getParent();
        parent.addView(gemText);
        
        // Position gem text
        int[] location = new int[2];
        sourceView.getLocationInWindow(location);
        gemText.setX(location[0] + sourceView.getWidth() / 2f);
        gemText.setY(location[1] + sourceView.getHeight() / 2f);
        
        // Create gem icons
        for (int i = 0; i < 5; i++) {
            ImageView gemIcon = new ImageView(this);
            gemIcon.setImageResource(R.drawable.ic_gem);
            gemIcon.setAlpha(0.8f);
            parent.addView(gemIcon);
            
            // Position gem icon
            gemIcon.setX(location[0] + sourceView.getWidth() / 2f);
            gemIcon.setY(location[1] + sourceView.getHeight() / 2f);
            
            // Animate gem icon
            float angle = (float) (i * Math.PI / 2.5f);
            float distance = 200f;
            float targetX = gemIcon.getX() + (float) (Math.cos(angle) * distance);
            float targetY = gemIcon.getY() + (float) (Math.sin(angle) * distance);
            
            ObjectAnimator translateX = ObjectAnimator.ofFloat(gemIcon, "translationX", 0f, targetX - gemIcon.getX());
            ObjectAnimator translateY = ObjectAnimator.ofFloat(gemIcon, "translationY", 0f, targetY - gemIcon.getY());
            ObjectAnimator alpha = ObjectAnimator.ofFloat(gemIcon, "alpha", 0.8f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(gemIcon, "scaleX", 1f, 0.5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(gemIcon, "scaleY", 1f, 0.5f);
            
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(translateX, translateY, alpha, scaleX, scaleY);
            animatorSet.setDuration(1000);
            animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
            
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    parent.removeView(gemIcon);
                }
            });
            
            animatorSet.start();
        }
        
        // Animate gem text
        ObjectAnimator translateY = ObjectAnimator.ofFloat(gemText, "translationY", 0f, -100f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(gemText, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(gemText, "scaleX", 1f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(gemText, "scaleY", 1f, 1.5f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translateY, alpha, scaleX, scaleY);
        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                parent.removeView(gemText);
            }
        });
        
        animatorSet.start();
    }

    private void saveRewardToFirebase(RewardManager.RewardData reward, int gemReward) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(user.getUid());

        // Obtener gemas actuales
        int currentGems = prefs.getInt(CURRENCY_KEY, 0);
        int newGems = currentGems + gemReward;

        // Actualizar Firebase con nueva recompensa y gemas
        Map<String, Object> updates = new HashMap<>();
        updates.put("claimed_rewards." + reward.key, true);
        updates.put("gems", newGems);
        updates.put("last_reward_claimed", System.currentTimeMillis());

        userRef.update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Recompensa y gemas guardadas en Firebase exitosamente");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al guardar recompensa y gemas en Firebase", e);
                // Revertir cambios locales si la actualización de Firebase falla
                rewardManager.markRewardAsRemoved(reward.key);
                prefs.edit().putInt(CURRENCY_KEY, currentGems).apply();
                updateCurrencyDisplay();
                mostrarRecompensasEnBarra(); // Actualizar la visualización de recompensas
            });
    }

    private void saveShopItemToFirebase(ShopItem item) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(user.getUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put("purchased_items." + item.getId(), true);

        userRef.update(updates)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Shop item purchase saved to Firebase: " + item.getId()))
            .addOnFailureListener(e -> Log.e(TAG, "Error saving shop item purchase to Firebase: " + item.getId(), e));
    }

    private void handlePurchasedItemAction(ShopItem item) {
        switch (item.getType()) {
            case AVATAR:
                // Por ahora, solo marcar como comprado. Futuro: permitir equipar
                break;
            case FRAME:
                // Equipar el marco inmediatamente
                onEquipClick(item); // Llamar a la lógica de equipamiento existente
                break;
            // Agregar otros tipos de items según sea necesario
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync rewards and gems with Firebase
        syncWithFirebase();
    }

    private void syncWithFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(user.getUid());

        userRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Actualizar gemas
                    Long gems = documentSnapshot.getLong("gems");
                    if (gems != null) {
                        prefs.edit().putInt(CURRENCY_KEY, gems.intValue()).apply();
                        updateCurrencyDisplay();
                    }

                    // Actualizar recompensas reclamadas
                    Map<String, Boolean> claimedRewards = (Map<String, Boolean>) documentSnapshot.get("claimed_rewards");
                    if (claimedRewards != null) {
                        for (Map.Entry<String, Boolean> entry : claimedRewards.entrySet()) {
                            if (entry.getValue()) {
                                rewardManager.markRewardAsRemoved(entry.getKey());
                            }
                        }
                        mostrarRecompensasEnBarra();
                    }

                    // Actualizar items comprados y equipados
                    Map<String, Boolean> purchasedItems = (Map<String, Boolean>) documentSnapshot.get("purchased_items");
                    if (purchasedItems != null) {
                        for (ShopItem item : shopItems) {
                            if (purchasedItems.containsKey(item.getId()) && purchasedItems.get(item.getId())) {
                                item.setPurchased(true);
                            }
                        }
                        // Reconstruir lista de items de la tienda y notificar al adaptador después de sincronizar items comprados
                        initializeShopItems(); // Reinicializar para filtrar items comprados
                        shopAdapter = new ShopAdapter(RewardsActivity.this, shopItems, RewardsActivity.this);
                        shopRecyclerView.setAdapter(shopAdapter);
                    }

                    // Actualizar items equipados desde Firebase
                    String equippedAvatar = documentSnapshot.getString(EQUIPPED_AVATAR_KEY);
                    String equippedFrame = documentSnapshot.getString(EQUIPPED_FRAME_KEY);
                    
                    if (equippedAvatar != null) prefs.edit().putString(EQUIPPED_AVATAR_KEY, equippedAvatar).apply();
                    if (equippedFrame != null) prefs.edit().putString(EQUIPPED_FRAME_KEY, equippedFrame).apply();
                    
                    // Necesario actualizar la visualización de la tienda después de cargar el estado de equipamiento
                    initializeShopItems(); // Reinicializar para actualizar estado de equipamiento
                    shopAdapter = new ShopAdapter(RewardsActivity.this, shopItems, RewardsActivity.this);
                    shopRecyclerView.setAdapter(shopAdapter);

                } else {
                    Log.d(TAG, "User document not found in Firebase");
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar con Firebase", e));
    }
} 