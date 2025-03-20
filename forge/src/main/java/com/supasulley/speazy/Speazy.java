package com.supasulley.speazy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Speazy.MODID)
public class Speazy {
	
	// Define mod id in a common place for everything to reference
	public static final String MODID = "speazy";
	// Directly reference a slf4j logger
	public static final Logger LOGGER = LogUtils.getLogger();
	
	private JarController controller;
	
	public Speazy(FMLJavaModLoadingContext context)
	{
		IEventBus modEventBus = context.getModEventBus();
		
		// Register the commonSetup method for modloading
		modEventBus.addListener(this::commonSetup);
		
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
		
		// Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
		context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
	}
	
	private void commonSetup(final FMLCommonSetupEvent event)
	{
		// Some common setup code
		LOGGER.info("HELLO FROM COMMON SETUP");
		
		if(Config.logDirtBlock)
			LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
		
		LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
		
		Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
		
		// Transfer zipped model to temp directory
		try
		{
			Path temp = Files.createTempFile("jar", ".jar");
			temp.toFile().deleteOnExit();
			Files.copy(Speazy.class.getClassLoader().getResourceAsStream("app.jar"), temp, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Copying jar to " + temp);
			
			controller = new JarController(temp.toAbsolutePath().toString());
		} catch(IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * 
	 * @param putAboveInventory true if you want this text to be centered, false if it should go in the chat
	 * @param message
	 */
	private void sendToChat(boolean putAboveInventory, String message)
	{
		Minecraft.getInstance().getChatListener().handleSystemMessage(Component.literal(message), putAboveInventory);
	}
	
	@SubscribeEvent
	public void onPlayerJoinWorld(EntityJoinLevelEvent entity)
	{
		if(entity.getEntity() instanceof Player)
		{
			sendToChat(false, "Player joined world " + ((Player) entity.getEntity()).getScoreboardName());
		}
	}
	
	@SubscribeEvent
	public void onJoinWorld(LevelEvent.Load event)
	{
		LOGGER.info("WE HAVE LOADED");
		
		if(!controller.isRunning())
		{
			try
			{
				controller.start();
				sendToChat(false, "Recording started. Watch yo tone mf");
			} catch(IOException e)
			{
				LOGGER.error("Failed to start");
				e.printStackTrace();
			}
		}
	}
	
	@SubscribeEvent
	public void onLeaveWorld(LevelEvent.Unload event)
	{
		LOGGER.info("WE HAVE UNLOADED");
		
		if(controller.isRunning())
		{
			sendToChat(false, "Stopped recording");
			controller.stop();
		}
	}
	
	@SubscribeEvent
	public void onWorldTick(ServerTickEvent event)
	{
		String buffer = controller.getBuffer();
		
		if(!buffer.isBlank())
		{
			LOGGER.info("Received {}", buffer);
			sendToChat(false, buffer);
		}
	}
	
	// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
	@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class ClientModEvents {
		
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event)
		{
			// Some client setup code
			LOGGER.info("SPEAZY INIT");
		}
	}
}
