package me.pepperbell.continuity.client.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.pepperbell.continuity.client.mixinterface.LifecycledResourceManagerImplExtension;
import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

@Mixin(LifecycledResourceManagerImpl.class)
public class LifecycledResourceManagerImplMixin implements LifecycledResourceManagerImplExtension {
	@Unique
	private ResourceRedirectHandler redirectHandler;

	@Inject(method = "<init>(Lnet/minecraft/resource/ResourceType;Ljava/util/List;)V", at = @At("TAIL"))
	private void onTailInit(ResourceType type, List<ResourcePack> packs, CallbackInfo ci) {
		if (type == ResourceType.CLIENT_RESOURCES) {
			redirectHandler = new ResourceRedirectHandler();
		}
	}

	@ModifyVariable(method = "getResource(Lnet/minecraft/util/Identifier;)Lnet/minecraft/resource/Resource;", at = @At("HEAD"))
	private Identifier redirectGetResourceId(Identifier id) {
		if (redirectHandler != null) {
			return redirectHandler.redirect(id);
		}
		return id;
	}

	@ModifyVariable(method = "containsResource(Lnet/minecraft/util/Identifier;)Z", at = @At("HEAD"))
	private Identifier redirectContainsResourceId(Identifier id) {
		if (redirectHandler != null) {
			return redirectHandler.redirect(id);
		}
		return id;
	}

	@ModifyVariable(method = "getAllResources(Lnet/minecraft/util/Identifier;)Ljava/util/List;", at = @At("HEAD"))
	private Identifier redirectGetAllResourcesId(Identifier id) {
		if (redirectHandler != null) {
			return redirectHandler.redirect(id);
		}
		return id;
	}

	@Override
	@Nullable
	public ResourceRedirectHandler getRedirectHandler() {
		return redirectHandler;
	}
}
