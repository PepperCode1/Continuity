package me.pepperbell.continuity.client.mixin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.client.texture.atlas.AtlasLoader;
import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.client.texture.atlas.SingleAtlasSource;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.metadata.ResourcePackMetadata;
import net.minecraft.util.Identifier;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.resource.AtlasLoaderInitContext;
import me.pepperbell.continuity.client.resource.AtlasLoaderLoadContext;
import me.pepperbell.continuity.client.resource.EmissiveSuffixLoader;

@Mixin(AtlasLoader.class)
abstract class AtlasLoaderMixin {
	
	private static final String MTS_NAMESPACE = "mts";
	private static final String MTS_PACK_METADATA_KEY = "mts";
	private static final String MTS_VALID_RESOURCES_KEY = "validResources";
	
	private static final Map<String, Set<String>> MTS_VALID_RESOURCES_CACHE = new Object2ObjectOpenHashMap<>();

	
	@ModifyVariable(method = "<init>(Ljava/util/List;)V", at = @At(value = "LOAD", ordinal = 0), argsOnly = true, ordinal = 0)
	private List<AtlasSource> continuity$modifySources(List<AtlasSource> sources) {
		
		AtlasLoaderInitContext context = AtlasLoaderInitContext.THREAD_LOCAL.get();
		if (context == null) {
			ContinuityClient.LOGGER.debug("AtlasLoaderInitContext is null, skipping extra sources");
			return sources;
		}

		Set<Identifier> extraIds = context.getExtraIds();
		if (extraIds == null || extraIds.isEmpty()) {
			return sources;
		}

		List<AtlasSource> validExtraSources = new ObjectArrayList<>();
		for (Identifier extraId : extraIds) {
			if (isMtsResourceValid(context.getResourceManager(), extraId)) {
				validExtraSources.add(new SingleAtlasSource(extraId, Optional.empty()));
			} else {
				ContinuityClient.LOGGER.debug("Skipping invalid MTS extra texture: {}", extraId);
			}
		}

		
		if (sources instanceof ArrayList) {
			sources.addAll(0, validExtraSources);
		} else {
			List<AtlasSource> mutableSources = new ArrayList<>(validExtraSources);
			mutableSources.addAll(sources);
			return mutableSources;
		}
		return sources;
	}


	@Inject(method = "loadSources(Lnet/minecraft/resource/ResourceManager;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;", remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
	private void continuity$afterLoadSources(ResourceManager resourceManager, CallbackInfoReturnable<List<Supplier<SpriteContents>>> cir, Map<Identifier, AtlasSource.SpriteRegion> suppliers) {
		
		AtlasLoaderLoadContext context = AtlasLoaderLoadContext.THREAD_LOCAL.get();
		if (context == null) {
			ContinuityClient.LOGGER.debug("AtlasLoaderLoadContext is null, skipping emissive texture load");
			return;
		}

		String emissiveSuffix = EmissiveSuffixLoader.getEmissiveSuffix();
		if (emissiveSuffix == null) {
			return;
		}

		Map<Identifier, AtlasSource.SpriteRegion> emissiveSuppliers = new Object2ObjectOpenHashMap<>();
		Map<Identifier, Identifier> emissiveIdMap = new Object2ObjectOpenHashMap<>();

		suppliers.forEach((id, supplier) -> {
		
			if (id.getPath().endsWith(emissiveSuffix)) {
				return;
			}

			
			if (!isMtsResourceValid(resourceManager, id)) {
				ContinuityClient.LOGGER.debug("Skipping emissive for invalid MTS texture: {}", id);
				return;
			}

			Identifier emissiveId = id.withPath(id.getPath() + emissiveSuffix);
			
			if (suppliers.containsKey(emissiveId)) {
				emissiveIdMap.put(id, emissiveId);
				return;
			}

			
			Identifier emissiveLocation = new Identifier(id.getNamespace(), "textures/" + emissiveId.getPath() + ".png");
			Optional<Resource> optionalResource = resourceManager.getResource(emissiveLocation);

			if (optionalResource.isPresent()) {
				Resource resource = optionalResource.get();
				
				try (InputStream is = resource.getInputStream()) {
					if (is == null) {
						ContinuityClient.LOGGER.warn("Null input stream for emissive texture: {}", emissiveLocation);
						return;
					}

					
					emissiveSuppliers.put(emissiveId, () -> {
						try {
							
							return SpriteLoader.load(emissiveId, resourceManager.getResource(emissiveLocation).get());
						} catch (Exception e) {
							ContinuityClient.LOGGER.error("Failed to load emissive texture: {}", emissiveId, e);
							return null; 
						}
					});
					emissiveIdMap.put(id, emissiveId);
				} catch (Exception e) {
					ContinuityClient.LOGGER.error("Failed to check emissive texture stream: {}", emissiveLocation, e);
				}
			}
		});

		
		suppliers.putAll(emissiveSuppliers);
		if (!emissiveIdMap.isEmpty()) {
			context.setEmissiveIdMap(emissiveIdMap);
		}
	}

	
	private boolean isMtsResourceValid(ResourceManager resourceManager, Identifier id) {
	
		if (!MTS_NAMESPACE.equals(id.getNamespace())) {
			return true;
		}

		
		for (ResourcePack pack : resourceManager.streamResourcePacks().toList()) {
			String packName = pack.getName();
			Set<String> validPaths = MTS_VALID_RESOURCES_CACHE.computeIfAbsent(packName, name -> {
				try {
					ResourcePackMetadata metadata = pack.getMetadata();
					if (metadata != null && metadata.getRaw().has(MTS_PACK_METADATA_KEY)) {
						JsonObject mtsMeta = metadata.getRaw().getAsJsonObject(MTS_PACK_METADATA_KEY);
						if (mtsMeta.has(MTS_VALID_RESOURCES_KEY)) {
							Set<String> paths = new Object2ObjectOpenHashMap<>().keySet();
							mtsMeta.getAsJsonArray(MTS_VALID_RESOURCES_KEY).forEach(elem -> paths.add(elem.getAsString()));
							return paths;
						}
					}
				} catch (Exception e) {
					ContinuityClient.LOGGER.warn("Failed to parse MTS metadata for pack: {}", name, e);
				}
				return Set.of(); 
			});

		
			if (validPaths.contains(id.toString()) || validPaths.contains(id.getPath())) {
				return true;
			}
		}

		
		return false;
	}
}
