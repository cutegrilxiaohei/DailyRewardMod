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
import net.minecraft.text.TextColor;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import static net.minecraft.server.command.CommandManager.*;

public class DuiHuanCommand {
    private static final long EXPIRE = TimeUnit.SECONDS.toMillis(60);
    public enum Type { DIPI, JIAREN, CHENGHAO, TPDIE, MINGDAO }
    public static class Request {
        public Type type; public int cost; public int blocks; public String title; public long time;
        public Request(Type t, int c, int b, String ti) { type=t; cost=c; blocks=b; title=ti; time=System.currentTimeMillis(); }
        public boolean expired() { return System.currentTimeMillis() - time > EXPIRE; }
    }
    public static final ConcurrentHashMap<UUID, Request> pending = new ConcurrentHashMap<>();
    public static void startCleanupTask(MinecraftServer s) {
        s.getTickManager().schedule(() -> pending.entrySet().removeIf(e -> e.getValue().expired()), 20, 20*30);
    }
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("duihuan").executes(ctx -> {
                            ctx.getSource().getPlayer().sendMessage(Text.literal("§6用法: /duihuan dipi|jiaren|chenghao|tpdie|mingdao"), false);
                            return 1;
                        })
                        .then(literal("dipi").then(argument("points", IntegerArgumentType.integer(1)).executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
                            int pts = IntegerArgumentType.getInteger(ctx, "points");
                            int blocks = pts/10; if(blocks==0) { p.sendMessage(Text.literal("§c积分不足10"), false); return 0; }
                            int cost = blocks*10;
                            if(PlayerDataManager.getTotalPoints(p) < cost) { p.sendMessage(Text.literal("§c积分不足"), false); return 0; }
                            pending.put(p.getUuid(), new Request(Type.DIPI, cost, blocks, null));
                            sendConfirm(p, "消耗§e"+cost+"§6积分兑换§a"+blocks+"§6个地皮");
                            return 1;
                        })))
                        .then(literal("jiaren").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
                            if(PlayerDataManager.getTotalPoints(p) < 100) { p.sendMessage(Text.literal("§c积分不足100"), false); return 0; }
                            pending.put(p.getUuid(), new Request(Type.JIAREN, 100, 0, null));
                            sendConfirm(p, "消耗§e100§6积分兑换一个假人");
                            return 1;
                        }))
                        .then(literal("chenghao").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
                            PlayerDataManager.setCurrentTitle(p, "");
                            p.sendMessage(Text.literal("§a已删除当前称号"), false);
                            return 1;
                        }).then(argument("title", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
                            String title = StringArgumentType.getString(ctx, "title").replace("&", "§");
                            if(title.length()>20) { p.sendMessage(Text.literal("§c称号过长"), false); return 0; }
                            if(PlayerDataManager.getTotalPoints(p) < 100) { p.sendMessage(Text.literal("§c积分不足100"), false); return 0; }
                            pending.put(p.getUuid(), new Request(Type.CHENGHAO, 100, 0, title));
                            sendConfirm(p, "消耗§e100§6积分购买称号 §f<"+title+"§f>");
                            return 1;
                        })))
                        .then(literal("tpdie").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
                            if(PlayerDataManager.getTotalPoints(p) < 30) { p.sendMessage(Text.literal("§c积分不足30"), false); return 0; }
                            if(!PlayerDataManager.hasLastDeathLocation(p)) { p.sendMessage(Text.literal("§c没有死亡记录"), false); return 0; }
                            pending.put(p.getUuid(), new Request(Type.TPDIE, 30, 0, null));
                            sendConfirm(p, "消耗§e30§6积分传送至上次死亡点");
                            return 1;
                        }))
                        .then(literal("mingdao").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
                            if(PlayerDataManager.getTotalPoints(p) < 10) { p.sendMessage(Text.literal("§c积分不足10"), false); return 0; }
                            pending.put(p.getUuid(), new Request(Type.MINGDAO, 10, 0, null));
                            sendConfirm(p, "消耗§e10§6积分购买名刀·司命");
                            return 1;
                        }))
        );
    }
    private static void sendConfirm(ServerPlayerEntity p, String desc) {
        Text c = Text.literal("§a[确认]").styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duihuan_confirm")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击确认"))));
        Text d = Text.literal("§c[拒绝]").styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duihuan_cancel")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击取消"))));
        p.sendMessage(Text.literal("§6[系统]：是否" + desc + "？ ").append(c).append(Text.literal(" ")).append(d), false);
    }
}