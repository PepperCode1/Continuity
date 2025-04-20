package me.pepperbell.continuity.client.mixin;

import net.fabricmc.fabric.api.renderer.v1.render.RenderLayerHelper;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.FallingBlockEntityRenderState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.FallingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import me.pepperbell.continuity.api.client.ContinuityFeatureStates;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;


@Mixin(value = FallingBlockEntityRenderer.class,
		priority = 900 // Higher than Fabric-API (see overwrite below)
)
public abstract class FallingBlockEntityRendererMixin extends EntityRenderer<FallingBlockEntity, FallingBlockEntityRenderState> {
	@Unique
	private void continuity$beforeRenderModel() {
		ContinuityFeatureStates states = ContinuityFeatureStates.get();
		states.getConnectedTexturesState().disable();
		states.getEmissiveTexturesState().disable();
	}

	@Unique
	private void continuity$afterRenderModel() {
		ContinuityFeatureStates states = ContinuityFeatureStates.get();
		states.getConnectedTexturesState().enable();
		states.getEmissiveTexturesState().enable();
	}

	@Shadow
	@Final
	private BlockRenderManager blockRenderManager;

	private FallingBlockEntityRendererMixin(EntityRendererFactory.Context context) {
		super(context);
	}

	/**
	 * @reason Fabric-API also brings a FallingBlockEntityRendererMixin that uses @Overwrite
	 * which results in conflicts.
	 * Until this is fixed by Fabric we need to overwrite it here.
	 * @author Continuity
	 */
	@Overwrite
	public void render(FallingBlockEntityRenderState renderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
		BlockState blockState = renderState.blockState;

		if (blockState.getRenderType() == BlockRenderType.MODEL) {
			matrixStack.push();
			matrixStack.translate(-0.5, 0.0, -0.5);

			BlockStateModel model = blockRenderManager.getModel(blockState);
			long seed = blockState.getRenderingSeed(renderState.fallingBlockPos);
			continuity$beforeRenderModel(); // Inserted
			blockRenderManager.getModelRenderer().render(renderState, model, blockState, renderState.currentPos, matrixStack, layer -> vertexConsumers.getBuffer(RenderLayerHelper.getMovingBlockLayer(layer)), false, seed, OverlayTexture.DEFAULT_UV);
			continuity$afterRenderModel();  // Inserted

			matrixStack.pop();
			super.render(renderState, matrixStack, vertexConsumers, light);
		}
	}
}
