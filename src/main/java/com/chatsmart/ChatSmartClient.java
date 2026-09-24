package com.chatsmart;

import com.chatsmart.gui.MainScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatSmartClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("chatsmart");

    public static final List<Component>  recentChat = new ArrayList<>();
    public static final List<ReplyRule>  rules      = new ArrayList<>();
    public static final List<ChatBind>   binds      = new ArrayList<>();

    private static KeyMapping openGuiKey;

    private static final Random RNG = new Random();
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "chatsmart-scheduler");
                t.setDaemon(true);
                return t;
            });

    private static final Map<ReplyRule, AtomicBoolean> pending =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private static final Map<ReplyRule, long[]> waitingForRepeat =
            Collections.synchronizedMap(new IdentityHashMap<>());

    @Override
    public void onInitializeClient() {
        Config.load();
        LOGGER.info("[ChatSmart] Loaded — {} rule(s), {} bind(s)", rules.size(), binds.size());

        // 26.1.2 (Mojang mappings): the keybind category is a KeyMapping.Category object,
        // created from a ResourceLocation (formerly Yarn's Identifier).
        KeyMapping.Category category = KeyMapping.Category.create(ResourceLocation.fromNamespaceAndPath("chatsmart", "main"));
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatsmart.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, receptionTimestamp) -> {
                    storeHistory(message);
                    String raw = message.getString();
                    checkRepeatWaiters(raw);
                    checkAutoReply(raw);
                });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                storeHistory(message);
                String raw = message.getString();
                checkRepeatWaiters(raw);
                checkAutoReply(raw);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.player != null) client.setScreen(new MainScreen());
            }

            if (client.screen == null && client.player != null) {
                // 26.1.2: InputConstants.isKeyDown takes the GLFW window handle.
                long windowHandle = client.getWindow().getWindow();
                for (ChatBind bind : binds) {
                    if (!bind.enabled) { bind.held = false; continue; }
                    boolean down = InputConstants.isKeyDown(windowHandle, bind.keyCode);
                    if (down && !bind.held) {
                        bind.held = true;
                        send(client, bind.message);
                    } else if (!down) {
                        bind.held = false;
                    }
                }
            }

            expireWaiters();
        });
    }

    private static void storeHistory(Component message) {
        if (recentChat.size() >= 100) recentChat.remove(0);
        recentChat.add(message);
    }

    private static void checkAutoReply(String raw) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (raw.contains("<" + client.player.getName().getString() + ">")) return;

        String lower = raw.toLowerCase();
        for (ReplyRule rule : rules) {
            if (!rule.enabled) continue;
            if (!lower.contains(rule.trigger.toLowerCase())) continue;

            if (pending.containsKey(rule)) {
                LOGGER.info("[ChatSmart] Rule \"{}\" already pending — ignoring", rule.trigger);
                break;
            }

            if (rule.chanceEnabled) {
                int roll = RNG.nextInt(100) + 1;
                if (roll > rule.chance) {
                    LOGGER.info("[ChatSmart] Rule \"{}\" failed chance ({}/{})", rule.trigger, roll, rule.chance);
                    break;
                }
            }

            AtomicBoolean token = new AtomicBoolean(true);
            pending.put(rule, token);

            if (rule.waitRepeatEnabled) {
                int timeoutSecs = rule.waitTimeoutSecs > 0 ? rule.waitTimeoutSecs : 30;
                long expiry = System.currentTimeMillis() + timeoutSecs * 1000L;
                waitingForRepeat.put(rule, new long[]{ expiry });
                LOGGER.info("[ChatSmart] Rule \"{}\" waiting for repeat (timeout {}s)", rule.trigger, timeoutSecs);
                break;
            }

            scheduleReply(rule, token);
            break;
        }
    }

    private static void checkRepeatWaiters(String raw) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (raw.contains("<" + client.player.getName().getString() + ">")) return;

        String lower = raw.toLowerCase();
        List<ReplyRule> toFire = new ArrayList<>();

        synchronized (waitingForRepeat) {
            for (Map.Entry<ReplyRule, long[]> entry : waitingForRepeat.entrySet()) {
                if (lower.contains(entry.getKey().reply.toLowerCase())) toFire.add(entry.getKey());
            }
            for (ReplyRule rule : toFire) waitingForRepeat.remove(rule);
        }

        for (ReplyRule rule : toFire) {
            AtomicBoolean token = pending.get(rule);
            if (token != null && token.get()) scheduleReply(rule, token);
        }
    }

    private static void expireWaiters() {
        long now = System.currentTimeMillis();
        List<ReplyRule> expired = new ArrayList<>();
        synchronized (waitingForRepeat) {
            for (Map.Entry<ReplyRule, long[]> entry : waitingForRepeat.entrySet()) {
                if (now >= entry.getValue()[0]) expired.add(entry.getKey());
            }
            for (ReplyRule rule : expired) waitingForRepeat.remove(rule);
        }
        for (ReplyRule rule : expired) {
            AtomicBoolean token = pending.remove(rule);
            if (token != null) token.set(false);
            LOGGER.info("[ChatSmart] Rule \"{}\" wait timed out", rule.trigger);
        }
    }

    private static void scheduleReply(ReplyRule rule, AtomicBoolean token) {
        long delayMs = 0;
        if (rule.delayEnabled) {
            if (rule.rangeEnabled) {
                float min = Math.min(rule.delayMin, rule.delayMax);
                float max = Math.max(rule.delayMin, rule.delayMax);
                delayMs = (long)((min + RNG.nextFloat() * (max - min)) * 1000);
            } else {
                delayMs = (long)(rule.delayFixed * 1000);
            }
        }
        final String reply = rule.reply;
        SCHEDULER.schedule(() -> {
            if (!token.get()) return;
            pending.remove(rule);
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> send(client, reply));
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void send(Minecraft client, String message) {
        if (client.player == null || message.isBlank()) return;
        String m = message.trim();
        // 26.1.2 (Mojang mappings): ClientPacketListener exposes sendCommand / sendChat.
        if (m.startsWith("/")) client.player.connection.sendCommand(m.substring(1));
        else                   client.player.connection.sendChat(m);
    }
}
