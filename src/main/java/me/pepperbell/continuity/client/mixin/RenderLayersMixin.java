package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.resource.CustomBlockLayers;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;

@Mixin(RenderLayers.class)
public class RenderLayersMixin {
	@Inject(method = "getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/RenderLayer;", at = @At("HEAD"), cancellable = true)
	private static void continuity$onHeadGetBlockLayer(BlockState state, CallbackInfoReturnable<RenderLayer> cir) {
		if (!CustomBlockLayers.isEmpty() && ContinuityConfig.INSTANCE.customBlockLayers.get()) {
			RenderLayer layer = CustomBlockLayers.getLayer(state);
			if (layer != null) {
				cir.setReturnValue(layer);
			}
		}
	}

	@Inject(method = "getMovingBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/RenderLayer;", at = @At("HEAD"), cancellable = true)
	private static void continuity$onHeadGetMovingBlockLayer(BlockState state, CallbackInfoReturnable<RenderLayer> cir) {
		if (!CustomBlockLayers.isEmpty() && ContinuityConfig.INSTANCE.customBlockLayers.get()) {
			RenderLayer layer = CustomBlockLayers.getLayer(state);
			if (layer != null) {
				cir.setReturnValue(layer == RenderLayer.getTranslucent() ? RenderLayer.getTranslucentMovingBlock() : layer);
			}
		}
	}
}
