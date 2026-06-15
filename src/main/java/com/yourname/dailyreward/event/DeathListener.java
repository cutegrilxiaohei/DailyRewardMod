package com.yourname.dailyreward.event;

import com.yourname.dailyreward.data.PlayerDataManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class DeathListener {
    public static void register() {
        ServerPlayerEvents.AFTER_DEATH.register((player, source, damageSource) -> {
            PlayerDataManager.addDeath((ServerPlayerEntity) player);
            PlayerDataManager.setLastDeathLocation((ServerPlayerEntity) player);
        });
    }
}