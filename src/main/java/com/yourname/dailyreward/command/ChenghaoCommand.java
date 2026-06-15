package com.yourname.dailyreward.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import java.util.List;
import static net.minecraft.server.command.CommandManager.*;

public class ChenghaoCommand {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("chenghao").executes(ctx -> {
            var p = ctx.getSource().getPlayer(); if(p==null) return 0;
            PlayerDataManager.removeExpiredTitles(p);
            List<String> titles = PlayerDataManager.getOwnedTitlesWithTime(p);
            if(titles.isEmpty()) { p.sendMessage(Text.literal("§c暂无称号"), false); return 1; }
            p.sendMessage(Text.literal("§6========= 称号仓库 ========="), false);
            for(int i=0;i<titles.size();i++) {
                final int idx = i+1;
                Text t = Text.literal("§e"+idx+". "+titles.get(i)).styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chenghao use "+idx)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击使用"))));
                p.sendMessage(t, false);
            }
            return 1;
        }).then(literal("use").then(argument("index", IntegerArgumentType.integer(1)).executes(ctx -> {
            var p = ctx.getSource().getPlayer(); if(p==null) return 0;
            PlayerDataManager.removeExpiredTitles(p);
            List<String> titles = PlayerDataManager.getOwnedTitlesWithTime(p);
            int idx = IntegerArgumentType.getInteger(ctx, "index")-1;
            if(idx<0 || idx>=titles.size()) { p.sendMessage(Text.literal("§c编号无效"), false); return 0; }
            String raw = titles.get(idx).split(" §7")[0];
            PlayerDataManager.setCurrentTitle(p, raw);
            p.sendMessage(Text.literal("§a已切换称号为 "+raw), false);
            return 1;
        }))));
    }
}