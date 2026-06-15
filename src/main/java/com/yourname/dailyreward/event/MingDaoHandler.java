package com.yourname.dailyreward.event;

import com.yourname.dailyreward.data.PlayerDataManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.text.Text;

public class MingDaoHandler {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            float newHealth = entity.getHealth() - amount;
            if (newHealth <= 0 && PlayerDataManager.hasMingDao(player)) {
                teleportToRespawn(player);
                player.setHealth(player.getMaxHealth());
                player.clearStatusEffects();
                PlayerDataManager.consumeMingDao(player);
                player.sendMessage(Text.literal("§d[名刀·司命] 触发！传送回重生点并恢复生命"), false);
                PlayerDataManager.tryAutoRenewMingDao(player);
                return false;
            }
            return true;
        });
    }
    private static void teleportToRespawn(ServerPlayerEntity player) {
        BlockPos pos = player.getSpawnPointPosition();
        float angle = player.getSpawnAngle();
        if (pos != null) {
            var dim = player.getSpawnPointDimension();
            if (dim == null) dim = World.OVERWORLD;
            var world = player.getServer().getWorld(dim);
            if (world != null) {
                player.teleport(world, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, angle, 0);
                return;
            }
        }
        World overworld = player.getServer().getOverworld();
        BlockPos spawn = overworld.getSpawnPos();
        player.teleport(overworld, spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5, player.getYaw(), player.getPitch());
    }
}