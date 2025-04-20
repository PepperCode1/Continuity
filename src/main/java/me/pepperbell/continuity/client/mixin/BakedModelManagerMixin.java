package me.pepperbell.continuity.client.mixin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.pepperbell.continuity.client.resource.BakedModelManagerBakeContext;
import me.pepperbell.continuity.client.resource.BakedModelManagerReloadExtension;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.LoadedBlockEntityModels;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

@Mixin(BakedModelManager.class)
abstract class BakedModelManagerMixin {
	@Unique
	@Nullable
	private volatile BakedModelManagerReloadExtension continuity$reloadExtension;

	@Inject(method = "reload(Lnet/minecraft/resource/ResourceReloader$Synchronizer;Lnet/minecraft/resource/ResourceManager;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
	private void continuity$onHeadReload(ResourceReloader.Synchronizer synchronizer, ResourceManager resourceManager, Executor prepareExecutor, Executor applyExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		continuity$reloadExtension = new BakedModelManagerReloadExtension(resourceManager, prepareExecutor);

		BakedModelManagerReloadExtension reloadExtension = continuity$reloadExtension;
		if (reloadExtension != null) {
			reloadExtension.setContext();
		}
	}

	@Inject(method = "reload(Lnet/minecraft/resource/ResourceReloader$Synchronizer;Lnet/minecraft/resource/ResourceManager;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
	private void continuity$onReturnReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		BakedModelManagerReloadExtension reloadExtension = continuity$reloadExtension;
		if (reloadExtension != null) {
			reloadExtension.clearContext();
		}
	}

	@ModifyReturnValue(method = "reload(Lnet/minecraft/resource/ResourceReloader$Synchronizer;Lnet/minecraft/resource/ResourceManager;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
	private CompletableFuture<Void> continuity$modifyReturnReload(CompletableFuture<Void> original) {
		return original.thenRun(() -> continuity$reloadExtension = null);
	}

	@ModifyArg(
			method = "reload(Lnet/minecraft/resource/ResourceReloader$Synchronizer;Lnet/minecraft/resource/ResourceManager;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
			slice = @Slice(
					from = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/SpriteAtlasManager;reload(Lnet/minecraft/resource/ResourceManager;ILjava/util/concurrent/Executor;)Ljava/util/Map;")),
			at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenComposeAsync(Ljava/util/function/Function;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", ordinal = 0),
			index = 0)
	private Function<Void, Object> continuity$modifyFunction(Function<Void, Object> function) {
		BakedModelManagerReloadExtension reloadExtension = continuity$reloadExtension;
		if (reloadExtension != null) {
			return v -> {
				BakedModelManagerBakeContext.THREAD_LOCAL.set(reloadExtension);
				Object result = function.apply(v);
				BakedModelManagerBakeContext.THREAD_LOCAL.remove();
				return result;
			};
		}
		return function;
	}

	@Inject(method = "bake", at = @At("HEAD"))
	private static void continuity$onHeadBake(final Map<Identifier, SpriteAtlasManager.AtlasPreparation> atlases, ModelBaker baker, Object2IntMap<BlockState> blockStates, LoadedEntityModels entityModels, LoadedBlockEntityModels blockEntityModels, Executor executor, CallbackInfoReturnable<Object> cir) {
		BakedModelManagerBakeContext context = BakedModelManagerBakeContext.THREAD_LOCAL.get();
		if (context != null) {
			context.beforeBake(atlases);
		}
	}

	@Inject(method = "upload(Lnet/minecraft/client/render/model/BakedModelManager$BakingResult;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("RETURN"))
	private void continuity$onReturnUpload(CallbackInfo ci) {
		BakedModelManagerReloadExtension reloadExtension = continuity$reloadExtension;
		if (reloadExtension != null) {
			reloadExtension.apply();
		}
	}
}
