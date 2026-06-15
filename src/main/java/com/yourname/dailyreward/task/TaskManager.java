package com.yourname.dailyreward.task;

import com.yourname.dailyreward.data.PlayerDataManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private static final Map<UUID, Map<UUID, Double>> dragonDamage = new ConcurrentHashMap<>();
    public static void register() {
        ServerLivingEntityEvents.DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof EnderDragonEntity dragon && source.getAttacker() instanceof PlayerEntity p) {
                dragonDamage.computeIfAbsent(dragon.getUuid(), k -> new ConcurrentHashMap<>()).merge(p.getUuid(), amount, Double::sum);
            }
            return amount;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof EnderDragonEntity dragon) {
                Map<UUID, Double> map = dragonDamage.remove(dragon.getUuid());
                if (map == null || map.isEmpty()) return;
                double total = map.values().stream().mapToDouble(Double::doubleValue).sum();
                if (total <= 0) return;
                int totalReward = 200;
                for (Map.Entry<UUID, Double> e : map.entrySet()) {
                    int reward = (int) Math.max(1, Math.round(totalReward * e.getValue() / total));
                    ServerPlayerEntity p = entity.getServer().getPlayerManager().getPlayer(e.getKey());
                    if (p != null) {
                        PlayerDataManager.addPoints(p, reward);
                        p.sendMessage(Text.literal("§a[任务] 参与击杀末影龙，获得 " + reward + " 积分"));
                    }
                }
                entity.getServer().getPlayerManager().broadcast(Text.literal("§6[系统] 末影龙被击杀！参与玩家按伤害获得积分奖励！"), false);
            }
        });
    }
    public static List<String> getTaskList() {
        return Arrays.asList("§e击杀末影龙 §7- 按伤害分配200积分", "§e完成成就 §7- 普通5/目标10/挑战30积分");
    }
}