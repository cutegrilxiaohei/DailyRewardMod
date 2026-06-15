package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import static net.minecraft.server.command.CommandManager.*;

public class PayCommand {
    private static final long EXPIRE = TimeUnit.SECONDS.toMillis(60);
    public static class Req { String target; int amount; long time; Req(String t, int a) { target=t; amount=a; time=System.currentTimeMillis(); } boolean expired() { return System.currentTimeMillis()-time>EXPIRE; } }
    public static final ConcurrentHashMap<UUID, Req> pending = new ConcurrentHashMap<>();
    public static void startCleanupTask(MinecraftServer s) { s.getTickManager().schedule(() -> pending.entrySet().removeIf(e -> e.getValue().expired()), 20, 20*30); }
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("pay").then(argument("player", StringArgumentType.word()).then(argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
            ServerPlayerEntity from = ctx.getSource().getPlayer(); if(from==null) return 0;
            String target = StringArgumentType.getString(ctx, "player");
            int amt = IntegerArgumentType.getInteger(ctx, "amount");
            if(target.equalsIgnoreCase(from.getName().getString())) { from.sendMessage(Text.literal("§c不能给自己转账"), false); return 0; }
            ServerPlayerEntity to = from.getServer().getPlayerManager().getPlayer(target);
            if(to == null) { from.sendMessage(Text.literal("§c玩家不在线"), false); return 0; }
            if(PlayerDataManager.getPoints(from) < amt) { from.sendMessage(Text.literal("§c普通积分不足"), false); return 0; }
            pending.put(from.getUuid(), new Req(target, amt));
            Text c = Text.literal("§a[确认]").styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pay_confirm")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("确认支付"))));
            Text d2 = Text.literal("§c[拒绝]").styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pay_cancel")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("取消支付"))));
            from.sendMessage(Text.literal("§6是否支付 §e"+amt+"§6 给 §a"+target+"§6？ ").append(c).append(Text.literal(" ")).append(d2), false);
            return 1;
        }))));
    }
}