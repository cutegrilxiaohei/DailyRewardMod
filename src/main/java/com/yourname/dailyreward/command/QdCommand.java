package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

public class QdCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("qd").executes(ctx -> {
            var p = ctx.getSource().getPlayer();
            if (p == null) return 0;
            if (PlayerDataManager.hasSignedToday(p)) {
                p.sendMessage(Text.literal("§c你今天已经签过到了！"), false);
                return 0;
            }
            int reward = PlayerDataManager.sign(p);
            String msg = String.format("§6[系统]：玩家§a%s§r签到成功，积分+§e%d", p.getName().getString(), reward);
            for (ServerPlayerEntity online : p.getServer().getPlayerManager().getPlayerList()) {
                online.sendMessage(Text.literal(msg), false);
            }
            p.sendMessage(Text.literal("§a✅ 签到成功 +" + reward + " 积分"), true);
            return 1;
        }));
    }
}