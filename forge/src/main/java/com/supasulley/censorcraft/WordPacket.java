package com.supasulley.censorcraft;

import java.nio.charset.Charset;

import net.minecraft.network.RegistryFriendlyByteBuf;

public class WordPacket {
	
	private String payload;
	
	public WordPacket(String payload)
	{
		this.payload = payload;
	}
	
	public String getPayload()
	{
		return payload;
	}
	
	public static void encode(WordPacket packet, RegistryFriendlyByteBuf buffer)
	{
		byte[] bytes = packet.payload.getBytes(Charset.defaultCharset());
		
		buffer.writeInt(bytes.length);
		buffer.writeBytes(bytes);
	}
	
	public static WordPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new WordPacket(buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString());
	}
}
