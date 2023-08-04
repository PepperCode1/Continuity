package me.pepperbell.continuity.client.processor;

import java.util.function.Consumer;
import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.ProcessingDataKey;
import me.pepperbell.continuity.api.client.ProcessingDataKeyRegistry;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.overlay.SimpleOverlayQuadProcessor;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import net.minecraft.util.math.BlockPos;

public final class ProcessingDataKeys {
	public static final ProcessingDataKey<BlockPos.Mutable> MUTABLE_POS_KEY = create("mutable_pos", BlockPos.Mutable::new);
	public static final ProcessingDataKey<BaseProcessingPredicate.BiomeCache> BIOME_CACHE_KEY = create("biome_cache", BaseProcessingPredicate.BiomeCache::new, BaseProcessingPredicate.BiomeCache::reset);
	public static final ProcessingDataKey<BaseProcessingPredicate.BlockEntityNameCache> BLOCK_ENTITY_NAME_CACHE_KEY = create("block_entity_name_cache", BaseProcessingPredicate.BlockEntityNameCache::new, BaseProcessingPredicate.BlockEntityNameCache::reset);
	public static final ProcessingDataKey<CompactCTMQuadProcessor.VertexContainer> VERTEX_CONTAINER_KEY = create("vertex_container", CompactCTMQuadProcessor.VertexContainer::new);
	public static final ProcessingDataKey<StandardOverlayQuadProcessor.OverlayEmitterPool> STANDARD_OVERLAY_EMITTER_POOL_KEY = create("standard_overlay_emitter_pool", StandardOverlayQuadProcessor.OverlayEmitterPool::new, StandardOverlayQuadProcessor.OverlayEmitterPool::reset);
	public static final ProcessingDataKey<SimpleOverlayQuadProcessor.OverlayEmitterPool> SIMPLE_OVERLAY_EMITTER_POOL_KEY = create("simple_overlay_emitter_pool", SimpleOverlayQuadProcessor.OverlayEmitterPool::new, SimpleOverlayQuadProcessor.OverlayEmitterPool::reset);

	private static <T> ProcessingDataKey<T> create(String id, Supplier<T> valueSupplier) {
		return ProcessingDataKeyRegistry.get().registerKey(ContinuityClient.asId(id), valueSupplier);
	}

	private static <T> ProcessingDataKey<T> create(String id, Supplier<T> valueSupplier, Consumer<T> valueResetAction) {
		return ProcessingDataKeyRegistry.get().registerKey(ContinuityClient.asId(id), valueSupplier, valueResetAction);
	}

	public static void init() {
	}
}
