package com.chatsmart.gui;

import com.chatsmart.ChatBind;
import com.chatsmart.ChatSmartClient;
import com.chatsmart.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BindsScreen extends Screen {

    private final Screen parent;
    private int scroll = 0;

    private static final int ROW_H    = 28;
    private static final int HEADER_H = 24;
    private static final int FOOTER_H = 28;
    private static final int MARGIN   = 6;

    public BindsScreen(Screen parent) {
        super(Component.literal("Chat Binds"));
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
        List<ChatBind> binds = ChatSmartClient.binds;
        int cx  = width / 2;
        int vis = visibleRows();
        int top = listTop();
        int end = Math.min(scroll + vis, binds.size());

        for (int i = scroll; i < end; i++) {
            final int idx  = i;
            ChatBind  bind = binds.get(i);
            int y = top + (i - scroll) * ROW_H;

            addRenderableWidget(Button.builder(
                    Component.literal(bind.enabled ? "ON" : "OFF"),
                    b -> { bind.enabled = !bind.enabled; Config.save(); build(); })
                .bounds(cx - 195, y + 14, 38, 12).build());

            addRenderableWidget(Button.builder(
                    Component.literal("Edit"),
                    b -> minecraft.setScreen(new AddEditBindScreen(this, idx)))
                .bounds(cx + 100, y + 14, 36, 12).build());

            addRenderableWidget(Button.builder(
                    Component.literal("X"),
                    b -> { binds.remove(idx); Config.save();
                           scroll = Math.max(0, Math.min(scroll, binds.size() - vis));
                           build(); })
                .bounds(cx + 140, y + 14, 18, 12).build());
        }

        addRenderableWidget(Button.builder(Component.literal("+ Add Bind"),
                b -> minecraft.setScreen(new AddEditBindScreen(this, -1)))
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

        ctx.drawCenteredString(font, "Chat Binds", cx, MARGIN, 0xFFFFFFFF);
        ctx.drawCenteredString(font, "press key -> send message", cx, MARGIN + 11, 0xFF666666);
        ctx.fill(cx - 195, top - 1, cx + 165, top, 0x33FFFFFF);

        List<ChatBind> binds = ChatSmartClient.binds;
        if (binds.isEmpty())
            ctx.drawCenteredString(font, "No binds yet. Press + Add Bind.", cx, top + 20, 0xFF888888);

        int end = Math.min(scroll + vis, binds.size());
        for (int i = scroll; i < end; i++) {
            ChatBind bind = binds.get(i);
            int y = top + (i - scroll) * ROW_H;

            ctx.fill(cx - 195, y, cx + 165, y + ROW_H - 1,
                    i % 2 == 0 ? 0xFF1A1A1A : 0xFF202020);

            // TOP line: [KEY] -> message
            String key = bind.keyCode != -1 ? "[" + keyName(bind.keyCode) + "]" : "[?]";
            String msg = clip(bind.message, 30);
            int keyCol = bind.enabled ? 0xFF88CCFF : 0xFF777777;
            ctx.drawString(font, key, cx - 190, y + 3, keyCol);
            ctx.drawString(font, "->", cx + 10,  y + 3, 0xFF444444);
            ctx.drawString(font, msg,  cx + 22,  y + 3, 0xFFCCCCCC);
        }

        if (binds.size() > vis)
            ctx.drawCenteredString(font,
                    "scroll  (" + (scroll+1) + "-" + end + " / " + binds.size() + ")",
                    cx, footerY() - 11, 0xFF444444);

        // Draw buttons last
        for (var child : this.children()) {
            if (child instanceof Renderable d) d.render(ctx, mx, my, delta);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, ChatSmartClient.binds.size() - visibleRows());
        scroll  = scrollY < 0 ? Math.min(scroll+1, max) : Math.max(scroll-1, 0);
        build(); return true;
    }

    @Override
    public void renderInGameBackground(GuiGraphics ctx) {
        ctx.fill(0, 0, width, height, 0xFF111111);
    }

    private String clip(String s, int n) { return s.length() > n ? s.substring(0,n-2)+"..." : s; }
    private String keyName(int kc) {
        String n = GLFW.glfwGetKeyName(kc, 0);
        return n != null ? n.toUpperCase() : "KEY" + kc;
    }
}
