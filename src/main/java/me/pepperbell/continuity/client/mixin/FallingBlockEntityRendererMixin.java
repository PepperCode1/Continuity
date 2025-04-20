package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.pepperbell.continuity.api.client.ContinuityFeatureStates;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;

@Mixin(targets = "net.fabricmc.fabric.mixin.renderer.client.block.render.FallingBlockEntityRendererMixin")
abstract class FallingBlockEntityRendererMixin {
	@Inject(
			method = "render(Lnet/minecraft/client/render/entity/state/FallingBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockModelRenderer;render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BlockStateModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ZJI)V"))
	private void continuity$beforeRenderModel(CallbackInfo ci) {
		ContinuityFeatureStates states = ContinuityFeatureStates.get();
		states.getConnectedTexturesState().disable();
		states.getEmissiveTexturesState().disable();
	}

	@Inject(
			method = "render(Lnet/minecraft/client/render/entity/state/FallingBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockModelRenderer;render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BlockStateModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ZJI)V", shift = At.Shift.AFTER))
	private void continuity$afterRenderModel(CallbackInfo ci) {
		ContinuityFeatureStates states = ContinuityFeatureStates.get();
		states.getConnectedTexturesState().enable();
		states.getEmissiveTexturesState().enable();
	}
}
