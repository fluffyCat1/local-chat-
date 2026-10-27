package com.example.localchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuteManager {
    public static class Entry {
        public String name;
        public long until;   // 0 = навсегда, иначе время окончания в мс
        public String reason;

        public Entry() {}

        public Entry(String name, long until, String reason) {
            this.name = name;
            this.until = until;
            this.reason = reason;
        }

        public boolean expired() {
            return until != 0 && System.currentTimeMillis() >= until;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Entry> MUTES = new HashMap<>();
    private static Path file;

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("localchat_mutes.json");
        MUTES.clear();
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            Type t = new TypeToken<Map<String, Entry>>() {}.getType();
            Map<String, Entry> data = GSON.fromJson(r, t);
            if (data != null) {
                data.forEach((k, v) -> MUTES.put(UUID.fromString(k), v));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        if (file == null) return;
        try (Writer w = Files.newBufferedWriter(file)) {
            Map<String, Entry> data = new HashMap<>();
            MUTES.forEach((k, v) -> data.put(k.toString(), v));
            GSON.toJson(data, w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mute(UUID id, String name, long until, String reason) {
        MUTES.put(id, new Entry(name, until, reason));
        save();
    }

    public static boolean unmute(UUID id) {
        boolean removed = MUTES.remove(id) != null;
        if (removed) save();
        return removed;
    }

    /** Возвращает активный мут или null (просроченный мут удаляется). */
    public static Entry get(UUID id) {
        Entry e = MUTES.get(id);
        if (e == null) return null;
        if (e.expired()) {
            MUTES.remove(id);
            save();
            return null;
        }
        return e;
    }

    public static Map<UUID, Entry> all() {
        MUTES.entrySet().removeIf(en -> en.getValue().expired());
        return MUTES;
    }

    public static String timeLeft(long until) {
        if (until == 0) return "навсегда";
        long ms = until - System.currentTimeMillis();
        long min = Math.max(1, (ms + 59999) / 60000);
        if (min >= 1440) return (min / 1440) + " д " + ((min % 1440) / 60) + " ч";
        if (min >= 60) return (min / 60) + " ч " + (min % 60) + " мин";
        return min + " мин";
    }
}
