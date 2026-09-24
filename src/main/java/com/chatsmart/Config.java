package com.chatsmart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger("chatsmart");
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();
    private static final Path   DIR    = FabricLoader.getInstance()
                                            .getConfigDir().resolve("chatsmart");

    public static void load() {
        DIR.toFile().mkdirs();

        List<ReplyRule> rules = loadFile("rules.json",
                new TypeToken<List<ReplyRule>>(){});
        List<ChatBind> binds = loadFile("binds.json",
                new TypeToken<List<ChatBind>>(){});

        ChatSmartClient.rules.clear();
        ChatSmartClient.binds.clear();
        if (rules != null) ChatSmartClient.rules.addAll(rules);
        if (binds != null) ChatSmartClient.binds.addAll(binds);
    }

    private static <T> List<T> loadFile(String name, TypeToken<List<T>> token) {
        File f = DIR.resolve(name).toFile();
        if (!f.exists()) return null;
        try (Reader r = new FileReader(f)) {
            return GSON.fromJson(r, token.getType());
        } catch (Exception e) {
            LOGGER.error("[ChatSmart] Could not load {}: {}", name, e.getMessage());
            return null;
        }
    }

    public static void save() {
        DIR.toFile().mkdirs();
        saveFile("rules.json", ChatSmartClient.rules);
        saveFile("binds.json", ChatSmartClient.binds);
    }

    private static void saveFile(String name, Object data) {
        try (Writer w = new FileWriter(DIR.resolve(name).toFile())) {
            GSON.toJson(data, w);
        } catch (Exception e) {
            LOGGER.error("[ChatSmart] Could not save {}: {}", name, e.getMessage());
        }
    }
}
