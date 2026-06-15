package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourname.dailyreward.event.WhitelistManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.*;

public class MingDanCommand {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("mingdan").requires(s -> s.hasPermissionLevel(4))
                .then(literal("add").then(argument("player", StringArgumentType.word()).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "player");
                    WhitelistManager.addPlayer(name);
                    ctx.getSource().sendFeedback(() -> Text.literal("§a已将 "+name+" 加入白名单"), false);
                    return 1;
                })))
                .then(literal("remove").then(argument("player", StringArgumentType.word()).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "player");
                    WhitelistManager.removePlayer(name);
                    ctx.getSource().sendFeedback(() -> Text.literal("§c已将 "+name+" 移出白名单"), false);
                    return 1;
                })))
                .then(literal("list").executes(ctx -> {
                    var list = WhitelistManager.getWhitelist();
                    if(list.isEmpty()) ctx.getSource().sendFeedback(() -> Text.literal("§e白名单为空"), false);
                    else { ctx.getSource().sendFeedback(() -> Text.literal("§6===== 白名单列表 ====="), false); for(String n : list) ctx.getSource().sendFeedback(() -> Text.literal("§7- §f"+n), false); }
                    return 1;
                }))
        );
    }
}