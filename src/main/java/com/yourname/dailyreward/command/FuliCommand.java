package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.*;

public class FuliCommand {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("fuli").requires(s -> s.hasPermissionLevel(4))
                .then(literal("jifen").then(argument("msg", StringArgumentType.greedyString()).then(argument("points", IntegerArgumentType.integer(1)).executes(ctx -> {
                    String msg = StringArgumentType.getString(ctx, "msg");
                    int pts = IntegerArgumentType.getInteger(ctx, "points");
                    PlayerDataManager.addWelfare("jifen", msg, pts, "");
                    PlayerDataManager.grantAllWelfareToOnline(ctx.getSource().getServer());
                    ctx.getSource().sendFeedback(() -> Text.literal("§a已添加积分福利"), false);
                    return 1;
                }))))
                .then(literal("chenghao").then(argument("title", StringArgumentType.greedyString()).then(argument("msg", StringArgumentType.greedyString()).executes(ctx -> {
                    String title = StringArgumentType.getString(ctx, "title").replace("&", "§");
                    String msg = StringArgumentType.getString(ctx, "msg");
                    PlayerDataManager.addWelfare("chenghao", msg, 0, title);
                    PlayerDataManager.grantAllWelfareToOnline(ctx.getSource().getServer());
                    ctx.getSource().sendFeedback(() -> Text.literal("§a已添加称号福利"), false);
                    return 1;
                }))))
        );
    }
}