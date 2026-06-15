package com.yourname.dailyreward.data;

import com.google.gson.*;
import com.yourname.dailyreward.DailyRewardMod;
import com.yourname.dailyreward.scoreboard.ScoreboardService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path dataPath;
    private static final ConcurrentHashMap<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private static MinecraftServer server;

    public static class PlayerData {
        public int points = 0;
        public int bindingPoints = 0;
        public LocalDate lastSignDate = null;
        public int consecutiveDays = 0;
        public int accumulatedTicks = 0;
        public long totalOnlineTicks = 0;
        public int todayOnlineTicks = 0;
        public LocalDate lastResetDate = null;
        public String lastKnownName = "";
        public int deathCount = 0;
        public String lastDeathDimension = "";
        public int lastDeathX = 0, lastDeathY = 0, lastDeathZ = 0;
        public boolean hasMingDao = false;
        public boolean autoRenewMingDao = false;
        public Map<String, Long> titles = new HashMap<>();
        public String currentTitle = "";
        public Set<String> rewardedAdvancements = new HashSet<>();
    }

    public static class WelfareItem {
        public String type = "";
        public String message = "";
        public int points = 0;
        public String title = "";
        public long expireDay = 0;
        public Set<String> givenPlayers = new HashSet<>();
    }
    private static List<WelfareItem> welfareList = new ArrayList<>();
    private static Path welfarePath;
    private static Path botCounterPath;
    private static int botCounter = 0;

    public static void init() {}
    public static void setServer(MinecraftServer srv) {
        server = srv;
        dataPath = srv.getRunDirectory().toPath().resolve("dailyreward_data");
        try { Files.createDirectories(dataPath); } catch (IOException e) { DailyRewardMod.LOGGER.error("数据目录创建失败", e); }
        botCounterPath = dataPath.resolve("bot_counter.json");
        loadBotCounter();
        welfarePath = dataPath.resolve("welfare_list.json");
        loadWelfareList();
    }
    private static Path getPlayerFile(UUID uuid) { return dataPath.resolve(uuid.toString() + ".json"); }
    public static PlayerData getPlayerData(UUID uuid) { return cache.computeIfAbsent(uuid, k -> new PlayerData()); }

    public static void loadPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Path file = getPlayerFile(uuid);
        if (Files.exists(file)) {
            try (Reader r = new FileReader(file.toFile())) {
                PlayerData data = GSON.fromJson(r, PlayerData.class);
                if (data != null) {
                    data.lastKnownName = player.getName().getString();
                    cache.put(uuid, data);
                    checkAndResetToday(player, data);
                    updateDisplayName(player);
                }
            } catch (IOException e) { DailyRewardMod.LOGGER.error("加载玩家数据失败", e); }
        } else {
            PlayerData newData = new PlayerData();
            newData.lastKnownName = player.getName().getString();
            newData.lastResetDate = LocalDate.now();
            cache.put(uuid, newData);
            updateDisplayName(player);
        }
    }
    public static void savePlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        data.lastKnownName = player.getName().getString();
        try (Writer w = new FileWriter(getPlayerFile(uuid).toFile())) { GSON.toJson(data, w); }
        catch (IOException e) { DailyRewardMod.LOGGER.error("保存玩家数据失败", e); }
    }
    public static void saveAll() {
        for (UUID uuid : cache.keySet()) {
            try (Writer w = new FileWriter(getPlayerFile(uuid).toFile())) { GSON.toJson(cache.get(uuid), w); }
            catch (IOException e) { DailyRewardMod.LOGGER.error("保存数据失败", e); }
        }
    }

    // === 积分操作 ===
    public static int getPoints(ServerPlayerEntity p) { return getPlayerData(p.getUuid()).points; }
    public static void setPoints(ServerPlayerEntity p, int points) { getPlayerData(p.getUuid()).points = Math.max(0, points); savePlayer(p); ScoreboardService.updateForPlayer(p); }
    public static void addPoints(ServerPlayerEntity p, int amt) { getPlayerData(p.getUuid()).points += amt; savePlayer(p); ScoreboardService.updateForPlayer(p); }
    public static int getBindingPoints(ServerPlayerEntity p) { return getPlayerData(p.getUuid()).bindingPoints; }
    public static void addBindingPoints(ServerPlayerEntity p, int amt) { getPlayerData(p.getUuid()).bindingPoints += amt; savePlayer(p); ScoreboardService.updateForPlayer(p); }
    public static int getTotalPoints(ServerPlayerEntity p) { PlayerData d = getPlayerData(p.getUuid()); return d.points + d.bindingPoints; }
    public static boolean deductPoints(ServerPlayerEntity p, int amt) {
        PlayerData d = getPlayerData(p.getUuid());
        int total = d.points + d.bindingPoints;
        if (total < amt) return false;
        if (d.bindingPoints >= amt) d.bindingPoints -= amt;
        else { int need = amt - d.bindingPoints; d.bindingPoints = 0; d.points -= need; }
        savePlayer(p);
        ScoreboardService.updateForPlayer(p);
        return true;
    }

    // === 签到 ===
    public static boolean hasSignedToday(ServerPlayerEntity p) {
        LocalDate today = LocalDate.now();
        PlayerData d = getPlayerData(p.getUuid());
        return d.lastSignDate != null && d.lastSignDate.equals(today);
    }
    public static int sign(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        if (d.lastSignDate != null && d.lastSignDate.equals(yesterday)) d.consecutiveDays++;
        else if (d.lastSignDate != null && d.lastSignDate.equals(today)) return 0;
        else d.consecutiveDays = 1;
        d.lastSignDate = today;
        int reward;
        if (d.consecutiveDays >= 15) reward = 100;
        else if (d.consecutiveDays >= 7) reward = 70;
        else if (d.consecutiveDays >= 3) reward = 50;
        else reward = 30;
        if (d.consecutiveDays >= 30) {
            String title30 = "§c<持之以恒>";
            boolean has = false;
            long now = System.currentTimeMillis();
            if (d.titles.containsKey(title30)) {
                long exp = d.titles.get(title30);
                if (exp == -1 || exp > now) has = true;
            }
            if (!has) {
                d.titles.put(title30, now + 30L*24*60*60*1000);
                p.sendMessage(Text.literal("§6[系统]：获得限时30天称号 " + title30));
                if (d.currentTitle.isEmpty()) d.currentTitle = title30;
            }
        }
        d.points += reward;
        savePlayer(p);
        ScoreboardService.updateForPlayer(p);
        updateDisplayName(p);
        return reward;
    }

    // === 在线时长 ===
    public static int getAccumulatedTicks(ServerPlayerEntity p) { return getPlayerData(p.getUuid()).accumulatedTicks; }
    public static void addAccumulatedTicks(ServerPlayerEntity p, int ticks) { getPlayerData(p.getUuid()).accumulatedTicks += ticks; savePlayer(p); }
    public static void modifyAccumulatedTicks(ServerPlayerEntity p, int delta) { getPlayerData(p.getUuid()).accumulatedTicks += delta; savePlayer(p); }
    public static long getTotalOnlineTicks(ServerPlayerEntity p) { return getPlayerData(p.getUuid()).totalOnlineTicks; }
    public static void addTotalOnlineTick(ServerPlayerEntity p) { getPlayerData(p.getUuid()).totalOnlineTicks++; savePlayer(p); }
    public static int getTodayOnlineTicks(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        checkAndResetToday(p, d);
        return d.todayOnlineTicks;
    }
    public static void addTodayOnlineTicks(ServerPlayerEntity p, int ticks) {
        PlayerData d = getPlayerData(p.getUuid());
        checkAndResetToday(p, d);
        d.todayOnlineTicks += ticks;
        savePlayer(p);
    }
    private static void checkAndResetToday(ServerPlayerEntity p, PlayerData d) {
        LocalDate today = LocalDate.now();
        if (d.lastResetDate == null || !d.lastResetDate.equals(today)) {
            d.todayOnlineTicks = 0;
            d.accumulatedTicks = 0;
            d.lastResetDate = today;
            savePlayer(p);
        }
    }

    // === 死亡 ===
    public static int getDeathCount(ServerPlayerEntity p) { return getPlayerData(p.getUuid()).deathCount; }
    public static void addDeath(ServerPlayerEntity p) { getPlayerData(p.getUuid()).deathCount++; savePlayer(p); ScoreboardService.updateForPlayer(p); }
    public static void setLastDeathLocation(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        d.lastDeathDimension = p.getServerWorld().getRegistryKey().getValue().toString();
        d.lastDeathX = p.getBlockX(); d.lastDeathY = p.getBlockY(); d.lastDeathZ = p.getBlockZ();
        savePlayer(p);
    }
    public static boolean hasLastDeathLocation(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        return d.lastDeathDimension != null && !d.lastDeathDimension.isEmpty();
    }
    public static void teleportToLastDeath(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        String cmd = String.format("execute in %s run tp %s %d %d %d", d.lastDeathDimension, p.getName().getString(), d.lastDeathX, d.lastDeathY, d.lastDeathZ);
        p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource().withPermissionLevel(4), cmd);
    }

    // === 名刀 ===
    public static boolean hasMingDao(ServerPlayerEntity p) { return getPlayerData(p.getUuid()).hasMingDao; }
    public static void setMingDao(ServerPlayerEntity p, boolean has) { getPlayerData(p.getUuid()).hasMingDao = has; savePlayer(p); }
    public static boolean isAutoRenewMingDao(ServerPlayerEntity p) { return getPlayerData(p.getUuid()).autoRenewMingDao; }
    public static void setAutoRenewMingDao(ServerPlayerEntity p, boolean auto) { getPlayerData(p.getUuid()).autoRenewMingDao = auto; savePlayer(p); }
    public static boolean consumeMingDao(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        if (d.hasMingDao) { d.hasMingDao = false; savePlayer(p); return true; }
        return false;
    }
    public static boolean tryAutoRenewMingDao(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        if (d.autoRenewMingDao && deductPoints(p, 10)) {
            d.hasMingDao = true;
            savePlayer(p);
            p.sendMessage(Text.literal("§a[系统]：名刀·司命已自动续费，消耗10积分"));
            return true;
        } else if (d.autoRenewMingDao) {
            d.autoRenewMingDao = false;
            savePlayer(p);
            p.sendMessage(Text.literal("§c[系统]：积分不足，名刀自动续费已终止"));
        }
        return false;
    }

    // === 称号 ===
    public static void addTitle(ServerPlayerEntity p, String title, long expire) { getPlayerData(p.getUuid()).titles.put(title, expire); savePlayer(p); updateDisplayName(p); }
    public static List<String> removeExpiredTitles(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        Iterator<Map.Entry<String, Long>> it = d.titles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() != -1 && e.getValue() <= now) {
                expired.add(e.getKey());
                it.remove();
                if (d.currentTitle.equals(e.getKey())) d.currentTitle = "";
            }
        }
        if (!expired.isEmpty()) { savePlayer(p); updateDisplayName(p); }
        return expired;
    }
    public static List<String> getOwnedTitlesWithTime(ServerPlayerEntity p) {
        PlayerData d = getPlayerData(p.getUuid());
        long now = System.currentTimeMillis();
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : d.titles.entrySet()) {
            if (e.getValue() == -1) list.add(e.getKey() + " §7(永久)");
            else list.add(e.getKey() + " §7(限时 " + ((e.getValue() - now) / (24*60*60*1000)) + " 天)");
        }
        return list;
    }
    public static String getCurrentTitleRaw(ServerPlayerEntity p) { return getPlayerData(p.getUuid()).currentTitle; }
    public static void setCurrentTitle(ServerPlayerEntity p, String title) {
        PlayerData d = getPlayerData(p.getUuid());
        if (d.titles.containsKey(title)) { d.currentTitle = title; savePlayer(p); updateDisplayName(p); }
    }
    public static void updateDisplayName(ServerPlayerEntity p) {
        String title = getPlayerData(p.getUuid()).currentTitle;
        p.setDisplayName(Text.literal(title + p.getName().getString()));
        p.setCustomName(Text.literal(title + p.getName().getString()));
    }

    // === 假人计数器 ===
    private static void loadBotCounter() {
        if (Files.exists(botCounterPath)) {
            try (Reader r = new FileReader(botCounterPath.toFile())) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                if (obj != null && obj.has("counter")) botCounter = obj.get("counter").getAsInt();
            } catch (IOException e) {}
        }
    }
    private static void saveBotCounter() {
        JsonObject obj = new JsonObject();
        obj.addProperty("counter", botCounter);
        try (Writer w = new FileWriter(botCounterPath.toFile())) { GSON.toJson(obj, w); } catch (IOException e) {}
    }
    public static synchronized int getNextBotNumber() { botCounter++; saveBotCounter(); return botCounter; }

    // === 成就 ===
    public static boolean isAdvancementRewarded(ServerPlayerEntity p, String id, String crit) { return getPlayerData(p.getUuid()).rewardedAdvancements.contains(id + "/" + crit); }
    public static void markAdvancementRewarded(ServerPlayerEntity p, String id, String crit) { getPlayerData(p.getUuid()).rewardedAdvancements.add(id + "/" + crit); savePlayer(p); }
    public static void rewardAdvancement(ServerPlayerEntity p, String id, String crit, int points, boolean broadcast) {
        if (isAdvancementRewarded(p, id, crit)) return;
        addPoints(p, points);
        markAdvancementRewarded(p, id, crit);
        if (broadcast) p.getServer().getPlayerManager().broadcast(Text.literal("§6[系统] 玩家 " + p.getName().getString() + " 完成了挑战成就！获得 " + points + " 积分"), false);
        else p.sendMessage(Text.literal("§a[成就] 完成进度，获得 " + points + " 积分"));
    }

    // === 福利 ===
    private static void loadWelfareList() {
        if (Files.exists(welfarePath)) {
            try (Reader r = new FileReader(welfarePath.toFile())) {
                WelfareItem[] arr = GSON.fromJson(r, WelfareItem[].class);
                if (arr != null) welfareList = new ArrayList<>(Arrays.asList(arr));
            } catch (IOException e) {}
        }
        if (welfareList == null) welfareList = new ArrayList<>();
        cleanExpiredWelfare();
    }
    private static void saveWelfareList() { try (Writer w = new FileWriter(welfarePath.toFile())) { GSON.toJson(welfareList, w); } catch (IOException e) {} }
    private static long getTodayStart() { return LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000; }
    public static void cleanExpiredWelfare() { long today = getTodayStart(); if (welfareList.removeIf(w -> w.expireDay != today)) saveWelfareList(); }
    public static void addWelfare(String type, String msg, int points, String title) {
        WelfareItem w = new WelfareItem();
        w.type = type; w.message = msg; w.points = points; w.title = title; w.expireDay = getTodayStart();
        welfareList.add(w);
        saveWelfareList();
    }
    public static void grantAllWelfare(ServerPlayerEntity p) {
        cleanExpiredWelfare();
        String uuid = p.getUuid().toString();
        boolean changed = false;
        for (WelfareItem w : welfareList) {
            if (w.givenPlayers.contains(uuid)) continue;
            if (w.type.equals("jifen")) {
                addBindingPoints(p, w.points);
                p.sendMessage(Text.literal("§6[系统]：" + w.message + "（绑定积分 +" + w.points + "）"));
                changed = true;
            } else if (w.type.equals("chenghao")) {
                addTitle(p, w.title, -1);
                p.sendMessage(Text.literal("§6[系统]：" + w.message + " 获得称号 " + w.title));
                changed = true;
            }
            w.givenPlayers.add(uuid);
        }
        if (changed) saveWelfareList();
    }
    public static void grantAllWelfareToOnline(MinecraftServer srv) { for (ServerPlayerEntity p : srv.getPlayerManager().getPlayerList()) grantAllWelfare(p); }

    // === 排行榜数据 ===
    public static List<Map.Entry<UUID, Integer>> getPointsLeaderboard() {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> e : cache.entrySet()) list.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().points));
        list.sort((a,b) -> b.getValue().compareTo(a.getValue()));
        return list;
    }
    public static List<Map.Entry<UUID, Integer>> getDeathLeaderboard() {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> e : cache.entrySet()) list.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().deathCount));
        list.sort((a,b) -> b.getValue().compareTo(a.getValue()));
        return list;
    }
    public static List<Map.Entry<UUID, Long>> getOnlineTimeLeaderboard() {
        List<Map.Entry<UUID, Long>> list = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> e : cache.entrySet()) list.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().totalOnlineTicks));
        list.sort((a,b) -> b.getValue().compareTo(a.getValue()));
        return list;
    }
    public static List<PlayerDataEntry> getAllPlayerData() {
        List<PlayerDataEntry> list = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> e : cache.entrySet()) {
            PlayerData d = e.getValue();
            String name = d.lastKnownName.isEmpty() ? e.getKey().toString().substring(0,8) : d.lastKnownName;
            list.add(new PlayerDataEntry(name, d.points, d.lastSignDate));
        }
        list.sort((a,b) -> a.name.compareTo(b.name));
        return list;
    }
    public static class PlayerDataEntry { public final String name; public final int points; public final LocalDate lastSignDate; public PlayerDataEntry(String n, int p, LocalDate d) { name=n; points=p; lastSignDate=d; } }
    public static String getPlayerName(UUID uuid, MinecraftServer srv) {
        ServerPlayerEntity p = srv.getPlayerManager().getPlayer(uuid);
        if (p != null) return p.getName().getString();
        PlayerData d = cache.get(uuid);
        return (d != null && !d.lastKnownName.isEmpty()) ? d.lastKnownName : uuid.toString().substring(0,8);
    }
}