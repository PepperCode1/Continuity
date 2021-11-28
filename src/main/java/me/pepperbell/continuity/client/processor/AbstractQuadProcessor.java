package me.pepperbell.continuity.client.processor;

import java.util.Random;
import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.QuadProcessor;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public abstract class AbstractQuadProcessor implements QuadProcessor {
	protected Sprite[] sprites;
	protected ProcessingPredicate processingPredicate;

	public AbstractQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate) {
		this.sprites = sprites;
		this.processingPredicate = processingPredicate;
	}

	@Override
	public ProcessingResult processQuad(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, int processorIndex, ProcessingContext context) {
		if (!processingPredicate.shouldProcessQuad(quad, sprite, blockView, state, pos, context)) {
			return ProcessingResult.CONTINUE;
		}
		return processQuadInner(quad, sprite, blockView, state, pos, randomSupplier, pass, processorIndex, context);
	}

	public abstract ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, int processorIndex, ProcessingContext context);
}
