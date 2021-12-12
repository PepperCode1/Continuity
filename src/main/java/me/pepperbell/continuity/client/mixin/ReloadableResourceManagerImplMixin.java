package me.pepperbell.continuity.client.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.pepperbell.continuity.client.mixinterface.ReloadableResourceManagerImplExtension;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

@Mixin(ReloadableResourceManagerImpl.class)
public class ReloadableResourceManagerImplMixin implements ReloadableResourceManagerImplExtension {
	@Unique
	private static final Map<Identifier, Identifier> redirects = new Object2ObjectOpenHashMap<>();

	@Override
	public void addRedirect(Identifier from, Identifier to) {
		redirects.put(from, to);
	}

	@Unique
	private Identifier redirect(Identifier id) {
		Identifier redirect = redirects.get(id);
		if (redirect != null) {
			return redirect;
		}
		return id;
	}

	@ModifyVariable(method = "getResource(Lnet/minecraft/util/Identifier;)Lnet/minecraft/resource/Resource;", at = @At("HEAD"))
	private Identifier redirectGetResourceId(Identifier id) {
		return redirect(id);
	}

	@ModifyVariable(method = "containsResource(Lnet/minecraft/util/Identifier;)Z", at = @At("HEAD"))
	private Identifier redirectContainsResourceId(Identifier id) {
		return redirect(id);
	}

	@ModifyVariable(method = "getAllResources(Lnet/minecraft/util/Identifier;)Ljava/util/List;", at = @At("HEAD"))
	private Identifier redirectGetAllResourcesId(Identifier id) {
		return redirect(id);
	}

	// TODO: what if redirect doesn't actually exist?
//	@Inject(method = "findResources(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Collection;", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;", shift = At.Shift.BY, by = 2), locals = LocalCapture.CAPTURE_FAILHARD)
//	private void onFindResources(String startingPath, Predicate<String> pathPredicate, CallbackInfoReturnable<Collection<Identifier>> cir, Set<Identifier> set) {
//		for (Identifier id : redirects.keySet()) {
//			String path = id.getPath();
//			if (path.startsWith(startingPath) && pathPredicate.test(path)) {
//				set.add(id);
//			}
//		}
//	}

	@Inject(method = "reload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/minecraft/resource/ResourceReload;", at = @At("HEAD"))
	private void onHeadReload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<ResourcePack> packs, CallbackInfoReturnable<ResourceReload> cir) {
		redirects.clear();
	}
}
