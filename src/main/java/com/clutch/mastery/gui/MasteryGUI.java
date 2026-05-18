package com.clutch.mastery.gui;

import com.clutch.mastery.manager.MasteryManager;
import com.clutch.mastery.model.MasteryData;
import com.clutch.mastery.model.MasteryType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MasteryGUI {
    public static final String TITLE = "숙련도 [CLUTCH]";
    private final MasteryManager masteryManager;

    public MasteryGUI(MasteryManager masteryManager) {
        this.masteryManager = masteryManager;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(TITLE));
        inventory.setItem(11, createIcon(player, MasteryType.FARMING));
        inventory.setItem(13, createIcon(player, MasteryType.MINING));
        inventory.setItem(15, createIcon(player, MasteryType.FISHING));
        player.openInventory(inventory);
    }

    private ItemStack createIcon(Player player, MasteryType type) {
        MasteryData data = masteryManager.getData(player.getUniqueId(), type);
        int maxLevel = masteryManager.getMaxLevel();
        int required = masteryManager.getRequiredExp(data.getLevel());
        int remaining = data.getLevel() >= maxLevel ? 0 : Math.max(0, required - data.getExp());
        ItemStack itemStack = new ItemStack(type.getIcon());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§a" + type.getKoreanName() + " 숙련도"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§f현재 레벨: §aLv." + data.getLevel() + " / " + maxLevel));
            lore.add(Component.text("§f경험치: §e" + data.getExp() + " / " + required));
            lore.add(Component.text("§f다음 레벨까지: §e" + remaining + " EXP"));
            lore.add(Component.text(String.format("§f추가 획득 확률: §b%.1f%%", masteryManager.getBonusChance(data.getLevel()))));
            lore.add(Component.text(""));
            lore.add(Component.text("§f현재 활성 가능한 효과: §a" + activeEffects(type, data.getLevel())));
            lore.add(Component.text("§7Lv.25 보상: " + type.getLevel25Reward()));
            lore.add(Component.text("§7Lv.50 보상: " + type.getLevel50Reward()));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private String activeEffects(MasteryType type, int level) {
        if (level >= 50) {
            return type.getLevel25Reward() + ", " + type.getLevel50Reward();
        }
        if (level >= 25) {
            return type.getLevel25Reward();
        }
        return "없음";
    }
}
