package com.yourname.dailyreward.event;

import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.text.Text;

public class OnlineTimeHandler {
    private static final int NORMAL = 20 * 60 * 30;
    private static final int REDUCED = 20 * 60 * 60;
    private static final long THREE_HOURS = 20 * 60 * 60 * 3;

    public static void init(MinecraftServer server) {
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                PlayerDataManager.addTodayOnlineTicks(p, 1);
                PlayerDataManager.addAccumulatedTicks(p, 1);
                int today = PlayerDataManager.getTodayOnlineTicks(p);
                int interval = today >= THREE_HOURS ? REDUCED : NORMAL;
                int cur = PlayerDataManager.getAccumulatedTicks(p);
                while (cur >= interval) {
                    PlayerDataManager.addBindingPoints(p, 10);
                    p.sendMessage(Text.literal("⏰ 在线时长奖励 +10 绑定积分"), true);
                    PlayerDataManager.modifyAccumulatedTicks(p, -interval);
                    cur -= interval;
                }
            }
        });
    }
}