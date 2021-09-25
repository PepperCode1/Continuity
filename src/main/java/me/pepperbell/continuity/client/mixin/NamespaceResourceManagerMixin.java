package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.util.InvalidIdentifierHandler;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.util.Identifier;

@Mixin(NamespaceResourceManager.class)
public class NamespaceResourceManagerMixin {
	@Inject(at = @At("HEAD"), method = "getMetadataPath(Lnet/minecraft/util/Identifier;)Lnet/minecraft/util/Identifier;")
	private static void onHeadGetMetadataPath(Identifier id, CallbackInfoReturnable<Identifier> cir) {
		InvalidIdentifierHandler.enableInvalidPaths();
	}

	@Inject(at = @At("TAIL"), method = "getMetadataPath(Lnet/minecraft/util/Identifier;)Lnet/minecraft/util/Identifier;")
	private static void onTailGetMetadataPath(Identifier id, CallbackInfoReturnable<Identifier> cir) {
		InvalidIdentifierHandler.disableInvalidPaths();
	}
}
