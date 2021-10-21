package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.pepperbell.continuity.client.util.SpriteCalculator;
import net.minecraft.client.render.block.BlockModels;

@Mixin(BlockModels.class)
public class BlockModelsMixin {
	@Inject(method = "reload()V", at = @At("HEAD"))
	private void onHeadReload(CallbackInfo ci) {
		SpriteCalculator.clearCache();
	}
}
