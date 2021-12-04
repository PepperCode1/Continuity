package me.pepperbell.continuity.client.processor.simple;

import java.util.Random;
import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.AbstractQuadProcessorFactory;
import me.pepperbell.continuity.client.processor.BaseProcessingPredicate;
import me.pepperbell.continuity.client.processor.ProcessingPredicate;
import me.pepperbell.continuity.client.properties.BaseCTMProperties;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.TextureUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class SimpleQuadProcessor implements QuadProcessor {
	protected SpriteProvider spriteProvider;
	protected ProcessingPredicate processingPredicate;

	public SimpleQuadProcessor(SpriteProvider spriteProvider, ProcessingPredicate processingPredicate) {
		this.spriteProvider = spriteProvider;
		this.processingPredicate = processingPredicate;
	}

	@Override
	public ProcessingResult processQuad(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, int processorIndex, ProcessingContext context) {
		if (!processingPredicate.shouldProcessQuad(quad, sprite, blockView, state, pos, context)) {
			return ProcessingResult.CONTINUE;
		}
		Sprite newSprite = spriteProvider.getSprite(quad, sprite, blockView, state, pos, randomSupplier, context);
		return process(quad, sprite, newSprite);
	}

	// TODO: rename? move?
	public static ProcessingResult process(MutableQuadView quad, Sprite oldSprite, Sprite newSprite) {
		if (newSprite == null) {
			return ProcessingResult.ABORT_AND_RENDER_QUAD;
		}
		if (TextureUtil.isMissingSprite(newSprite)) {
			return ProcessingResult.CONTINUE;
		}
		QuadUtil.interpolate(quad, oldSprite, newSprite);
		return ProcessingResult.STOP;
	}

	public static class Factory<T extends BaseCTMProperties> extends AbstractQuadProcessorFactory<T> {
		protected SpriteProvider.Factory<? super T> spriteProviderFactory;

		public Factory(SpriteProvider.Factory<? super T> spriteProviderFactory) {
			this.spriteProviderFactory = spriteProviderFactory;
		}

		@Override
		public QuadProcessor createProcessor(T properties, Sprite[] sprites) {
			return new SimpleQuadProcessor(spriteProviderFactory.createSpriteProvider(sprites, properties), BaseProcessingPredicate.fromProperties(properties));
		}

		@Override
		public int getTextureAmount(T properties) {
			return spriteProviderFactory.getTextureAmount(properties);
		}
	}
}
