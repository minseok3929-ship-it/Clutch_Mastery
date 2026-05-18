package com.clutch.mastery.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

public enum MasteryType {
    FARMING("농사", Material.WHEAT, "작물 수확 시 점프 강화 I", "작물 수확 시 신속 I"),
    MINING("채광", Material.DIAMOND_PICKAXE, "광물 채굴 시 성급함 I", "광물 채굴 시 야간투시"),
    FISHING("낚시", Material.FISHING_ROD, "돌고래의 은총 I 상시 적용", "수중호흡 상시 적용");

    private final String koreanName;
    private final Material icon;
    private final String level25Reward;
    private final String level50Reward;

    MasteryType(String koreanName, Material icon, String level25Reward, String level50Reward) {
        this.koreanName = koreanName;
        this.icon = icon;
        this.level25Reward = level25Reward;
        this.level50Reward = level50Reward;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getLevel25Reward() {
        return level25Reward;
    }

    public String getLevel50Reward() {
        return level50Reward;
    }

    public String getConfigKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<MasteryType> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        for (MasteryType type : values()) {
            if (type.name().equals(normalized) || type.koreanName.equals(input.trim())) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
