package com.chatsmart.gui;

import com.chatsmart.ChatSmartClient;
import com.chatsmart.Config;
import com.chatsmart.ReplyRule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RulesScreen extends Screen {

    private final Screen parent;
    private int scroll = 0;

    // Two-line rows: line 1 = trigger→reply text, line 2 = buttons
    private static final int ROW_H    = 28;  // tall enough for 2 lines
    private static final int HEADER_H = 24;
    private static final int FOOTER_H = 28;
    private static final int MARGIN   = 6;

    public RulesScreen(Screen parent) {
        super(Component.literal("Reply Rules"));
        this.parent = parent;
    }

    @Override protected void init() { build(); }

    private int visibleRows() {
        return Math.max(1, (height - HEADER_H - FOOTER_H - MARGIN * 2) / ROW_H);
    }
    private int listTop()  { return MARGIN + HEADER_H; }
    private int footerY()  { return height - FOOTER_H + 4; }

    private void build() {
        clearWidgets();
        List<ReplyRule> rules = ChatSmartClient.rules;
        int cx  = width / 2;
        int vis = visibleRows();
        int top = listTop();
        int end = Math.min(scroll + vis, rules.size());

        for (int i = scroll; i < end; i++) {
            final int idx  = i;
            ReplyRule rule = rules.get(i);
            // Buttons on the BOTTOM line of the row (y + 14)
            int y = top + (i - scroll) * ROW_H;

            addRenderableWidget(Button.builder(
                    Component.literal(rule.enabled ? "ON" : "OFF"),
                    b -> { rule.enabled = !rule.enabled; Config.save(); build(); })
                .bounds(cx - 195, y + 14, 38, 12).build());

            addRenderableWidget(Button.builder(
                    Component.literal("Edit"),
                    b -> minecraft.setScreen(new AddEditRuleScreen(this, idx)))
                .bounds(cx + 100, y + 14, 36, 12).build());

            addRenderableWidget(Button.builder(
                    Component.literal("X"),
                    b -> { rules.remove(idx); Config.save();
                           scroll = Math.max(0, Math.min(scroll, rules.size() - vis));
                           build(); })
                .bounds(cx + 140, y + 14, 18, 12).build());
        }

        addRenderableWidget(Button.builder(Component.literal("+ Add Rule"),
                b -> minecraft.setScreen(new ChatPickerScreen(this, -1)))
            .bounds(cx - 98, footerY(), 94, 20).build());

        addRenderableWidget(Button.builder(Component.literal("< Back"),
                b -> minecraft.setScreen(parent))
            .bounds(cx + 4, footerY(), 94, 20).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        int cx  = width / 2;
        int vis = visibleRows();
        int top = listTop();

        ctx.fill(0, 0, width, height, 0xFF111111);

        // Header
        ctx.drawCenteredString(font, "Reply Rules", cx, MARGIN, 0xFFFFFFFF);
        ctx.drawCenteredString(font, "trigger in chat -> auto send reply", cx, MARGIN + 11, 0xFF666666);
        ctx.fill(cx - 195, top - 1, cx + 165, top, 0x33FFFFFF);

        List<ReplyRule> rules = ChatSmartClient.rules;

        if (rules.isEmpty())
            ctx.drawCenteredString(font, "No rules yet. Press + Add Rule.", cx, top + 20, 0xFF888888);

        int end = Math.min(scroll + vis, rules.size());
        for (int i = scroll; i < end; i++) {
            ReplyRule rule = rules.get(i);
            int y = top + (i - scroll) * ROW_H;

            // Alternating row background
            ctx.fill(cx - 195, y, cx + 165, y + ROW_H - 1,
                    i % 2 == 0 ? 0xFF1A1A1A : 0xFF202020);

            // TOP line: trigger -> reply  (y + 3, above buttons)
            String trig = strip(clip(rule.trigger, 22));
            String rep  = strip(clip(rule.reply,   26));
            int col = rule.enabled ? 0xFFFFDD88 : 0xFF777777;
            ctx.drawString(font, trig, cx - 190, y + 3, col);
            ctx.drawString(font, "->",  cx + 10,  y + 3, 0xFF444444);
            ctx.drawString(font, rep,   cx + 22,  y + 3, 0xFFCCCCCC);
        }

        // Scroll hint
        if (rules.size() > vis)
            ctx.drawCenteredString(font,
                    "scroll  (" + (scroll+1) + "-" + end + " / " + rules.size() + ")",
                    cx, footerY() - 11, 0xFF444444);

        // Draw buttons last
        for (var child : this.children()) {
            if (child instanceof Renderable d) d.render(ctx, mx, my, delta);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, ChatSmartClient.rules.size() - visibleRows());
        scroll  = scrollY < 0 ? Math.min(scroll+1, max) : Math.max(scroll-1, 0);
        build(); return true;
    }

    @Override
    public void renderInGameBackground(GuiGraphics ctx) {
        ctx.fill(0, 0, width, height, 0xFF111111);
    }

    private String clip(String s, int n) { return s.length() > n ? s.substring(0,n-2)+"..." : s; }
    private String strip(String s)       { return s.replaceAll("(?i)\u00a7[0-9a-fk-or]", ""); }
}
