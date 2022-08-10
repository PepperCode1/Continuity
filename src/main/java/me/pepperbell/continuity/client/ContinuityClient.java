package me.pepperbell.continuity.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.pepperbell.continuity.api.client.CTMLoader;
import me.pepperbell.continuity.api.client.CTMLoaderRegistry;
import me.pepperbell.continuity.api.client.CTMPropertiesFactory;
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
import me.pepperbell.continuity.client.properties.RandomCTMProperties;
import me.pepperbell.continuity.client.properties.RepeatCTMProperties;
import me.pepperbell.continuity.client.properties.StandardConnectingCTMProperties;
import me.pepperbell.continuity.client.properties.TileAmountValidator;
import me.pepperbell.continuity.client.properties.overlay.BaseOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.RandomOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.RepeatOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.StandardConnectingOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.StandardOverlayCTMProperties;
import me.pepperbell.continuity.client.resource.CustomBlockLayers;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import me.pepperbell.continuity.client.util.biome.BiomeRetriever;
import me.pepperbell.continuity.impl.client.ProcessingDataKeyRegistryImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class ContinuityClient implements ClientModInitializer {
	public static final String ID = "continuity";
	public static final String NAME = "Continuity";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	@Override
	public void onInitializeClient() {
		ProcessingDataKeyRegistryImpl.INSTANCE.init();
		BiomeHolderManager.init();
		BiomeRetriever.init();
		ProcessingDataKeys.init();
		RenderUtil.ReloadListener.init();
		CustomBlockLayers.ReloadListener.init();

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

		CTMLoaderRegistry registry = CTMLoaderRegistry.get();
		CTMLoader<?> loader;

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingCTMProperties::new, new TileAmountValidator.AtLeast<>(47)),
				new SimpleQuadProcessor.Factory<>(new CTMSpriteProvider.Factory(true))
		);
		registry.registerLoader("ctm", loader);
		registry.registerLoader("glass", loader);

		loader = CTMLoader.of(
				wrapFactory(CompactConnectingCTMProperties::new, new TileAmountValidator.AtLeast<>(5)),
				new CompactCTMQuadProcessor.Factory()
		);
		registry.registerLoader("ctm_compact", loader);

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(4)),
				new HorizontalQuadProcessor.Factory()
		);
		registry.registerLoader("horizontal", loader);
		registry.registerLoader("bookshelf", loader);

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(4)),
				new VerticalQuadProcessor.Factory()
		);
		registry.registerLoader("vertical", loader);

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(7)),
				new HorizontalVerticalQuadProcessor.Factory()
		);
		registry.registerLoader("horizontal+vertical", loader);
		registry.registerLoader("h+v", loader);

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(7)),
				new VerticalHorizontalQuadProcessor.Factory()
		);
		registry.registerLoader("vertical+horizontal", loader);
		registry.registerLoader("v+h", loader);

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingCTMProperties::new, new TileAmountValidator.Exactly<>(1)),
				new TopQuadProcessor.Factory()
		);
		registry.registerLoader("top", loader);

		loader = CTMLoader.of(
				wrapFactory(RandomCTMProperties::new),
				new SimpleQuadProcessor.Factory<>(new RandomSpriteProvider.Factory())
		);
		registry.registerLoader("random", loader);

		loader = CTMLoader.of(
				wrapFactory(RepeatCTMProperties::new, new RepeatCTMProperties.Validator<>()),
				new SimpleQuadProcessor.Factory<>(new RepeatSpriteProvider.Factory())
		);
		registry.registerLoader("repeat", loader);

		loader = CTMLoader.of(
				wrapFactory(BaseCTMProperties::new, new TileAmountValidator.Exactly<>(1)),
				new SimpleQuadProcessor.Factory<>(new FixedSpriteProvider.Factory())
		);
		registry.registerLoader("fixed", loader);

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
		registry.registerLoader("overlay", loader);

		loader = CTMLoader.of(
				wrapFactory(StandardConnectingOverlayCTMProperties::new, new TileAmountValidator.AtLeast<>(47)),
				new SimpleOverlayQuadProcessor.Factory<>(new CTMSpriteProvider.Factory(false))
		);
		registry.registerLoader("overlay_ctm", loader);

		loader = CTMLoader.of(
				wrapFactory(RandomOverlayCTMProperties::new),
				new SimpleOverlayQuadProcessor.Factory<>(new RandomSpriteProvider.Factory())
		);
		registry.registerLoader("overlay_random", loader);

		loader = CTMLoader.of(
				wrapFactory(RepeatOverlayCTMProperties::new, new RepeatCTMProperties.Validator<>()),
				new SimpleOverlayQuadProcessor.Factory<>(new RepeatSpriteProvider.Factory())
		);
		registry.registerLoader("overlay_repeat", loader);

		loader = CTMLoader.of(
				wrapFactory(BaseOverlayCTMProperties::new, new TileAmountValidator.Exactly<>(1)),
				new SimpleOverlayQuadProcessor.Factory<>(new FixedSpriteProvider.Factory())
		);
		registry.registerLoader("overlay_fixed", loader);
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
