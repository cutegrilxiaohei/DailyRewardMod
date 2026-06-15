package com.yourname.dailyreward.scoreboard;

import com.yourname.dailyreward.data.PlayerDataManager;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class ScoreboardService {
    private static final String ID = "dailyreward_lb";
    private static int mode = 0;
    private static int tick = 0;
    public static void init(MinecraftServer server) {
        server.getTickManager().schedule(() -> {
            tick++;
            if (tick >= 100) { tick = 0; mode = (mode + 1) % 3; }
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) update(p);
        }, 40, 40);
    }
    public static void updateForPlayer(ServerPlayerEntity p) { update(p); }
    private static void update(ServerPlayerEntity p) {
        Scoreboard sb = p.getScoreboard();
        ScoreboardObjective obj = sb.getObjective(ID);
        if (obj == null) obj = sb.addObjective(ID, ScoreboardCriterion.DUMMY, Text.literal("§6TRE纯净服务器排行榜"), ScoreboardCriterion.RenderType.INTEGER);
        sb.clearSlot(ScoreboardDisplaySlot.SIDEBAR);
        obj.setDisplayName(Text.literal("§6§lTRE纯净服务器排行榜"));
        sb.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, obj);
        if (mode == 0) showPoints(sb, obj, p);
        else if (mode == 1) showDeath(sb, obj, p);
        else showOnline(sb, obj, p);
        // 进度条
        int acc = PlayerDataManager.getAccumulatedTicks(p);
        int min = acc / (20*60);
        int filled = (min * 10) / 30;
        StringBuilder bar = new StringBuilder("§7[");
        for (int i=0;i<10;i++) bar.append(i<filled ? "§a|" : "§7-");
        bar.append("§7] §f").append(min).append("/30 min");
        addLine(sb, obj, bar.toString(), -20);
    }
    private static void showPoints(Scoreboard sb, ScoreboardObjective obj, ServerPlayerEntity p) {
        addLine(sb, obj, "§7§m----------", 0);
        addLine(sb, obj, "§e● 积分榜 §f(前3名)", -1);
        addLine(sb, obj, "§7§m----------", -2);
        List<Map.Entry<UUID, Integer>> list = PlayerDataManager.getPointsLeaderboard();
        int rank=1;
        for (Map.Entry<UUID,Integer> e : list) {
            if (rank>3) break;
            String name = getDisplayName(e.getKey(), p.getServer());
            addLine(sb, obj, String.format("§7#%d §f%s §a%d", rank, name.length()>10?name.substring(0,10):name, e.getValue()), -2-rank);
            rank++;
        }
        for (int i=rank;i<=3;i++) addLine(sb, obj, "§7空", -2-i);
        addLine(sb, obj, "", -6);
        int total = PlayerDataManager.getTotalPoints(p);
        int norm = PlayerDataManager.getPoints(p);
        int bind = PlayerDataManager.getBindingPoints(p);
        addLine(sb, obj, "§6总积分: §e"+total, -7);
        addLine(sb, obj, "§7(普§e"+norm+" §7绑§e"+bind+"§7)", -8);
    }
    private static void showDeath(Scoreboard sb, ScoreboardObjective obj, ServerPlayerEntity p) {
        addLine(sb, obj, "§7§m----------", 0);
        addLine(sb, obj, "§c● 死亡榜 §f(前3名)", -1);
        addLine(sb, obj, "§7§m----------", -2);
        List<Map.Entry<UUID,Integer>> list = PlayerDataManager.getDeathLeaderboard();
        int rank=1;
        for (Map.Entry<UUID,Integer> e : list) {
            if (rank>3) break;
            String name = getDisplayName(e.getKey(), p.getServer());
            addLine(sb, obj, String.format("§7#%d §f%s §c%d", rank, name.length()>10?name.substring(0,10):name, e.getValue()), -2-rank);
            rank++;
        }
        for (int i=rank;i<=3;i++) addLine(sb, obj, "§7空", -2-i);
        addLine(sb, obj, "", -6);
        addLine(sb, obj, "§c你的死亡次数: §e"+PlayerDataManager.getDeathCount(p), -7);
    }
    private static void showOnline(Scoreboard sb, ScoreboardObjective obj, ServerPlayerEntity p) {
        addLine(sb, obj, "§7§m----------", 0);
        addLine(sb, obj, "§b● 在线时长榜 §f(前3名)", -1);
        addLine(sb, obj, "§7§m----------", -2);
        List<Map.Entry<UUID,Long>> list = PlayerDataManager.getOnlineTimeLeaderboard();
        int rank=1;
        for (Map.Entry<UUID,Long> e : list) {
            if (rank>3) break;
            String name = getDisplayName(e.getKey(), p.getServer());
            double hours = e.getValue() / (20.0*3600);
            addLine(sb, obj, String.format("§7#%d §f%s §b%.1fh", rank, name.length()>10?name.substring(0,10):name, hours), -2-rank);
            rank++;
        }
        for (int i=rank;i<=3;i++) addLine(sb, obj, "§7空", -2-i);
        addLine(sb, obj, "", -6);
        double self = PlayerDataManager.getTotalOnlineTicks(p) / (20.0*3600);
        addLine(sb, obj, "§b你的在线时长: §e"+String.format("%.1f", self)+"h", -7);
    }
    private static String getDisplayName(UUID uuid, MinecraftServer srv) {
        ServerPlayerEntity p = srv.getPlayerManager().getPlayer(uuid);
        if (p != null) return p.getDisplayName().getString();
        String title = PlayerDataManager.getCurrentTitleRaw(null);
        String name = PlayerDataManager.getPlayerName(uuid, srv);
        return title.isEmpty() ? name : title + name;
    }
    private static void addLine(Scoreboard sb, ScoreboardObjective obj, String text, int score) {
        sb.getOrCreateScore(ScoreHolder.fromName(text), obj).setScore(score);
    }
    public static void removePlayer(ServerPlayerEntity p) {}
}