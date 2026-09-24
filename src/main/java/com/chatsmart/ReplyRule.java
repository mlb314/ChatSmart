package com.chatsmart;

public class ReplyRule {
    // Core
    public String  trigger = "";
    public String  reply   = "";
    public boolean enabled = true;

    // Chance
    public boolean chanceEnabled = false;
    public int     chance        = 100;

    // Wait for someone else to say the reply first
    public boolean waitRepeatEnabled  = false;
    public int     waitTimeoutSecs    = 30;   // fizzle if nobody says it within this time

    // Delay
    public boolean delayEnabled = false;
    public float   delayFixed   = 1.0f;
    public boolean rangeEnabled = false;
    public float   delayMin     = 1.0f;
    public float   delayMax     = 2.0f;

    public ReplyRule() {}
    public ReplyRule(String trigger, String reply) {
        this.trigger = trigger;
        this.reply   = reply;
    }
}
