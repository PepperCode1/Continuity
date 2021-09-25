package me.pepperbell.continuity.client;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.pepperbell.continuity.api.client.CTMLoader;
import me.pepperbell.continuity.api.client.CTMLoaderRegistry;
import me.pepperbell.continuity.api.client.CTMPropertiesFactory;
import me.pepperbell.continuity.client.event.AddBlockStateModelCallback;
import me.pepperbell.continuity.client.event.ModelsAddedCallback;
import me.pepperbell.continuity.client.handler.AddBlockStateModelCallbackHandler;
import me.pepperbell.continuity.client.handler.ClientPlayJoinHandler;
import me.pepperbell.continuity.client.handler.ClientStartedHandler;
import me.pepperbell.continuity.client.handler.ModelsAddedCallbackHandler;
import me.pepperbell.continuity.client.processor.CompactCTMQuadProcessor;
import me.pepperbell.continuity.client.processor.HorizontalQuadProcessor;
import me.pepperbell.continuity.client.processor.HorizontalVerticalQuadProcessor;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.TopQuadProcessor;
import me.pepperbell.continuity.client.processor.VerticalHorizontalQuadProcessor;
import me.pepperbell.continuity.client.processor.VerticalQuadProcessor;
import me.pepperbell.continuity.client.processor.overlay.SimpleOverlayQuadProcessor;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import me.pepperbell.continuity.client.processor.simple.CTMSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.FixedSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.RandomSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.RepeatSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.properties.BaseCTMProperties;
import me.pepperbell.continuity.client.properties.CompactConnectingCTMProperties;
import me.pepperbell.continuity.client.properties.ConnectingCTMProperties;
import me.pepperbell.continuity.client.properties.RandomCTMProperties;
import me.pepperbell.continuity.client.properties.RepeatCTMProperties;
import me.pepperbell.continuity.client.properties.StandardConnectingCTMProperties;
import me.pepperbell.continuity.client.properties.TileAmountValidator;
import me.pepperbell.continuity.client.properties.overlay.BaseOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.RandomOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.RepeatOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.StandardConnectingOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.StandardOverlayCTMProperties;
import me.pepperbell.continuity.client.resource.CTMLoadingContainer;
import me.pepperbell.continuity.client.resource.CustomBlockLayers;
import me.pepperbell.continuity.client.util.biome.BiomeRetriever;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

public class ContinuityClient implements ClientModInitializer {
	public static final String ID = "continuity";
	public static final String NAME = "Continuity";
	public static final Logger LOGGER = LogManager.getLogger(NAME);

	@Override
	public void onInitializeClient() {
		Map<ModelIdentifier, BlockState> modelId2StateMap = new Object2ObjectOpenHashMap<>();
		Map<ModelIdentifier, List<CTMLoadingContainer<?>>> modelId2ContainersMap = new Object2ObjectOpenHashMap<>();
		AddBlockStateModelCallback.EVENT.register(new AddBlockStateModelCallbackHandler(modelId2StateMap, modelId2ContainersMap));
		ModelsAddedCallback.EVENT.register(new ModelsAddedCallbackHandler(modelId2StateMap, modelId2ContainersMap));

		ClientLifecycleEvents.CLIENT_STARTED.register(new ClientStartedHandler());
		ClientPlayConnectionEvents.JOIN.register(new ClientPlayJoinHandler());

		BiomeRetriever.init();
		ProcessingDataKeys.init();
		CustomBlockLayers.init();

		FabricLoader.getInstance().getModContainer(ID).ifPresent(container -> {
			ResourceManagerHelper.registerBuiltinResourcePack(asId("default"), container, ResourcePackActivationType.NORMAL);
			ResourceManagerHelper.registerBuiltinResourcePack(asId("glass_pane_culling_fix"), container, ResourcePackActivationType.NORMAL);
		});

		// Regular methods

		/*
		"ctm" "glass"
		"ctm_compact"
		"horizontal" "bookshelf"
		"vertical"
		"horizontal+vertical" "h+v"
		"vertical+horizontal" "v+h"
		"top"
		"random"
		"repeat"
		"fixed"
		 */

		CTMLoader<?> loader;

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingCTMProperties::new, new TileAmountValidator.AtLeast<>(47)),
				new SimpleQuadProcessor.Factory<>(new CTMSpriteProvider.Factory())
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("ctm", loader);
		CTMLoaderRegistry.INSTANCE.registerLoader("glass", loader);

		loader = CTMLoader.of(
				wrapFactory(CompactConnectingCTMProperties::new, new TileAmountValidator.AtLeast<>(5)),
				new CompactCTMQuadProcessor.Factory()
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("ctm_compact", loader);

		loader = CTMLoader.of(
				wrapFactory(ConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(4)),
				new HorizontalQuadProcessor.Factory()
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("horizontal", loader);
		CTMLoaderRegistry.INSTANCE.registerLoader("bookshelf", loader);

		loader = CTMLoader.of(
				wrapFactory(ConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(4)),
				new VerticalQuadProcessor.Factory()
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("vertical", loader);

		loader = CTMLoader.of(
				wrapFactory(ConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(7)),
				new HorizontalVerticalQuadProcessor.Factory()
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("horizontal+vertical", loader);
		CTMLoaderRegistry.INSTANCE.registerLoader("h+v", loader);

		loader = CTMLoader.of(
				wrapFactory(ConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(7)),
				new VerticalHorizontalQuadProcessor.Factory()
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("vertical+horizontal", loader);
		CTMLoaderRegistry.INSTANCE.registerLoader("v+h", loader);

		loader = CTMLoader.of(
				wrapFactory(ConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(1)),
				new TopQuadProcessor.Factory()
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("top", loader);

		loader = CTMLoader.of(
				wrapFactory(RandomCTMProperties::new),
				new SimpleQuadProcessor.Factory<>(new RandomSpriteProvider.Factory())
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("random", loader);

		loader = CTMLoader.of(
				wrapFactory(RepeatCTMProperties::new, new TileAmountValidator.Repeat<>()),
				new SimpleQuadProcessor.Factory<>(new RepeatSpriteProvider.Factory())
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("repeat", loader);

		loader = CTMLoader.of(
				wrapFactory(BaseCTMProperties::new, new TileAmountValidator.Exactly<>(1)),
				new SimpleQuadProcessor.Factory<>(new FixedSpriteProvider.Factory())
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("fixed", loader);

		// Overlay methods

		/*
		"overlay"
		"overlay_ctm"
		"overlay_random"
		"overlay_repeat"
		"overlay_fixed"
		 */

		loader = CTMLoader.of(
				wrapFactory(StandardOverlayCTMProperties::new, new TileAmountValidator.AtLeast<>(17)),
				new StandardOverlayQuadProcessor.Factory()
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("overlay", loader);

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingOverlayCTMProperties::new, new TileAmountValidator.AtLeast<>(47)),
				new SimpleOverlayQuadProcessor.Factory<>(new CTMSpriteProvider.Factory())
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("overlay_ctm", loader);

		loader = CTMLoader.of(
				wrapFactory(RandomOverlayCTMProperties::new),
				new SimpleOverlayQuadProcessor.Factory<>(new RandomSpriteProvider.Factory())
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("overlay_random", loader);

		loader = CTMLoader.of(
				wrapFactory(RepeatOverlayCTMProperties::new, new TileAmountValidator.Repeat<>()),
				new SimpleOverlayQuadProcessor.Factory<>(new RepeatSpriteProvider.Factory())
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("overlay_repeat", loader);

		loader = CTMLoader.of(
				wrapFactory(BaseOverlayCTMProperties::new, new TileAmountValidator.Exactly<>(1)),
				new SimpleOverlayQuadProcessor.Factory<>(new FixedSpriteProvider.Factory())
		);
		CTMLoaderRegistry.INSTANCE.registerLoader("overlay_fixed", loader);
	}

	private static <T extends BaseCTMProperties> CTMPropertiesFactory<T> wrapFactory(CTMPropertiesFactory<T> factory) {
		return BaseCTMProperties.wrapFactory(factory);
	}

	private static <T extends BaseCTMProperties> CTMPropertiesFactory<T> wrapFactory(CTMPropertiesFactory<T> factory, TileAmountValidator<T> validator) {
		return TileAmountValidator.wrapFactory(wrapFactory(factory), validator);
	}

	public static Identifier asId(String path) {
		return new Identifier(ID, path);
	}
}
