package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

public class PointsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("积分").executes(ctx -> {
            var p = ctx.getSource().getPlayer(); if(p==null) return 0;
            p.sendMessage(Text.literal("§6普通积分: §e"+PlayerDataManager.getPoints(p)+" §6绑定积分: §e"+PlayerDataManager.getBindingPoints(p)), false);
            return 1;
        }));
        d.register(literal("points").executes(ctx -> {
            var p = ctx.getSource().getPlayer(); if(p==null) return 0;
            p.sendMessage(Text.literal("§6Normal: §e"+PlayerDataManager.getPoints(p)+" §6Binding: §e"+PlayerDataManager.getBindingPoints(p)), false);
            return 1;
        }));
    }
}