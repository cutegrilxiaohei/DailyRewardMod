package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import static net.minecraft.server.command.CommandManager.*;

public class QdOpCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("qdop").requires(s -> s.hasPermissionLevel(4))
                .then(literal("add").then(argument("player", StringArgumentType.word()).then(argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "player");
                    int amt = IntegerArgumentType.getInteger(ctx, "amount");
                    var target = ctx.getSource().getServer().getPlayerManager().getPlayer(name);
                    if (target == null) { ctx.getSource().sendError(Text.literal("玩家不在线")); return 0; }
                    PlayerDataManager.addPoints(target, amt);
                    ctx.getSource().sendFeedback(() -> Text.literal("已为 " + name + " 增加 " + amt + " 积分"), false);
                    return 1;
                }))))
                .then(literal("remove").then(argument("player", StringArgumentType.word()).then(argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "player");
                    int amt = IntegerArgumentType.getInteger(ctx, "amount");
                    var target = ctx.getSource().getServer().getPlayerManager().getPlayer(name);
                    if (target == null) { ctx.getSource().sendError(Text.literal("玩家不在线")); return 0; }
                    int cur = PlayerDataManager.getPoints(target);
                    PlayerDataManager.setPoints(target, Math.max(0, cur - amt));
                    ctx.getSource().sendFeedback(() -> Text.literal("已为 " + name + " 减少 " + amt + " 积分"), false);
                    return 1;
                }))))
                .then(literal("set").then(argument("player", StringArgumentType.word()).then(argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "player");
                    int amt = IntegerArgumentType.getInteger(ctx, "amount");
                    var target = ctx.getSource().getServer().getPlayerManager().getPlayer(name);
                    if (target == null) { ctx.getSource().sendError(Text.literal("玩家不在线")); return 0; }
                    PlayerDataManager.setPoints(target, amt);
                    ctx.getSource().sendFeedback(() -> Text.literal("已将 " + name + " 积分设为 " + amt), false);
                    return 1;
                }))))
                .then(literal("look").executes(ctx -> {
                    var list = PlayerDataManager.getAllPlayerData();
                    if (list.isEmpty()) { ctx.getSource().sendFeedback(() -> Text.literal("暂无数据"), false); return 1; }
                    ctx.getSource().sendFeedback(() -> Text.literal("===== 玩家积分列表 ====="), false);
                    int i = 1;
                    LocalDate today = LocalDate.now();
                    for (var e : list) {
                        String last = e.lastSignDate == null ? "从未签到" : (e.lastSignDate.equals(today) ? "今天" : ChronoUnit.DAYS.between(e.lastSignDate, today) + "天前");
                        ctx.getSource().sendFeedback(() -> Text.literal(String.format("§e%d. §f%s §a积分%d §7上次签到§f%s", i++, e.name, e.points, last)), false);
                    }
                    return 1;
                }))
        );
    }
}