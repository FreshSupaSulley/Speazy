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
		super(Component.literal("sup asshole"));
		
		this.minecraft = minecraft;
	}
	
	/**
	 * Ran everytime the config button is pressed.
	 */
	@Override
	protected void init()
	{
		int bottom = addRenderableWidget(Checkbox.builder(Component.literal("Indicate recording"), font).tooltip(Tooltip.create(Component.literal("Displays a microphone icon indicating your microphone is being used"))).pos(PADDING, PADDING).selected(Config.Client.INDICATE_RECORDING.get()).onValueChange((button, value) ->
		{
			Config.Client.INDICATE_RECORDING.set(value);
		}).build()).getBottom();
		
		bottom = addRenderableWidget(Checkbox.builder(Component.literal("Show spoken text"), font).pos(PADDING, bottom + PADDING).tooltip(Tooltip.create(Component.literal("Displays live audio transcriptions when in-game"))).selected(Config.Client.SHOW_TRANSCRIPTION.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_TRANSCRIPTION.set(value);
		}).build()).getBottom();
		
		// List of microphones
		MicrophoneList list = new MicrophoneList(PADDING, bottom + PADDING, this.width - PADDING * 2, this.height - bottom - PADDING * 2, minecraft, AudioRecorder.getMicrophones().stream().map(mic -> mic.getName()).collect(Collectors.toList()));
//		list.setRectangle(PADDING, bottom + PADDING, this.width - PADDING * 2, this.height);
		addRenderableWidget(list);
		
		// Close button
		addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose()).bounds(this.width / 2 - Button.BIG_WIDTH / 2, this.height - 26, Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build());
	}
	
	@Override
	public void render(GuiGraphics p_281549_, int p_281550_, int p_282878_, float p_282465_)
	{
		super.render(p_281549_, p_281550_, p_282878_, p_282465_);
	}
}
