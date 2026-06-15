package com.yourname.dailyreward;

import com.yourname.dailyreward.anticheat.AntiCheatManager;
import com.yourname.dailyreward.command.*;
import com.yourname.dailyreward.data.PlayerDataManager;
import com.yourname.dailyreward.event.*;
import com.yourname.dailyreward.scoreboard.ScoreboardService;
import com.yourname.dailyreward.task.TaskManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DailyRewardMod implements ModInitializer {
    public static final String MOD_ID = "dailyreward";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("加载模组...");
        PlayerDataManager.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            QdCommand.register(dispatcher);
            QdOpCommand.register(dispatcher);
            DuiHuanCommand.register(dispatcher);
            DuiHuanConfirmCommand.register(dispatcher);
            PayCommand.register(dispatcher);
            PayConfirmCommand.register(dispatcher);
            PointsCommand.register(dispatcher);
            ChenghaoCommand.register(dispatcher);
            FuliCommand.register(dispatcher);
            MingDanCommand.register(dispatcher);
            RenwuCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerDataManager.setServer(server);
            OnlineTimeHandler.init(server);
            ScoreboardService.init(server);
            AntiCheatManager.init(server);
            WhitelistManager.init(server);
            DuiHuanCommand.startCleanupTask(server);
            PayCommand.startCleanupTask(server);
            // 定时保存数据（每5分钟）
            server.getTickManager().schedule(() -> {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
                    PlayerDataManager.savePlayer(p);
                LOGGER.debug("定时保存玩家数据");
            }, 20 * 60 * 5, 20 * 60 * 5);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerDataManager.saveAll();
            LOGGER.info("数据已保存");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerDataManager.loadPlayer(handler.getPlayer());
            PlayerDataManager.grantAllWelfare(handler.getPlayer());
            ScoreboardService.updateForPlayer(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerDataManager.savePlayer(handler.getPlayer());
            ScoreboardService.removePlayer(handler.getPlayer());
            AntiCheatManager.resetAll(handler.getPlayer());
        });

        DeathListener.register();
        MingDaoHandler.register();
        TaskManager.register();
        AdvancementListener.register();
        LOGGER.info("模组加载完成");
    }
}