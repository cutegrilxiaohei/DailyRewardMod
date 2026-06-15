package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

public class DuiHuanConfirmCommand {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("duihuan_confirm").executes(ctx -> {
            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
            var req = DuiHuanCommand.pending.remove(p.getUuid());
            if(req == null || req.expired()) { p.sendMessage(Text.literal("§c请求已过期"), false); return 0; }
            if(!PlayerDataManager.deductPoints(p, req.cost)) { p.sendMessage(Text.literal("§c积分不足"), false); return 0; }
            switch(req.type) {
                case DIPI:
                    p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource().withPermissionLevel(4), "flan giveClaimBlocks "+p.getName().getString()+" "+req.blocks);
                    p.sendMessage(Text.literal("§a兑换成功！地皮 +"+req.blocks), false);
                    break;
                case JIAREN:
                    int num = PlayerDataManager.getNextBotNumber();
                    p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource().withPermissionLevel(4), "player bot"+num+" spawn");
                    p.sendMessage(Text.literal("§a兑换成功！已召唤假人 bot"+num), false);
                    break;
                case CHENGHAO:
                    String title = "§f<"+req.title+">";
                    PlayerDataManager.addTitle(p, title, System.currentTimeMillis()+7L*24*60*60*1000);
                    PlayerDataManager.setCurrentTitle(p, title);
                    p.sendMessage(Text.literal("§a兑换成功！获得称号 "+title), false);
                    break;
                case TPDIE:
                    PlayerDataManager.teleportToLastDeath(p);
                    p.sendMessage(Text.literal("§a已传送至上次死亡点"), false);
                    break;
                case MINGDAO:
                    PlayerDataManager.setMingDao(p, true);
                    p.sendMessage(Text.literal("§a兑换成功！获得名刀·司命效果"), false);
                    Text yes = Text.literal("§a[是]").styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mingdao_auto")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("开启自动续费"))));
                    Text no = Text.literal("§c[否]").styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mingdao_no")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("关闭自动续费"))));
                    p.sendMessage(Text.literal("§6是否开启自动续费？ ").append(yes).append(Text.literal(" ")).append(no), false);
                    break;
            }
            return 1;
        }));
        d.register(literal("duihuan_cancel").executes(ctx -> {
            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
            if(DuiHuanCommand.pending.remove(p.getUuid()) != null) p.sendMessage(Text.literal("§e已取消"), false);
            else p.sendMessage(Text.literal("§c无待处理请求"), false);
            return 1;
        }));
        d.register(literal("mingdao_auto").executes(ctx -> {
            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p!=null) { PlayerDataManager.setAutoRenewMingDao(p, true); p.sendMessage(Text.literal("§a已开启自动续费"), false); }
            return 1;
        }));
        d.register(literal("mingdao_no").executes(ctx -> {
            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p!=null) { PlayerDataManager.setAutoRenewMingDao(p, false); p.sendMessage(Text.literal("§a已关闭自动续费"), false); }
            return 1;
        }));
    }
}