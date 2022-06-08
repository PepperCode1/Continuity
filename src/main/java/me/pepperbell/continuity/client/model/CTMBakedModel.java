package me.pepperbell.continuity.client.model;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class CTMBakedModel extends ForwardingBakedModel {
	public static final int MULTIPASS_LIMIT = 3;

	protected final List<QuadProcessor> processors;
	@Nullable
	protected final List<QuadProcessor> multipassProcessors;

	public CTMBakedModel(BakedModel wrapped, List<QuadProcessor> processors, @Nullable List<QuadProcessor> multipassProcessors) {
		this.wrapped = wrapped;
		this.processors = processors;
		this.multipassProcessors = multipassProcessors;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		if (!ContinuityConfig.INSTANCE.connectedTextures.get()) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getConnectedTexturesState().isEnabled()) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		CTMQuadTransform quadTransform = container.ctmQuadTransform;
		if (quadTransform.isActive()) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		quadTransform.prepare(processors, multipassProcessors, blockView, state, pos, randomSupplier, ContinuityConfig.INSTANCE.useManualCulling.get());

		context.pushTransform(quadTransform);
		super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
		context.popTransform();

		quadTransform.processingContext.accept(context);
		quadTransform.reset();
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	protected static class CTMQuadTransform implements RenderContext.QuadTransform {
		protected final ProcessingContextImpl processingContext = new ProcessingContextImpl();
		protected final CullingCache cullingCache = new CullingCache();

		protected List<QuadProcessor> processors;
		protected List<QuadProcessor> multipassProcessors;
		protected BlockRenderView blockView;
		protected BlockState state;
		protected BlockPos pos;
		protected Supplier<Random> randomSupplier;
		protected boolean useManualCulling;

		protected boolean active;

		@Override
		public boolean transform(MutableQuadView quad) {
			if (useManualCulling && cullingCache.shouldCull(quad, blockView, pos, state)) {
				return false;
			}

			Boolean result = transformOnce(quad, processors, 0);
			if (result != null) {
				return result;
			}
			if (multipassProcessors != null) {
				for (int pass = 0; pass < MULTIPASS_LIMIT; pass++) {
					result = transformOnce(quad, multipassProcessors, pass + 1);
					if (result != null) {
						return result;
					}
				}
			}

			return true;
		}

		protected Boolean transformOnce(MutableQuadView quad, List<QuadProcessor> processors, int pass) {
			Sprite sprite = RenderUtil.getSpriteFinder().find(quad, 0);
			int amount = processors.size();
			for (int i = 0; i < amount; i++) {
				QuadProcessor processor = processors.get(i);
				QuadProcessor.ProcessingResult result = processor.processQuad(quad, sprite, blockView, state, pos, randomSupplier, pass, i, processingContext);
				if (result == QuadProcessor.ProcessingResult.CONTINUE) {
					continue;
				}
				if (result == QuadProcessor.ProcessingResult.STOP) {
					return null;
				}
				if (result == QuadProcessor.ProcessingResult.ABORT_AND_CANCEL_QUAD) {
					return false;
				}
				if (result == QuadProcessor.ProcessingResult.ABORT_AND_RENDER_QUAD) {
					return true;
				}
			}
			return true;
		}

		public boolean isActive() {
			return active;
		}

		public void prepare(List<QuadProcessor> processors, List<QuadProcessor> multipassProcessors, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, boolean useManualCulling) {
			this.processors = processors;
			this.multipassProcessors = multipassProcessors;
			this.blockView = blockView;
			this.state = state;
			this.pos = pos;
			this.randomSupplier = randomSupplier;
			this.useManualCulling = useManualCulling;

			active = true;

			processingContext.prepare();
			cullingCache.prepare();
		}

		public void reset() {
			processors = null;
			multipassProcessors = null;
			blockView = null;
			state = null;
			pos = null;
			randomSupplier = null;
			useManualCulling = false;

			active = false;

			processingContext.reset();
		}
	}
}
