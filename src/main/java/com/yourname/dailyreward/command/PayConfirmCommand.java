package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

public class PayConfirmCommand {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("pay_confirm").executes(ctx -> {
            ServerPlayerEntity from = ctx.getSource().getPlayer(); if(from==null) return 0;
            var req = PayCommand.pending.remove(from.getUuid());
            if(req == null || req.expired()) { from.sendMessage(Text.literal("§c请求已过期"), false); return 0; }
            ServerPlayerEntity to = from.getServer().getPlayerManager().getPlayer(req.target);
            if(to == null) { from.sendMessage(Text.literal("§c对方已离线"), false); return 0; }
            int fromPoints = PlayerDataManager.getPoints(from);
            if(fromPoints < req.amount) { from.sendMessage(Text.literal("§c积分不足"), false); return 0; }
            PlayerDataManager.setPoints(from, fromPoints - req.amount);
            PlayerDataManager.addPoints(to, req.amount);
            from.sendMessage(Text.literal("§a支付 "+req.amount+" 积分给 "+req.target), false);
            to.sendMessage(Text.literal("§a收到来自 "+from.getName().getString()+" 的 "+req.amount+" 积分"), false);
            return 1;
        }));
        d.register(literal("pay_cancel").executes(ctx -> {
            ServerPlayerEntity p = ctx.getSource().getPlayer(); if(p==null) return 0;
            if(PayCommand.pending.remove(p.getUuid()) != null) p.sendMessage(Text.literal("§e已取消支付"), false);
            return 1;
        }));
    }
}