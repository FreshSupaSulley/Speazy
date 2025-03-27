package com.supasulley.censorcraft.gui;

import java.util.Collection;

import com.supasulley.censorcraft.Config;
import com.supasulley.censorcraft.gui.MicrophoneList.MicrophoneEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MicrophoneList extends ObjectSelectionList<MicrophoneEntry> {
	
	public MicrophoneList(int x, int y, int width, int height, Minecraft minecraft, Collection<String> microphones)
	{
		super(minecraft, width, height, x, y);
		
		microphones.forEach(sample ->
		{
			this.addEntry(new MicrophoneEntry(sample));
		});
		
		// Select the desired, otherwise just the first one
		setSelected(this.children().stream().filter(mic -> mic.micName.equals(Config.Client.PREFERRED_MIC.get())).findFirst().orElse(this.getFirstElement()));
	}
	
	// @Override
	// public int getRowWidth()
	// {
	// return this.width * 3 / 4;
	// }
	
	@OnlyIn(Dist.CLIENT)
	class MicrophoneEntry extends ObjectSelectionList.Entry<MicrophoneEntry> {
		
		private final String micName;
//		private final MultiLineLabel label;
		
		private MicrophoneEntry(final String micName)
		{
			this.micName = micName;
//			this.micName = micName;
//			label = MultiLineLabel.create(minecraft.font, Component.literal(micName));
		}
		
		@Override
		public void render(GuiGraphics graphics, int p_282727_, int p_283089_, int p_283116_, int p_281268_, int p_283038_, int p_283070_, int p_282448_, boolean p_281417_, float p_283226_)
		{
			graphics.drawString(minecraft.font, this.micName, p_283116_, p_283089_, -1);
//			label.renderLeftAligned(graphics, p_283116_, p_283089_ + 12, 9, -1);
		}
		
		@Override
		public Component getNarration()
		{
			return Component.literal(micName);
		}
	}
}
