package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yourname.dailyreward.task.TaskManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

public class RenwuCommand {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("renwu").executes(ctx -> {
            var p = ctx.getSource().getPlayer(); if(p==null) return 0;
            p.sendMessage(Text.literal("§6========= 任务列表 ========="), false);
            for(String s : TaskManager.getTaskList()) p.sendMessage(Text.literal(s), false);
            return 1;
        }));
    }
}