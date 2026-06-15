package com.yourname.dailyreward.anticheat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourname.dailyreward.DailyRewardMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiCheatManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static Config config;
    private static final Map<UUID, Violation> violations = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3d> lastPos = new HashMap<>();
    private static final Map<UUID, Long> lastPosTime = new HashMap<>();
    private static final Map<UUID, Long> lastAttack = new HashMap<>();

    static class Config {
        boolean enableSpeed = true;
        boolean enableFlight = true;
        boolean enableKillAura = true;
        boolean enableReach = true;
        double maxSpeed = 0.8;
        double maxVertical = 1.2;
        long minAttackInterval = 300;
        double maxReach = 5.0;
        int threshold = 5;
        String kickMsg = "§c检测到作弊，已被踢出！";
    }
    static class Violation { int v = 0; long lastDecay = System.currentTimeMillis(); }

    public static void init(MinecraftServer server) {
        configPath = server.getRunDirectory().toPath().resolve("config").resolve("anticheat.json");
        loadConfig();
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity p) {
                if (config.enableKillAura) checkKillAura(p);
                if (config.enableReach && hitResult != null) checkReach(p, hitResult.getPos());
            }
            return ActionResult.PASS;
        });
        server.getTickManager().schedule(() -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) tickPlayer(p);
        }, 1, 1);
        server.getTickManager().schedule(() -> {
            long now = System.currentTimeMillis();
            violations.values().forEach(v -> { if (now - v.lastDecay > 5000) { v.v = Math.max(0, v.v - 1); v.lastDecay = now; } });
        }, 100, 100);
    }
    private static void loadConfig() {
        if (Files.exists(configPath)) {
            try (Reader r = new FileReader(configPath.toFile())) { config = GSON.fromJson(r, Config.class); }
            catch (IOException e) {}
        }
        if (config == null) config = new Config();
        saveConfig();
    }
    private static void saveConfig() {
        try { Files.createDirectories(configPath.getParent()); try (Writer w = new FileWriter(configPath.toFile())) { GSON.toJson(config, w); } }
        catch (IOException e) {}
    }
    private static void tickPlayer(ServerPlayerEntity p) {
        Vec3d pos = p.getPos();
        long now = System.currentTimeMillis();
        Vec3d last = lastPos.get(p.getUuid());
        Long lastTime = lastPosTime.get(p.getUuid());
        if (last != null && lastTime != null && now - lastTime > 50) {
            double dt = (now - lastTime) / 1000.0;
            double dx = pos.x - last.x, dz = pos.z - last.z;
            double speed = Math.sqrt(dx*dx + dz*dz) / dt;
            double dy = Math.abs(pos.y - last.y) / dt;
            if (config.enableSpeed && speed > config.maxSpeed && !p.isCreative() && !p.isSpectator())
                addViolation(p, speed);
            if (config.enableFlight && dy > config.maxVertical && !p.isOnGround() && !p.isInLava() && !p.isTouchingWater() && !p.hasVehicle() && dy > 0)
                addViolation(p, dy);
        }
        lastPos.put(p.getUuid(), pos);
        lastPosTime.put(p.getUuid(), now);
    }
    private static void checkKillAura(ServerPlayerEntity p) {
        long now = System.currentTimeMillis();
        Long last = lastAttack.get(p.getUuid());
        if (last != null && now - last < config.minAttackInterval) addViolation(p, now - last);
        lastAttack.put(p.getUuid(), now);
    }
    private static void checkReach(ServerPlayerEntity p, Vec3d target) {
        double dist = p.getPos().distanceTo(target);
        if (dist > config.maxReach + 0.5) addViolation(p, dist);
    }
    private static void addViolation(ServerPlayerEntity p, double val) {
        Violation v = violations.computeIfAbsent(p.getUuid(), k -> new Violation());
        v.v++;
        if (v.v >= config.threshold) {
            p.networkHandler.disconnect(Text.literal(config.kickMsg));
            violations.remove(p.getUuid());
        }
    }
    public static void resetAll(ServerPlayerEntity p) { violations.remove(p.getUuid()); lastPos.remove(p.getUuid()); lastPosTime.remove(p.getUuid()); lastAttack.remove(p.getUuid()); }
}