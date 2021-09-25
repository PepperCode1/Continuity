package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.util.InvalidIdentifierHandler;
import net.minecraft.util.Identifier;

@Mixin(Identifier.class)
public class IdentifierMixin {
	@Inject(at = @At("HEAD"), method = "isPathValid(Ljava/lang/String;)Z", cancellable = true)
	private static void onIsPathValid(String path, CallbackInfoReturnable<Boolean> cir) {
		if (InvalidIdentifierHandler.areInvalidPathsEnabled()) {
			cir.setReturnValue(true);
		}
	}
}
