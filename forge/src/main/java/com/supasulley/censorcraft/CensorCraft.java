package com.supasulley.censorcraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.supasulley.jscribe.JScribe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CensorCraft.MODID)
public class CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	private JScribe controller;
	
	private static final int PROTOCOL_VERSION = 1;
	private SimpleChannel channel;
	
	public CensorCraft(FMLJavaModLoadingContext context)
	{
		// Forbidden words are defined at the server level
		Config.register(context);
		
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
		context.getModEventBus().addListener(this::clientSetup);
		context.getModEventBus().addListener(this::commonSetup);
	}
	
	public void commonSetup(FMLCommonSetupEvent event)
	{
		event.enqueueWork(() ->
		{
			channel = ChannelBuilder.named(CensorCraft.MODID).networkProtocolVersion(PROTOCOL_VERSION).simpleChannel();
			channel.messageBuilder(WordPacket.class, NetworkDirection.PLAY_TO_SERVER).encoder(WordPacket::encode).decoder(WordPacket::decode).consumerMainThread((packet, context) ->
			{
				ServerPlayer player = context.getSender();
				String payload = packet.getPayload();
				
				if(payload.isBlank())
				{
					LOGGER.warn("Received blank payload from {}", player.getUUID());
					return;
				}
				
				LOGGER.info("Received \"{}\" from {} ({})", payload, player.getName().getString(), player.getUUID());
				
				String taboo = Config.Server.TABOO_TREE.containsAnyIgnoreCase(payload);
				if(taboo == null)
					return;
				
				LOGGER.info("Taboo said by {}: \"{}\"!", player.getName().getString(), taboo);
				
				// Notify all players of the sin
				player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(player.getName().getString()).withColor(16711680).append(" said ").withColor(16777215).append("\"" + taboo + "\""), false));
				
				// Kill the player
				if(!player.isDeadOrDying())
				{
					// can the player survive the explosion (besides totems)?
					// can i get rid of new ExplosionDamageCalculator()?
					player.level().explode(null, player.level().damageSources().generic(), new ExplosionDamageCalculator(), player.getX(), player.getY(), player.getZ(), Config.Server.EXPLOSION_RADIUS, Config.Server.EXPLOSION_FIRE, Config.Server.EXPLOSION_GRIEFING ? ExplosionInteraction.BLOCK : ExplosionInteraction.NONE);
				}
			}).add();
		});
	}
	
	public void clientSetup(FMLClientSetupEvent event)
	{
		// Some common setup code
		LOGGER.info("Copying model to temp directory");
		
		try
		{
			Path tempZip = Files.createTempFile("model", ".en.bin");
			tempZip.toFile().deleteOnExit();
			Files.copy(CensorCraft.class.getClassLoader().getResourceAsStream("ggml-tiny.en.bin"), tempZip, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("Put whisper model at {}", tempZip);
			
			controller = new JScribe(tempZip);
		} catch(IOException e)
		{
			LOGGER.error("Failed to load model");
			e.printStackTrace();
		}
	}
	
	private void sendToChat(Component component, boolean h)
	{
		Minecraft.getInstance().getChatListener().handleSystemMessage(component, h);
	}
	
	/**
	 * Sends a string to the in-game chat.
	 * 
	 * @param putAboveInventory true if you want this text to be centered, false if it should go in the chat
	 * @param message           message to send
	 */
	private void sendToChat(String message, boolean putAboveInventory)
	{
		sendToChat(Component.literal(message), putAboveInventory);
	}
	
	/**
	 * Handles when the <b>client-side</b> player joins the world (respawns, logs in, moves between dimensions, etc.)
	 * 
	 * <p>
	 * Only applies to the local player, meaning when server players invoke this event, it's ignored.
	 * </p>
	 * 
	 * @param event {@linkplain EntityJoinLevelEvent}
	 */
	@SubscribeEvent
	public void onJoinWorld(EntityJoinLevelEvent event)
	{
		if(!(event.getEntity() instanceof LocalPlayer))
			return;
		
		try
		{
			if(controller.start())
			{
				MutableComponent component = Component.literal("Now recording. ");
				component.append(Component.literal("Watch your tone...").withColor(16711680));
				sendToChat(component, false);
			}
		} catch(Exception e)
		{
			chatError("Failed to start (" + e.getMessage() + ")");
			LOGGER.error("Failed to start transcription", e);
		}
	}
	
	/**
	 * Handles when the player leaves the world (dies, exits, quits, etc.).
	 * 
	 * <p>
	 * If the player moves between dimensions (i.e. travels to the nether), this doesn't get fired.
	 * </p>
	 * 
	 * @param event {@linkplain EntityLeaveLevelEvent}
	 */
	@SubscribeEvent
	public void onLeaveWorld(EntityLeaveLevelEvent event)
	{
		if(!(event.getEntity() instanceof LocalPlayer))
			return;
		
		controller.stop();
	}
	
	@SubscribeEvent
	public void onLevelTick(LevelTickEvent event)
	{
		// This is only for client ticks
		if(event.side != LogicalSide.CLIENT)
			return;
		
		if(!controller.isRunning())
			return;
		
		// Check if something went wrong
		String error = controller.getError();
		
		if(error != null)
		{
			chatError(error);
			return;
		}
		
		String buffer = controller.getBuffer();
		
		if(!buffer.isBlank())
		{
			LOGGER.info("Received \"{}\" (backlog size: {})", buffer, controller.getBacklog());
			channel.send(new WordPacket(buffer), PacketDistributor.SERVER.noArg());
			// sendToChat(buffer + " " + controller.getBacklog(), true);
			//
			// // if(startTime != 0 && System.currentTimeMillis() - startTime > 10000)
			// if(word != null)
			// {
			// PacketDistributor.SERVER.direction().buildPacket(channel, new WordPacket(buffer));
			// Minecraft.getInstance().getConnection().send(new WordPacket(buffer));
			// sendToChat("sending ", false);
			// startTime = System.currentTimeMillis();
			// channel.send(new WordPacket("boom"), PacketDistributor.SERVER.noArg());
			// }
		}
	}
	
	private void chatError(String error)
	{
		MutableComponent component = Component.literal("Speazy error: " + error + "\n").withColor(16711680);
		component.append(Component.literal("Rejoin world to restart. If the error persists, ").withColor(16777215));
		component.append(Component.literal("open an issue.").withStyle(Style.EMPTY.withColor(16777215).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Visit GitHub repo"))).withClickEvent(new ClickEvent(Action.OPEN_URL, "https://github.com/FreshSupaSulley/Speazy")).withUnderlined(true)));
		sendToChat(component, false);
	}
	
	@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class ClientModEvents {
		
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event)
		{
			LOGGER.info("SPEAZY INIT");
		}
	}
}
