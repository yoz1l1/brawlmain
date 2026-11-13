package ru.arena.mc;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Звуки боя из стандартных звуков Minecraft (своих файлов нет — нечего нарушать). */
public final class Sfx {
    private static final Map<String, Long> LAST = new HashMap<>();

    private Sfx() {}

    public static void play(SoundEvent ev, float pitch) {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ev, pitch));
    }

    public static void play(RegistryEntry<SoundEvent> ev, float pitch) {
        play(ev.value(), pitch);
    }

    public static void event(String key) {
        long now = System.currentTimeMillis();
        Long last = LAST.get(key);
        long gap = key.equals("hit") ? 90 : 60;
        if (last != null && now - last < gap) return;
        LAST.put(key, now);
        switch (key) {
            case "click": play(SoundEvents.UI_BUTTON_CLICK, 1.0f); break;
            case "hit": play(SoundEvents.ENTITY_PLAYER_HURT, 1.7f); break;
            case "explosion": play(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.4f); break;
            case "dash": play(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.9f); break;
            case "zap": play(SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f); break;
            case "slash": play(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f); break;
            case "super": play(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.1f); break;
            case "heal": play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f); break;
            case "death": play(SoundEvents.ENTITY_PLAYER_HURT, 0.6f); break;
            case "invite": play(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.2f); break;
            default: break;
        }
    }
}
