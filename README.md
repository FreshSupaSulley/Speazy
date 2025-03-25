# CensorCraft

Minecraft mod that listens to your voice and kills you for speaking forbidden words.

All clients are responsible for their own audio transcription, where the live audio feed is transcribed locally before the text is sent to the server where it explodes the player if they spoke a forbidden word.

# Installation

Use [CurseForge](https://www.curseforge.com/download/app) to run the mod. Other launchers have not yet been tested.

## Config

Use the server config file found at *world/serverconfig/censorcraft-server.toml* to edit the list of forbidden words, change explosion parameters, etc.

## macOS

Use the CurseForge launcher. Go to **Settings** and select **Skip launcher with CurseForge**.

Because this mod requires a microphone, you need to use a launcher that supports requesting microphone usage. The vanilla Minecraft launcher does not allow the ability to do this, so you need to use another launcher.

# Gradle

This is a multi-project gradle build:

- [forge](./forge)
Forge mod source. Depends on JScribe.
- [jscribe](./jscribe)
Records and transcribes speech-to-text. Depends on macrophone.
- [macrophone](./macrophone)
Allows macOS clients to interact with the microphone.
