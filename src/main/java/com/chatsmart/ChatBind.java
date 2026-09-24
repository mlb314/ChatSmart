package com.chatsmart;

/**
 * One chat bind: press [keyCode] → send [message] into chat.
 * message starting with "/" is sent as a command.
 * [enabled] lets you pause a bind without deleting it.
 */
public class ChatBind {
    public String  label   = "";   // human-readable key name, e.g. "G"
    public int     keyCode = -1;   // GLFW key code
    public String  message = "";   // what to send
    public boolean enabled = true;

    /** Transient — not saved, used only for debounce this session */
    public transient boolean held = false;

    public ChatBind() {}
    public ChatBind(String label, int keyCode, String message) {
        this.label   = label;
        this.keyCode = keyCode;
        this.message = message;
        this.enabled = true;
    }
}
