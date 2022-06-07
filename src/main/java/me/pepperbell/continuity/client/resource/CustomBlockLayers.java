package me.pepperbell.continuity.client.resource;

import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Predicate;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.properties.PropertiesParsingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class CustomBlockLayers {
	public static final Identifier LOCATION = new Identifier("optifine/block.properties");

	@SuppressWarnings("unchecked")
	private static final Predicate<BlockState>[] EMPTY_LAYER_PREDICATES = new Predicate[BlockLayer.VALUES.length];

	@SuppressWarnings("unchecked")
	private static final Predicate<BlockState>[] LAYER_PREDICATES = new Predicate[BlockLayer.VALUES.length];

	@Nullable
	public static RenderLayer getLayer(BlockState state) {
		for (int i = 0; i < BlockLayer.VALUES.length; i++) {
			Predicate<BlockState> predicate = LAYER_PREDICATES[i];
			if (predicate != null) {
				if (predicate.test(state)) {
					return BlockLayer.VALUES[i].getLayer();
				}
			}
		}
		return null;
	}

	private static void reload(ResourceManager manager) {
		System.arraycopy(EMPTY_LAYER_PREDICATES, 0, LAYER_PREDICATES, 0, EMPTY_LAYER_PREDICATES.length);
		try (Resource resource = manager.getResource(LOCATION)) {
			Properties properties = new Properties();
			properties.load(resource.getInputStream());
			reload(properties, resource.getId(), resource.getResourcePackName());
		} catch (FileNotFoundException e) {
			//
		} catch (Exception e) {
			ContinuityClient.LOGGER.error("Failed to load custom block layers from file '" + LOCATION + "'", e);
		}
	}

	private static void reload(Properties properties, Identifier fileLocation, String packName) {
		for (BlockLayer blockLayer : BlockLayer.VALUES) {
			String propertyKey = "layer." + blockLayer.getKey();
			Predicate<BlockState> predicate = PropertiesParsingHelper.parseBlockStates(properties, propertyKey, fileLocation, packName, true);
			LAYER_PREDICATES[blockLayer.ordinal()] = predicate;
		}
	}

	public static class ReloadListener implements SimpleSynchronousResourceReloadListener {
		public static final Identifier ID = ContinuityClient.asId("custom_block_layers");
		private static final ReloadListener INSTANCE = new ReloadListener();

		@ApiStatus.Internal
		public static void init() {
			ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
		}

		@Override
		public void reload(ResourceManager manager) {
			CustomBlockLayers.reload(manager);
		}

		@Override
		public Identifier getFabricId() {
			return ID;
		}
	}

	private enum BlockLayer {
		SOLID(RenderLayer.getSolid()),
		CUTOUT(RenderLayer.getCutout()),
		CUTOUT_MIPPED(RenderLayer.getCutoutMipped()),
		TRANSLUCENT(RenderLayer.getTranslucent());

		public static final BlockLayer[] VALUES = values();

		private final RenderLayer layer;
		private final String key;

		BlockLayer(RenderLayer layer) {
			this.layer = layer;
			key = name().toLowerCase(Locale.ROOT);
		}

		public RenderLayer getLayer() {
			return layer;
		}

		public String getKey() {
			return key;
		}
	}
}
