package com.chatsmart.gui;

import com.chatsmart.ChatBind;
import com.chatsmart.ChatSmartClient;
import com.chatsmart.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class AddEditBindScreen extends Screen {

    private final Screen parent;
    private final int    bindIdx;

    private int     capturedKey   = -1;
    private boolean waitingForKey = false;
    private String  savedMsg      = "";

    private EditBox messageField;

    public AddEditBindScreen(Screen parent, int bindIdx) {
        super(Component.literal(bindIdx == -1 ? "New Bind" : "Edit Bind"));
        this.parent  = parent;
        this.bindIdx = bindIdx;
        if (bindIdx >= 0) {
            ChatBind b = ChatSmartClient.binds.get(bindIdx);
            capturedKey = b.keyCode;
            savedMsg    = b.message;
        }
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;

        String keyLabel = capturedKey == -1
                ? "[ Click here, then press a key ]"
                : "[ " + keyName(capturedKey) + " ]   (click to re-capture)";

        addRenderableWidget(Button.builder(Component.literal(keyLabel),
                b -> { waitingForKey = true; rebuildWidgets(); })
            .bounds(cx - 140, cy - 26, 280, 22).build());

        messageField = new EditBox(font, cx - 140, cy + 12, 280, 20,
                Component.literal("Message"));
        messageField.setMaxLength(256);
        messageField.setValue(savedMsg);
        messageField.setHint(Component.literal("/home  or  gg everyone!"));
        addRenderableWidget(messageField);

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            String msg = messageField.getValue().trim();
            if (capturedKey != -1 && !msg.isEmpty()) {
                if (bindIdx == -1) {
                    ChatSmartClient.binds.add(new ChatBind(keyName(capturedKey), capturedKey, msg));
                } else {
                    ChatBind bind  = ChatSmartClient.binds.get(bindIdx);
                    bind.keyCode   = capturedKey;
                    bind.label     = keyName(capturedKey);
                    bind.message   = msg;
                }
                Config.save();
                minecraft.setScreen(parent);
            }
        }).bounds(cx - 140, cy + 44, 135, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"),
                b -> minecraft.setScreen(parent))
            .bounds(cx + 5, cy + 44, 135, 20).build());
    }

    // 26.1.2: keyPressed takes a KeyInput object (net.minecraft.client.input.KeyInput)
    @Override
    public boolean keyPressed(KeyInput input) {
        if (waitingForKey) {
            int keyCode = input.key();
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) capturedKey = keyCode;
            waitingForKey = false;
            savedMsg = messageField != null ? messageField.getValue() : savedMsg;
            rebuildWidgets();
            if (messageField != null) messageField.setValue(savedMsg);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        int cx = width / 2, cy = height / 2;
        ctx.drawCenteredString(font,
                bindIdx == -1 ? "New Key Bind" : "Edit Key Bind", cx, cy - 46, 0xFFFFFFFF);
        ctx.drawString(font, "Key:",     cx - 140, cy - 30, 0xFF999999);
        ctx.drawString(font, "Message:", cx - 140, cy,      0x999999);
        ctx.drawCenteredString(font,
                "Start with / for commands (e.g. /home)", cx, cy + 68, 0xFF444444);
        if (waitingForKey) {
            ctx.fill(cx - 145, cy - 31, cx + 145, cy - 9, 0xCC000000);
            ctx.drawCenteredString(font, "Press any key...", cx, cy - 24, 0xFFFFFF55);
        }
        super.render(ctx, mx, my, delta);
    }

    @Override
    public void renderInGameBackground(GuiGraphics ctx) {
        ctx.fill(0, 0, width, height, 0xFF111111);
    }

    private void rebuildWidgets() {
        savedMsg = messageField != null ? messageField.getValue() : savedMsg;
        clearWidgets();
        init();
        if (messageField != null) messageField.setValue(savedMsg);
    }

    private String keyName(int kc) {
        String n = GLFW.glfwGetKeyName(kc, 0);
        return n != null ? n.toUpperCase() : "KEY_" + kc;
    }
}
