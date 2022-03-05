package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;

@Mixin(ReloadableResourceManagerImpl.class)
public interface ReloadableResourceManagerImplAccessor {
	@Accessor("activeManager")
	LifecycledResourceManager getActiveManager();
}
