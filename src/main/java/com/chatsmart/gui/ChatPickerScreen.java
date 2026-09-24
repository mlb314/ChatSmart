package com.chatsmart.gui;

import com.chatsmart.ChatSmartClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatPickerScreen extends Screen {

    private final Screen parent;
    private final int    ruleIdx;

    private List<String> messages;
    private int selected = -1;
    private int scroll   = 0;

    // Rows are pure text — no overlapping buttons
    private static final int ROW_H    = 12;
    private static final int HEADER_H = 26;
    private static final int FOOTER_H = 44;
    private static final int MARGIN   = 4;

    public ChatPickerScreen(Screen parent, int ruleIdx) {
        super(Component.literal("Pick a Message"));
        this.parent  = parent;
        this.ruleIdx = ruleIdx;
    }

    @Override
    protected void init() {
        messages = new ArrayList<>();
        List<Component> raw = new ArrayList<>(ChatSmartClient.recentChat);
        Collections.reverse(raw);
        for (Component t : raw) messages.add(strip(t.getString()));

        int cx = width / 2;

        addRenderableWidget(Button.builder(Component.literal("Use Selected ->"), b -> {
            if (selected >= 0 && selected < messages.size())
                minecraft.setScreen(new AddEditRuleScreen(parent, ruleIdx, messages.get(selected)));
        }).bounds(cx - 130, footerY(), 124, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Type manually"),
                b -> minecraft.setScreen(new AddEditRuleScreen(parent, ruleIdx, "")))
            .bounds(cx + 6, footerY(), 124, 20).build());

        addRenderableWidget(Button.builder(Component.literal("< Cancel"),
                b -> minecraft.setScreen(parent))
            .bounds(cx - 36, footerY() + 22, 72, 16).build());
    }

    private int visibleRows() { return Math.max(1, (height - HEADER_H - FOOTER_H - MARGIN * 2) / ROW_H); }
    private int listTop()     { return MARGIN + HEADER_H; }
    private int footerY()     { return height - FOOTER_H + 2; }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        int cx  = width / 2;
        int vis = visibleRows();
        int top = listTop();

        ctx.fill(0, 0, width, height, 0xFF111111);

        // Header
        ctx.drawCenteredString(font, "Select a chat message", cx, MARGIN, 0xFFFFFFFF);
        ctx.drawCenteredString(font, "click to select, then Use Selected", cx, MARGIN + 11, 0xFF666666);
        ctx.fill(cx - 200, top - 1, cx + 200, top, 0x33FFFFFF);

        if (messages.isEmpty())
            ctx.drawCenteredString(font, "No chat captured yet.", cx, top + 20, 0xFF777777);

        int end = Math.min(scroll + vis, messages.size());

        // Draw message rows — pure text, no overlapping buttons
        for (int i = scroll; i < end; i++) {
            int     y   = top + (i - scroll) * ROW_H;
            boolean sel = i == selected;
            boolean hov = mx >= cx-200 && mx <= cx+200 && my >= y && my < y+ROW_H;

            // Row background
            int bg = sel ? 0xFF004D00 : hov ? 0xFF252525 : (i % 2 == 0 ? 0xFF181818 : 0xFF1E1E1E);
            ctx.fill(cx - 200, y, cx + 200, y + ROW_H - 1, bg);

            if (sel) {
                ctx.fill(cx-200, y,         cx+200, y+1,       0xFF33BB33);
                ctx.fill(cx-200, y+ROW_H-2, cx+200, y+ROW_H-1, 0xFF33BB33);
            }

            String msg  = messages.get(i);
            int    maxC = (width - 16) / 6;
            String disp = msg.length() > maxC ? msg.substring(0, maxC-2)+"..." : msg;
            ctx.drawString(font, disp,
                    cx - 196, y + 2, sel ? 0xFF77FF77 : 0xFFBBBBBB);
        }

        if (messages.size() > vis)
            ctx.drawCenteredString(font,
                    "scroll  (" + (scroll+1) + "-" + end + " / " + messages.size() + ")",
                    cx, footerY() - 11, 0xFF444444);

        // Buttons (footer only, never overlap message rows)
        for (var child : this.children()) {
            if (child instanceof Renderable d) d.render(ctx, mx, my, delta);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean handled) {
        if (!handled) {
            double mx = click.x(), my = click.y();
            int cx = width / 2, top = listTop();
            int end = Math.min(scroll + visibleRows(), messages.size());
            for (int i = scroll; i < end; i++) {
                int y = top + (i - scroll) * ROW_H;
                if (mx >= cx-200 && mx <= cx+200 && my >= y && my < y+ROW_H) {
                    selected = i; return true;
                }
            }
        }
        return super.mouseClicked(click, handled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, messages.size() - visibleRows());
        scroll  = scrollY < 0 ? Math.min(scroll+1, max) : Math.max(scroll-1, 0);
        return true;
    }

    @Override
    public void renderInGameBackground(GuiGraphics ctx) {
        ctx.fill(0, 0, width, height, 0xFF111111);
    }

    private static String strip(String s) {
        return s.replaceAll("(?i)\u00a7[0-9a-fk-or]", "");
    }
}
