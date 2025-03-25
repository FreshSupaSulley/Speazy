package com.supasulley.censorcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public abstract class Config {
	
	private static Client client;
	private static Server server;
	
	public static void register(FMLJavaModLoadingContext context)
	{
		context.registerConfig(ModConfig.Type.CLIENT, (client = new Client()).register());
		context.registerConfig(ModConfig.Type.SERVER, (server = new Server()).register());
	}
	
	@SubscribeEvent
	public static void onLoad(final ModConfigEvent event)
	{
		switch(event.getConfig().getType())
		{
			case CLIENT:
				client.list.forEach(ConfigResolver::apply);
				break;
			case SERVER:
				server.list.forEach(ConfigResolver::apply);
				break;
			default:
				break;
		}
	}
	
	protected List<ConfigResolver<?>> list = new ArrayList<ConfigResolver<?>>();;
	protected abstract ForgeConfigSpec register();
	
	protected <T> void addValue(ConfigValue<T> object, Consumer<T> consumer)
	{
		list.add(new ConfigResolver<T>(object, consumer));
	}
	
	static class Client extends Config {
		
		public static boolean SHOW_TRANSCRIPTION;
		public static long SAMPLE_LENGTH, SAMPLE_OVERLAP_LENGTH;
		
		@Override
		public ForgeConfigSpec register()
		{
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			builder.push("visuals");
			addValue(builder.comment("Display live transcription").define("show_transcription", true), value -> SHOW_TRANSCRIPTION = value);
			
			// Recording
			builder.pop().comment("Only mess with these settings if you know what you're doing").push("recording");
			addValue(builder.comment("Audio sample length in milliseconds").defineInRange("sample_length", 2000L, 2000, 10000), value -> SAMPLE_LENGTH = value);
			addValue(builder.comment("Number of milliseconds audio samples overlap each other").defineInRange("overlap_length", 500L, 0, 500), value -> SAMPLE_OVERLAP_LENGTH = value);
			
			return builder.build();
		}
	}
	
	static class Server extends Config {
		
		public static Trie TABOO_TREE;
		public static float EXPLOSION_RADIUS;
		public static boolean EXPLOSION_FIRE, EXPLOSION_GRIEFING;
		
		@Override
		public ForgeConfigSpec register()
		{
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			addValue(builder.comment("List of all forbidden words (case-insensitive)").defineListAllowEmpty("taboo", List.of("boom"), element -> true), value -> TABOO_TREE = new Trie(value));
			
			// Punishment
			builder.comment("Shit pertaining to shit").push("punishment");
			addValue(builder.comment("Explosion radius").define("explosion_radius", 5f), value -> EXPLOSION_RADIUS = value);
			addValue(builder.comment("Explosions create fires").define("explosion_fire", true), value -> EXPLOSION_FIRE = value);
			addValue(builder.comment("Explosions break blocks").define("explosion_griefing", true), value -> EXPLOSION_GRIEFING = value);
			
			return builder.build();
		}
	}
	
	private static class ConfigResolver<T> {
		
		private ConfigValue<T> object;
		private Consumer<T> consumer;
		
		private ConfigResolver(ConfigValue<T> object, Consumer<T> consumer)
		{
			this.object = object;
			this.consumer = consumer;
		}
		
		private void apply()
		{
			consumer.accept(object.get());
		}
	}
}
