package me.pepperbell.continuity.client.model;

import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class EmissiveBakedModel extends ForwardingBakedModel {
	protected static final RenderMaterial DEFAULT_EMISSIVE_MATERIAL = RenderUtil.getMaterialFinder().emissive(0, true).disableDiffuse(0, true).disableAo(0, true).find();
	protected static final RenderMaterial CUTOUT_MIPPED_EMISSIVE_MATERIAL = RenderUtil.getMaterialFinder().emissive(0, true).disableDiffuse(0, true).disableAo(0, true).blendMode(0, BlendMode.CUTOUT_MIPPED).find();

	public EmissiveBakedModel(BakedModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getConnectedTexturesState().isEnabled()) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		EmissiveBlockQuadTransform quadTransform = container.emissiveBlockQuadTransform;
		if (quadTransform.isActive()) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		MeshBuilder meshBuilder = container.meshBuilder;
		quadTransform.prepare(meshBuilder.getEmitter(), blockView, state, pos, ContinuityConfig.INSTANCE.useManualCulling.get());

		context.pushTransform(quadTransform);
		super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
		context.popTransform();

		if (quadTransform.didEmit()) {
			context.meshConsumer().accept(meshBuilder.build());
		}
		quadTransform.reset();
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			super.emitItemQuads(stack, randomSupplier, context);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			super.emitItemQuads(stack, randomSupplier, context);
			return;
		}

		EmissiveItemQuadTransform quadTransform = container.emissiveItemQuadTransform;
		if (quadTransform.isActive()) {
			super.emitItemQuads(stack, randomSupplier, context);
			return;
		}

		MeshBuilder meshBuilder = container.meshBuilder;
		quadTransform.prepare(meshBuilder.getEmitter());

		context.pushTransform(quadTransform);
		super.emitItemQuads(stack, randomSupplier, context);
		context.popTransform();

		if (quadTransform.didEmit()) {
			context.meshConsumer().accept(meshBuilder.build());
		}
		quadTransform.reset();
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	protected static class EmissiveBlockQuadTransform implements RenderContext.QuadTransform {
		protected final CullingCache cullingCache = new CullingCache();

		protected QuadEmitter emitter;
		protected BlockRenderView blockView;
		protected BlockState state;
		protected BlockPos pos;
		protected boolean useManualCulling;

		protected boolean active;
		protected boolean didEmit;
		protected boolean wasDefaultLayerCalculated;
		protected boolean isDefaultLayerSolid;

		@Override
		public boolean transform(MutableQuadView quad) {
			if (useManualCulling && cullingCache.shouldCull(quad, blockView, pos, state)) {
				return false;
			}

			Sprite sprite = RenderUtil.getSpriteFinder().find(quad, 0);
			Sprite emissiveSprite = EmissiveSpriteApi.get().getEmissiveSprite(sprite);
			if (emissiveSprite != null) {
				quad.copyTo(emitter);

				BlendMode blendMode = RenderUtil.getBlendMode(quad);
				boolean isSolid = false;
				if (blendMode == BlendMode.DEFAULT) {
					if (!wasDefaultLayerCalculated) {
						wasDefaultLayerCalculated = true;
						isDefaultLayerSolid = RenderLayers.getBlockLayer(state) == RenderLayer.getSolid();
					}
					isSolid = isDefaultLayerSolid;
				} else if (blendMode == BlendMode.SOLID) {
					isSolid = true;
				}
				RenderMaterial emissiveMaterial = isSolid ? CUTOUT_MIPPED_EMISSIVE_MATERIAL : DEFAULT_EMISSIVE_MATERIAL;

				emitter.material(emissiveMaterial);
				QuadUtil.interpolate(emitter, sprite, emissiveSprite);
				emitter.emit();
				didEmit = true;
			}
			return true;
		}

		public boolean isActive() {
			return active;
		}

		public boolean didEmit() {
			return didEmit;
		}

		public void prepare(QuadEmitter emitter, BlockRenderView blockView, BlockState state, BlockPos pos, boolean useManualCulling) {
			this.emitter = emitter;
			this.blockView = blockView;
			this.state = state;
			this.pos = pos;
			this.useManualCulling = useManualCulling;

			active = true;
			didEmit = false;
			wasDefaultLayerCalculated = false;
			isDefaultLayerSolid = false;

			cullingCache.prepare();
		}

		public void reset() {
			emitter = null;
			blockView = null;
			state = null;
			pos = null;
			useManualCulling = false;

			active = false;
		}
	}

	protected static class EmissiveItemQuadTransform implements RenderContext.QuadTransform {
		protected QuadEmitter emitter;

		protected boolean active;
		protected boolean didEmit;

		@Override
		public boolean transform(MutableQuadView quad) {
			Sprite sprite = RenderUtil.getSpriteFinder().find(quad, 0);
			Sprite emissiveSprite = EmissiveSpriteApi.get().getEmissiveSprite(sprite);
			if (emissiveSprite != null) {
				quad.copyTo(emitter);
				emitter.material(DEFAULT_EMISSIVE_MATERIAL);
				QuadUtil.interpolate(emitter, sprite, emissiveSprite);
				emitter.emit();
				didEmit = true;
			}
			return true;
		}

		public boolean isActive() {
			return active;
		}

		public boolean didEmit() {
			return didEmit;
		}

		public void prepare(QuadEmitter emitter) {
			this.emitter = emitter;

			active = true;
			didEmit = false;
		}

		public void reset() {
			active = false;
			emitter = null;
		}
	}
}
