package me.pepperbell.continuity.client.mixin;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.client.mixinterface.SpriteAtlasTextureDataExtension;
import me.pepperbell.continuity.client.mixinterface.SpriteExtension;
import me.pepperbell.continuity.client.resource.EmissiveSuffixLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

@Mixin(SpriteAtlasTexture.class)
public class SpriteAtlasTextureMixin {
	@Shadow
	@Final
	private Map<Identifier, Sprite> sprites;

	@Unique
	private boolean loadingEmissiveSprites;
	@Unique
	private Map<Identifier, Identifier> emissiveIdMap;

	@Shadow
	private Collection<Sprite.Info> loadSprites(ResourceManager resourceManager, Set<Identifier> ids) {
		return null;
	}

	@Shadow
	private Identifier getTexturePath(Identifier id) {
		return null;
	}

	@Inject(method = "loadSprites(Lnet/minecraft/resource/ResourceManager;Ljava/util/Set;)Ljava/util/Collection;", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void onTailLoadSprites(ResourceManager resourceManager, Set<Identifier> ids, CallbackInfoReturnable<Collection<Sprite.Info>> cir) {
		if (!loadingEmissiveSprites) {
			loadingEmissiveSprites = true;
			String emissiveSuffix = EmissiveSuffixLoader.getEmissiveSuffix();
			if (emissiveSuffix != null) {
				Collection<Sprite.Info> spriteInfos = cir.getReturnValue();
				Set<Identifier> emissiveIds = new ObjectOpenHashSet<>();
				emissiveIdMap = new Object2ObjectOpenHashMap<>();
				for (Sprite.Info spriteInfo : spriteInfos) {
					Identifier id = spriteInfo.getId();
					if (!id.getPath().endsWith(emissiveSuffix)) {
						Identifier emissiveId = new Identifier(id.getNamespace(), id.getPath() + emissiveSuffix);
						Identifier emissiveLocation = getTexturePath(emissiveId);
						if (resourceManager.containsResource(emissiveLocation)) {
							emissiveIds.add(emissiveId);
							emissiveIdMap.put(id, emissiveId);
						}
					}
				}
				if (!emissiveIds.isEmpty()) {
					Collection<Sprite.Info> emissiveSpriteInfos = loadSprites(resourceManager, emissiveIds);
					spriteInfos.addAll(emissiveSpriteInfos);
				} else {
					emissiveIdMap = null;
				}
			}
			loadingEmissiveSprites = false;
		}
	}

	@Inject(method = "stitch(Lnet/minecraft/resource/ResourceManager;Ljava/util/stream/Stream;Lnet/minecraft/util/profiler/Profiler;I)Lnet/minecraft/client/texture/SpriteAtlasTexture$Data;", at = @At("TAIL"))
	private void onTailStitch(ResourceManager resourceManager, Stream<Identifier> idStream, Profiler profiler, int mipmapLevel, CallbackInfoReturnable<SpriteAtlasTexture.Data> cir) {
		SpriteAtlasTexture.Data data = cir.getReturnValue();
		((SpriteAtlasTextureDataExtension) data).setEmissiveIdMap(emissiveIdMap);
		emissiveIdMap = null;
	}

	@Inject(method = "upload(Lnet/minecraft/client/texture/SpriteAtlasTexture$Data;)V", at = @At("TAIL"))
	private void onTailUpload(SpriteAtlasTexture.Data data, CallbackInfo ci) {
		Map<Identifier, Identifier> emissiveIdMap = ((SpriteAtlasTextureDataExtension) data).getEmissiveIdMap();
		if (emissiveIdMap != null) {
			emissiveIdMap.forEach((id, emissiveId) -> {
				Sprite sprite = sprites.get(id);
				if (sprite != null) {
					Sprite emissiveSprite = sprites.get(emissiveId);
					if (emissiveSprite != null) {
						((SpriteExtension) sprite).setEmissiveSprite(emissiveSprite);
					}
				}
			});
		}
	}
}
