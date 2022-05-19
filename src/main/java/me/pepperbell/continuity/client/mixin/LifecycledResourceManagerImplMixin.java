package me.pepperbell.continuity.client.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.pepperbell.continuity.client.mixinterface.LifecycledResourceManagerImplExtension;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.util.Identifier;

@Mixin(LifecycledResourceManagerImpl.class)
public class LifecycledResourceManagerImplMixin implements LifecycledResourceManagerImplExtension {
	@Unique
	private final Map<Identifier, Identifier> redirects = new Object2ObjectOpenHashMap<>();

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

	@ModifyVariable(method = "getResource", at = @At("HEAD"))
	private Identifier redirectGetResourceId(Identifier id) {
		return redirect(id);
	}

	//@ModifyVariable(method = "", at = @At("HEAD"))
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
}
