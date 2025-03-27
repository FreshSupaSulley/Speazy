package com.supasulley.censorcraft.gui;

import java.util.stream.Collectors;

import com.supasulley.censorcraft.Config;
import com.supasulley.jscribe.AudioRecorder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
	
	private static final int PADDING = 5;
	
	private Minecraft minecraft;
	
	public ConfigScreen(Minecraft minecraft, Screen screen)
	{
		super(Component.literal("Configure CensorCraft"));
		
		this.minecraft = minecraft;
	}
	
	/**
	 * Ran everytime the config button is pressed.
	 */
	@Override
	protected void init()
	{
		int bottom = addRenderableWidget(Checkbox.builder(Component.literal("Indicate recording"), font).tooltip(Tooltip.create(Component.literal("Displays an icon indicating your microphone is being used"))).pos(PADDING, PADDING).selected(Config.Client.INDICATE_RECORDING.get()).onValueChange((button, value) ->
		{
			Config.Client.INDICATE_RECORDING.set(value);
		}).build()).getBottom();
		
		bottom = addRenderableWidget(Checkbox.builder(Component.literal("Show spoken text"), font).pos(PADDING, bottom + PADDING).tooltip(Tooltip.create(Component.literal("Displays live audio transcriptions"))).selected(Config.Client.SHOW_TRANSCRIPTION.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_TRANSCRIPTION.set(value);
		}).build()).getBottom();
		
		// Close button
		final int closeButtonY = this.height - Button.DEFAULT_HEIGHT - PADDING;
		final int listWidth = this.width / 2;
		
		// List of microphones
		MicrophoneList list = new MicrophoneList(PADDING + listWidth / 2, bottom + PADDING, listWidth - PADDING * 2, closeButtonY - bottom - PADDING * 2, minecraft, AudioRecorder.getMicrophones().stream().map(mic -> mic.getName()).collect(Collectors.toList()));
		addRenderableWidget(list);
		
		addRenderableWidget(Button.builder(Component.literal("Close"), button ->
		{
			Config.Client.PREFERRED_MIC.set(list.getSelected().getMicrophoneName());
			this.onClose();
		}).bounds(this.width / 2 - Button.BIG_WIDTH / 2, closeButtonY, Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build());
	}
	
	@Override
	public void render(GuiGraphics p_281549_, int p_281550_, int p_282878_, float p_282465_)
	{
		super.render(p_281549_, p_281550_, p_282878_, p_282465_);
	}
}
