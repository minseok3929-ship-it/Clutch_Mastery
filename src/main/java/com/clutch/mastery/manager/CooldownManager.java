package com.clutch.mastery.manager;

import com.clutch.mastery.model.MasteryType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean tryUse(UUID uuid, MasteryType type, long cooldownMillis) {
        if (cooldownMillis <= 0L) {
            return true;
        }

        long now = System.currentTimeMillis();
        String key = uuid + ":" + type.name();
        Long previous = cooldowns.get(key);
        if (previous != null && now - previous < cooldownMillis) {
            return false;
        }
        cooldowns.put(key, now);
        return true;
    }

    public void clear(UUID uuid) {
        String prefix = uuid + ":";
        cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
    }
}
