package com.yourname.dailyreward.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourname.dailyreward.DailyRewardMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class WhitelistManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path;
    private static Set<String> whitelist = new HashSet<>();
    private static MinecraftServer server;

    public static void init(MinecraftServer srv) {
        server = srv;
        path = srv.getRunDirectory().toPath().resolve("config").resolve("whitelist.json");
        load();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> check(handler.getPlayer()));
        srv.getTickManager().schedule(() -> {
            for (ServerPlayerEntity p : srv.getPlayerManager().getPlayerList()) check(p);
        }, 20 * 30, 20 * 30);
    }
    private static void load() {
        if (Files.exists(path)) {
            try (Reader r = new FileReader(path.toFile())) {
                String[] arr = GSON.fromJson(r, String[].class);
                if (arr != null) for (String n : arr) whitelist.add(n.toLowerCase());
            } catch (IOException e) { DailyRewardMod.LOGGER.error("加载白名单失败", e); }
        }
    }
    private static void save() {
        try (Writer w = new FileWriter(path.toFile())) { GSON.toJson(whitelist, w); }
        catch (IOException e) { DailyRewardMod.LOGGER.error("保存白名单失败", e); }
    }
    public static boolean isWhitelisted(String name) { return whitelist.contains(name.toLowerCase()); }
    public static void addPlayer(String name) { whitelist.add(name.toLowerCase()); save(); checkAll(); }
    public static void removePlayer(String name) { whitelist.remove(name.toLowerCase()); save(); checkAll(); }
    public static Set<String> getWhitelist() { return new HashSet<>(whitelist); }
    private static void check(ServerPlayerEntity p) {
        if (!isWhitelisted(p.getName().getString())) p.networkHandler.disconnect(Text.literal("§c你不在白名单中！"));
    }
    private static void checkAll() { if (server != null) for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) check(p); }
}