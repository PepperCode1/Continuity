package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.resource.InvalidIdentifierStateHolder;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.util.Identifier;

@Mixin(NamespaceResourceManager.class)
public class NamespaceResourceManagerMixin {
	@Inject(method = "getMetadataPath(Lnet/minecraft/util/Identifier;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"))
	private static void continuity$onHeadGetMetadataPath(Identifier id, CallbackInfoReturnable<Identifier> cir) {
		InvalidIdentifierStateHolder.get().enable();
	}

	@Inject(method = "getMetadataPath(Lnet/minecraft/util/Identifier;)Lnet/minecraft/util/Identifier;", at = @At("TAIL"))
	private static void continuity$onTailGetMetadataPath(Identifier id, CallbackInfoReturnable<Identifier> cir) {
		InvalidIdentifierStateHolder.get().disable();
	}
}
