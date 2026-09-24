package com.chatsmart.gui;

import com.chatsmart.ChatSmartClient;
import com.chatsmart.Config;
import com.chatsmart.ReplyRule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Add or edit a reply rule.
 *
 * Execution order:
 *   1. Chance   — roll % to decide whether to fire at all
 *   2. Wait     — wait until another player says the reply text first
 *   3. Delay    — fixed seconds, or random range
 *   4. Send
 *
 * Each section has an ON/OFF toggle. Fields only show when ON.
 * Range toggle lives inside Delay — when ON it hides the fixed field.
 */
public class AddEditRuleScreen extends Screen {

    private final Screen parent;
    private final int    ruleIdx;   // -1 = new rule

    // ── Live state (survives rebuilds) ────────────────────────────
    private String  liveTrigger;
    private String  liveReply;
    private boolean chanceEnabled;
    private int     chance;
    private boolean waitEnabled;
    private int     waitTimeout;
    private boolean delayEnabled;
    private float   delayFixed;
    private boolean rangeEnabled;
    private float   delayMin;
    private float   delayMax;

    // ── Widgets ───────────────────────────────────────────────────
    private EditBox triggerField;
    private EditBox replyField;
    private EditBox chanceField;
    private EditBox waitTimeoutField;
    private EditBox fixedField;
    private EditBox minField;
    private EditBox maxField;

    // ── Layout ────────────────────────────────────────────────────
    private static final int LEFT   = -190;
    private static final int TOG_W  = 38;
    private static final int FLD_W  = 52;
    private static final int ROW    = 21;

    // ── Constructors ──────────────────────────────────────────────

    /** From ChatPickerScreen (new rule, prefilled trigger) */
    public AddEditRuleScreen(Screen parent, int ruleIdx, String prefillTrigger) {
        super(Component.literal(ruleIdx == -1 ? "New Rule" : "Edit Rule"));
        this.parent = parent;
        this.ruleIdx = ruleIdx;
        if (ruleIdx >= 0) {
            ReplyRule r = ChatSmartClient.rules.get(ruleIdx);
            liveTrigger = prefillTrigger.isEmpty() ? r.trigger : prefillTrigger;
            liveReply   = r.reply;
            loadFrom(r);
        } else {
            liveTrigger = prefillTrigger;
            liveReply   = "";
            setDefaults();
        }
    }

    /** From RulesScreen Edit button */
    public AddEditRuleScreen(Screen parent, int ruleIdx) {
        super(Component.literal("Edit Rule"));
        this.parent = parent;
        this.ruleIdx = ruleIdx;
        ReplyRule r = ChatSmartClient.rules.get(ruleIdx);
        liveTrigger = r.trigger;
        liveReply   = r.reply;
        loadFrom(r);
    }

    private void loadFrom(ReplyRule r) {
        chanceEnabled = r.chanceEnabled; chance     = r.chance;
        waitEnabled   = r.waitRepeatEnabled;
        waitTimeout   = r.waitTimeoutSecs;
        delayEnabled  = r.delayEnabled;  delayFixed = r.delayFixed;
        rangeEnabled  = r.rangeEnabled;  delayMin   = r.delayMin; delayMax = r.delayMax;
    }

    private void setDefaults() {
        chanceEnabled = false; chance     = 100;
        waitEnabled   = false; waitTimeout   = 30;
        delayEnabled  = false; delayFixed = 1.0f;
        rangeEnabled  = false; delayMin   = 1.0f; delayMax = 2.0f;
    }

    // ── Init / rebuild ────────────────────────────────────────────

    @Override
    protected void init() {
        clearWidgets();
        int cx = width / 2;
        int y  = topY();

        // ── Trigger ───────────────────────────────────────────────
        triggerField = field(cx + LEFT, y, 380, liveTrigger, "Phrase to watch for in chat…");
        y += 22;

        // ── Reply ─────────────────────────────────────────────────
        replyField = field(cx + LEFT, y, 380, liveReply, "What to send (/ for commands)…");
        y += 26;

        // ── Divider gap ───────────────────────────────────────────
        y += 4;

        // ── Row 1: Chance ─────────────────────────────────────────
        tog(cx + LEFT, y, chanceEnabled, v -> { chanceEnabled = v; snapshot(); rebuild(); });
        if (chanceEnabled) {
            chanceField = field(cx + LEFT + TOG_W + 80, y, FLD_W, String.valueOf(chance), "100");
        }
        y += ROW;

        // ── Row 2: Wait for repeat ────────────────────────────────
        tog(cx + LEFT, y, waitEnabled, v -> { waitEnabled = v; snapshot(); rebuild(); });
        if (waitEnabled) {
            waitTimeoutField = field(cx + LEFT + TOG_W + 80, y, FLD_W, String.valueOf(waitTimeout), "30");
        }
        y += ROW;

        // ── Row 3: Delay ──────────────────────────────────────────
        tog(cx + LEFT, y, delayEnabled, v -> { delayEnabled = v; snapshot(); rebuild(); });
        if (delayEnabled) {
            // Range sub-toggle (right side of row)
            addRenderableWidget(Button.builder(
                    Component.literal(rangeEnabled ? "§bRange" : "§7Range"),
                    b -> { rangeEnabled = !rangeEnabled; snapshot(); rebuild(); })
                .bounds(cx + 80, y, 52, 16).build());

            if (!rangeEnabled) {
                // Fixed field
                fixedField = field(cx + LEFT + TOG_W + 80, y, FLD_W, fmt(delayFixed), "1.0");
            } else {
                // Min – Max fields
                minField = field(cx + LEFT + TOG_W + 80,          y, FLD_W - 4, fmt(delayMin), "1.0");
                maxField = field(cx + LEFT + TOG_W + 80 + FLD_W,  y, FLD_W - 4, fmt(delayMax), "2.0");
            }
        }
        y += ROW + 8;

        // ── Save / Cancel ─────────────────────────────────────────
        addRenderableWidget(Button.builder(Component.literal("✔ Save"), b -> trySave())
            .bounds(cx - 96, y, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("← Cancel"),
                b -> minecraft.setScreen(parent))
            .bounds(cx + 6, y, 90, 20).build());
    }

    /** Save live text-field values into state, then reinit widgets */
    private void rebuild() {
        snapshot();
        init();
    }

    private void snapshot() {
        if (triggerField != null) liveTrigger = triggerField.getValue();
        if (replyField   != null) liveReply   = replyField.getValue();
        if (chanceField      != null) chance      = parseInt(chanceField.getValue(),        chance);
        if (waitTimeoutField != null) waitTimeout = parseInt(waitTimeoutField.getValue(),   waitTimeout);
        if (fixedField   != null) delayFixed  = parseFloat(fixedField.getValue(), delayFixed);
        if (minField     != null) delayMin    = parseFloat(minField.getValue(),   delayMin);
        if (maxField     != null) delayMax    = parseFloat(maxField.getValue(),   delayMax);
    }

    private void trySave() {
        snapshot();
        String trigger = liveTrigger.trim();
        String reply   = liveReply.trim();
        if (trigger.isEmpty() || reply.isEmpty()) return;

        ReplyRule r = ruleIdx == -1 ? new ReplyRule() : ChatSmartClient.rules.get(ruleIdx);
        r.trigger           = trigger;
        r.reply             = reply;
        r.chanceEnabled     = chanceEnabled;
        r.chance            = clamp(chance, 1, 100);
        r.waitRepeatEnabled = waitEnabled;
        r.waitTimeoutSecs   = Math.max(1, waitTimeout);
        r.delayEnabled      = delayEnabled;
        r.delayFixed        = Math.max(0, delayFixed);
        r.rangeEnabled      = rangeEnabled;
        r.delayMin          = Math.max(0, delayMin);
        r.delayMax          = Math.max(0, delayMax);
        if (ruleIdx == -1) { r.enabled = true; ChatSmartClient.rules.add(r); }
        Config.save();
        minecraft.setScreen(parent);
    }

    // ── Render ────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {

        ctx.fill(0, 0, width, height, 0xFF101010);

        super.render(ctx, mx, my, delta);        int cx = width / 2;
        int y  = topY();

        ctx.drawCenteredString(font,
                ruleIdx == -1 ? "§lNew Reply Rule" : "§lEdit Reply Rule", cx, y - 12, 0xFFFFFFFF);

        ctx.drawString(font, "§7Trigger:", cx + LEFT, y - 1,  0xAAAAAA); y += 22;
        ctx.drawString(font, "§7Reply:",   cx + LEFT, y - 1,  0xAAAAAA); y += 26;

        // Divider
        ctx.fill(cx - 192, y - 2, cx + 192, y - 1, 0x33FFFFFF);
        ctx.drawCenteredString(font, "§8— Settings —", cx, y - 10, 0xFF444444);

        // Chance
        ctx.drawString(font,
                "Chance: " + (chanceEnabled ? "§e" + chance + "§7%" : "§8off"),
                cx + LEFT + TOG_W + 4, y + 3, 0xFFAAAAAA);
        if (chanceEnabled)
            ctx.drawString(font, "§7%", cx + LEFT + TOG_W + 80 + FLD_W + 2, y + 3, 0xFF777777);
        y += ROW;

        // Wait repeat
        ctx.drawString(font,
                "Wait for someone to say reply first: " + (waitEnabled ? "on" : "off"),
                cx + LEFT + TOG_W + 4, y + 3, waitEnabled ? 0xAAFFAA : 0xAAAAAA);
        if (waitEnabled) {
            ctx.drawString(font, "timeout:",
                    cx + LEFT + TOG_W + 4, y + 13, 0xFF777777);
            ctx.drawString(font, "s",
                    cx + LEFT + TOG_W + 80 + FLD_W + 2, y + 3, 0xFF777777);
        }
        y += ROW;

        // Delay
        String delayDesc = !delayEnabled ? "§8off"
                : rangeEnabled ? "§b" + fmt(delayMin) + "–" + fmt(delayMax) + "s"
                               : "§e" + fmt(delayFixed) + "s";
        ctx.drawString(font,
                "Delay: " + delayDesc, cx + LEFT + TOG_W + 4, y + 3, 0xFFAAAAAA);
        if (delayEnabled && !rangeEnabled)
            ctx.drawString(font, "§7s",
                    cx + LEFT + TOG_W + 80 + FLD_W + 2, y + 3, 0xFF777777);
        if (delayEnabled && rangeEnabled)
            ctx.drawString(font, "§7–",
                    cx + LEFT + TOG_W + 80 + FLD_W - 4, y + 3, 0xFF888888);
    }

    // ── Widget helpers ────────────────────────────────────────────

    private EditBox field(int x, int y, int w, String text, String placeholder) {
        EditBox f = new EditBox(font, x, y, w, 16, Component.literal(""));
        f.setMaxLength(300);
        f.setValue(text);
        f.setHint(Component.literal(placeholder));
        addRenderableWidget(f);
        return f;
    }

    /** Toggle button: calls setter with new boolean value */
    private void tog(int x, int y, boolean state,
                     java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(
                Component.literal(state ? "§aON" : "§7OFF"),
                b -> setter.accept(!state))
            .bounds(x, y, TOG_W, 16).build());
    }

    private int topY()       { return height / 2 - 88; }
    private String fmt(float f) {
        return f == (int) f ? String.valueOf((int) f) : String.format("%.1f", f);
    }
    private int   parseInt(String s, int   fb) { try { return Integer.parseInt(s.trim()); }   catch (Exception e) { return fb; } }
    private float parseFloat(String s, float fb){ try { return Float.parseFloat(s.trim()); }  catch (Exception e) { return fb; } }
    private int   clamp(int v, int lo, int hi)  { return Math.max(lo, Math.min(hi, v)); }

    /** Override to prevent the blurred world from rendering behind our GUI */
    @Override
    public void renderInGameBackground(GuiGraphics ctx) {
        ctx.fill(0, 0, width, height, 0xFF111111);
    }


}
