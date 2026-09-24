# ChatSmart

A Fabric client mod for Minecraft **26.1.2**: auto-reply rules (trigger phrase →
scripted reply, with optional chance/delay/"wait for someone else to say it
first") and simple chat key binds (press a key → send a chat message or
command).

This repo is set up to be **built entirely on GitHub** — you don't need
Gradle, Java, or Minecraft installed locally. Every push triggers
`.github/workflows/build.yml`, which compiles the mod and uploads the built
jar as a downloadable workflow artifact.

## Building on GitHub

1. Push this repo to GitHub (see below).
2. Go to the **Actions** tab → the `build` workflow runs automatically.
3. Once it finishes, open the run → under **Artifacts**, download
   `chatsmart-jars`. That zip contains `chatsmart-<version>.jar`, ready to
   drop into your `mods` folder alongside Fabric Loader 0.19.3+ and Fabric
   API for 26.1.2.
4. You can also trigger a build manually from the Actions tab
   (**Run workflow**) without pushing a commit.

## Pushing this repo to GitHub

From this project folder:

```bash
git init
git add .
git commit -m "Port ChatSmart to Minecraft 26.1.2"
git branch -M main
git remote add origin https://github.com/<your-username>/chatsmart.git
git push -u origin main
```

(Create the empty `chatsmart` repository on GitHub first, without a README,
so there's no merge conflict.)

## About the 26.1.2 port

Minecraft 26.1 was a huge break for modding: **the game shipped unobfuscated
for the first time**, Yarn mappings were retired, and everyone moved onto
Mojang's official mapping names. That means this isn't a small version bump —
essentially every Minecraft-side class and method name in the GUI code
changed. What was updated here:

- **Build script**: Loom plugin id changed to `net.fabricmc.fabric-loom`, the
  `mappings` dependency was removed entirely (26.1.2's Minecraft jar already
  ships with official names, so there's nothing to remap), `modImplementation`
  → `implementation`, Java target bumped to 25, and dependency versions bumped
  (`loader_version=0.19.3`, `fabric_api_version=0.155.0+26.1.2`,
  Loom `1.15-SNAPSHOT`).
- **Code**: renamed per Mojang's mappings — `MinecraftClient`→`Minecraft`,
  `DrawContext`→`GuiGraphics`, `ButtonWidget`→`Button`,
  `TextFieldWidget`→`EditBox`, `KeyBinding`→`KeyMapping`, `Text`→`Component`,
  `Identifier`→`ResourceLocation`, `Screen.client`→`Screen.minecraft`,
  `.dimensions(...)`→`.bounds(...)`, `addDrawableChild`→`addRenderableWidget`,
  `getText/setText`→`getValue/setValue`, etc. Fabric API's own classes were
  renamed too (per Fabric's official 26.1 migration list) —
  `KeyBindingHelper`→`KeyMappingHelper`.

### A few spots worth double-checking against the Actions log

A handful of very recently-added APIs (added in late-2025/2026 snapshots,
after my training data) don't have a fully confirmed official name yet on my
end — I made the most consistent, best-supported choice for each, but if the
build fails, these are the first places to look:

- `net.minecraft.client.input.KeyInput` — the wrapper object now passed into
  `Screen.keyPressed(...)` (used in `AddEditBindScreen`). The package/class
  name may have shifted slightly.
- `net.minecraft.client.gui.Click` — same idea, for
  `Screen.mouseClicked(Click, boolean)` in `ChatPickerScreen`.
- `Screen.renderInGameBackground(GuiGraphics)` — the method that suppresses
  the blurred world background behind our GUI.

If GitHub Actions reports a compile error on any of these, paste the error
log back and it's a quick, targeted fix — no need to touch anything else.

## Project layout

```
build.gradle              Gradle build script (Loom)
gradle.properties         Minecraft/Loader/Loom/Fabric API versions
settings.gradle
src/main/java/com/chatsmart/
  ChatSmartClient.java     Mod entrypoint, tick/chat-event handling
  ChatBind.java            Key bind data model
  ReplyRule.java           Auto-reply rule data model
  Config.java              JSON persistence (rules.json / binds.json)
  gui/                     In-game config screens (Right Shift to open)
src/main/resources/fabric.mod.json
.github/workflows/build.yml   CI build
```
