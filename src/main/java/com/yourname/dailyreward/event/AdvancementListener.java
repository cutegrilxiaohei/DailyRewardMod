package com.yourname.dailyreward.event;

import com.yourname.dailyreward.data.PlayerDataManager;
import net.fabricmc.fabric.api.advancement.v1.AdvancementEvents;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.server.network.ServerPlayerEntity;

public class AdvancementListener {
    public static void register() {
        AdvancementEvents.CRITERIA_COMPLETE.register((player, advancement, criterionName) -> {
            if (!(player instanceof ServerPlayerEntity sp)) return;
            var display = advancement.display();
            if (display == null) return;
            AdvancementFrame frame = display.getFrame();
            int points = 0;
            boolean global = false;
            if (frame == AdvancementFrame.TASK) points = 5;
            else if (frame == AdvancementFrame.GOAL) points = 10;
            else if (frame == AdvancementFrame.CHALLENGE) { points = 30; global = true; }
            if (points > 0) {
                PlayerDataManager.rewardAdvancement(sp, advancement.getId().toString(), criterionName, points, global);
            }
        });
    }
}