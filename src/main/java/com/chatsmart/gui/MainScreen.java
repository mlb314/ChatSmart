package com.chatsmart.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Root screen — opened with Right Shift.
 * Two buttons: Reply Rules | Chat Binds
 */
public class MainScreen extends Screen {

    public MainScreen() { super(Component.literal("ChatSmart")); }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;

        addRenderableWidget(Button.builder(
                Component.literal("Reply Rules"),
                b -> minecraft.setScreen(new RulesScreen(this)))
            .bounds(cx - 80, cy - 22, 160, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Chat Binds"),
                b -> minecraft.setScreen(new BindsScreen(this)))
            .bounds(cx - 80, cy + 6, 160, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Close"), b -> onClose())
            .bounds(cx - 40, cy + 40, 80, 18).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {

        ctx.fill(0, 0, width, height, 0xFF101010);
        ctx.drawCenteredString(font, "§a§lChatSmart", width / 2, height / 2 - 42, 0xFFFFFFFF);
        ctx.drawCenteredString(font, "Right Shift to reopen", width / 2, height / 2 - 30, 0xFF555555);
        super.render(ctx, mx, my, delta);
    }

    /** Override to prevent the blurred world from rendering behind our GUI */
    @Override
    public void renderInGameBackground(GuiGraphics ctx) {
        ctx.fill(0, 0, width, height, 0xFF111111);
    }


}
