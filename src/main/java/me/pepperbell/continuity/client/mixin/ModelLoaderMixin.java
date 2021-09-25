package me.pepperbell.continuity.client.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.pepperbell.continuity.client.event.AddBlockStateModelCallback;
import me.pepperbell.continuity.client.event.ModelsAddedCallback;
import me.pepperbell.continuity.client.resource.CTMPropertiesLoader;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

@Mixin(ModelLoader.class)
public class ModelLoaderMixin {
	@Shadow
	@Final
	private Map<Identifier, UnbakedModel> unbakedModels;
	@Shadow
	@Final
	private Map<Identifier, UnbakedModel> modelsToBake;

	@Unique
	private BlockState currentBlockState;

	@Inject(at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=missing_model", shift = At.Shift.BEFORE), method = "<init>(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/client/color/block/BlockColors;Lnet/minecraft/util/profiler/Profiler;I)V")
	private void afterStoreArgs(ResourceManager resourceManager, BlockColors blockColors, Profiler profiler, int mipmap, CallbackInfo ci) {
		BiomeHolderManager.clearCache(); // TODO: move BiomeHolderManager calls elsewhere?
		CTMPropertiesLoader.loadAll(resourceManager);
	}

	@Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/client/color/block/BlockColors;Lnet/minecraft/util/profiler/Profiler;I)V")
	private void onTailInit(ResourceManager resourceManager, BlockColors blockColors, Profiler profiler, int mipmap, CallbackInfo ci) {
		BiomeHolderManager.refreshHolders(); // TODO: move BiomeHolderManager calls elsewhere?
		CTMPropertiesLoader.clearAll();
	}

	@Inject(at = @At("HEAD"), method = "method_4716(Lnet/minecraft/block/BlockState;)V")
	private void onAddBlockStateModel(BlockState state, CallbackInfo ci) {
		currentBlockState = state;
	}

	@Inject(at = @At(value = "TAIL"), method = "addModel(Lnet/minecraft/client/util/ModelIdentifier;)V", locals = LocalCapture.CAPTURE_FAILHARD)
	private void afterAddModel(ModelIdentifier id, CallbackInfo ci, UnbakedModel model) {
		if (currentBlockState != null) {
			AddBlockStateModelCallback.EVENT.invoker().onAddBlockStateModel(id, currentBlockState, model, (ModelLoader) (Object) this);
			currentBlockState = null;
		}
	}

	@Inject(at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=textures"), method = "<init>(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/client/color/block/BlockColors;Lnet/minecraft/util/profiler/Profiler;I)V")
	private void onFinishAddingModels(ResourceManager resourceManager, BlockColors blockColors, Profiler profiler, int mipmap, CallbackInfo ci) {
		ModelsAddedCallback.EVENT.invoker().onModelsAdded((ModelLoader) (Object) this, resourceManager, profiler, unbakedModels, modelsToBake);
	}
}
