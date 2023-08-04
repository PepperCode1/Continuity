package me.pepperbell.continuity.client.resource;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.CTMLoader;
import me.pepperbell.continuity.api.client.CTMLoaderRegistry;
import me.pepperbell.continuity.api.client.CTMProperties;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.util.BooleanState;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class CTMPropertiesLoader {
	public static LoadingResult loadAllWithState(ResourceManager resourceManager) {
		// TODO: move these to the very beginning of resource reload
		ResourcePackUtil.setup(resourceManager);
		BiomeHolderManager.clearCache();

		LoadingResult result = loadAll(resourceManager);

		// TODO: move these to the very end of resource reload
		ResourcePackUtil.clear();
		BiomeHolderManager.refreshHolders();

		return result;
	}

	public static LoadingResult loadAll(ResourceManager resourceManager) {
		LoadingData loadingData = new LoadingData();

		int packPriority = 0;
		Iterator<ResourcePack> iterator = resourceManager.streamResourcePacks().iterator();
		BooleanState invalidIdentifierState = InvalidIdentifierStateHolder.get();
		invalidIdentifierState.enable();
		while (iterator.hasNext()) {
			ResourcePack pack = iterator.next();
			loadAll(pack, packPriority, loadingData);
			packPriority++;
		}
		invalidIdentifierState.disable();

		resolveMultipassDependents(loadingData);

		return new LoadingResult(loadingData.affectsBlock, loadingData.ignoresBlock, loadingData.all.isEmpty());
	}

	private static void loadAll(ResourcePack pack, int packPriority, LoadingData loadingData) {
		String packName = pack.getName();
		for (String namespace : pack.getNamespaces(ResourceType.CLIENT_RESOURCES)) {
			Collection<Identifier> ids = pack.findResources(ResourceType.CLIENT_RESOURCES, namespace, "optifine/ctm", id -> id.getPath().endsWith(".properties"));
			for (Identifier id : ids) {
				try (InputStream stream = pack.open(ResourceType.CLIENT_RESOURCES, id)) {
					Properties properties = new Properties();
					properties.load(stream);
					load(properties, id, packName, packPriority, loadingData);
				} catch (Exception e) {
					ContinuityClient.LOGGER.error("Failed to load CTM properties from file '" + id + "' in pack '" + packName + "'", e);
				}
			}
		}
	}

	private static void load(Properties properties, Identifier id, String packName, int packPriority, LoadingData loadingData) {
		String method = properties.getProperty("method", "ctm").trim();
		CTMLoader<?> loader = CTMLoaderRegistry.get().getLoader(method);
		if (loader != null) {
			load(loader, properties, id, packName, packPriority, method, loadingData);
		} else {
			ContinuityClient.LOGGER.error("Unknown 'method' value '" + method + "' in file '" + id + "' in pack '" + packName + "'");
		}
	}

	private static <T extends CTMProperties> void load(CTMLoader<T> loader, Properties properties, Identifier id, String packName, int packPriority, String method, LoadingData loadingData) {
		T ctmProperties = loader.getPropertiesFactory().createProperties(properties, id, packName, packPriority, method);
		if (ctmProperties != null) {
			CTMLoadingContainer<T> container = new CTMLoadingContainer<>(loader, ctmProperties);
			loadingData.all.add(container);
			if (ctmProperties.affectsBlockStates()) {
				loadingData.affectsBlock.add(container);
			} else {
				loadingData.ignoresBlock.add(container);
			}
			if (ctmProperties.affectsTextures() && ctmProperties.isValidForMultipass()) {
				loadingData.validForMultipass.add(container);
			}
		}
	}

	private static void resolveMultipassDependents(LoadingData loadingData) {
		if (loadingData.all.isEmpty()) {
			return;
		}

		List<CTMLoadingContainer<?>> all = loadingData.all;
		List<CTMLoadingContainer<?>> validForMultipass = loadingData.validForMultipass;

		Object2ObjectOpenHashMap<Identifier, CTMLoadingContainer<?>> texture2ContainerMap = new Object2ObjectOpenHashMap<>();
		Object2ObjectOpenHashMap<Identifier, List<CTMLoadingContainer<?>>> texture2ContainerListMap = new Object2ObjectOpenHashMap<>();

		int amount = all.size();
		for (int i = 0; i < amount; i++) {
			CTMLoadingContainer<?> container = all.get(i);
			Collection<SpriteIdentifier> textureDependencies = container.getProperties().getTextureDependencies();
			for (SpriteIdentifier spriteId : textureDependencies) {
				Identifier textureId = spriteId.getTextureId();
				CTMLoadingContainer<?> containerValue = texture2ContainerMap.get(textureId);
				if (containerValue == null) {
					List<CTMLoadingContainer<?>> containerListValue = texture2ContainerListMap.get(textureId);
					if (containerListValue == null) {
						texture2ContainerMap.put(textureId, container);
					} else {
						containerListValue.add(container);
					}
				} else {
					List<CTMLoadingContainer<?>> containerList = new ObjectArrayList<>();
					containerList.add(containerValue);
					containerList.add(container);
					texture2ContainerListMap.put(textureId, containerList);
					texture2ContainerMap.remove(textureId);
				}
			}
		}

		int amount1 = validForMultipass.size();
		texture2ContainerMap.forEach((textureId, container1) -> {
			for (int i = 0; i < amount1; i++) {
				CTMLoadingContainer<?> container = validForMultipass.get(i);
				if (container.getProperties().affectsTexture(textureId)) {
					container1.addMultipassDependent(container);
				}
			}
		});
		texture2ContainerListMap.forEach((textureId, containerList) -> {
			int amount2 = containerList.size();

			for (int i = 0; i < amount1; i++) {
				CTMLoadingContainer<?> container = validForMultipass.get(i);
				if (container.getProperties().affectsTexture(textureId)) {
					for (int j = 0; j < amount2; j++) {
						CTMLoadingContainer<?> container1 = containerList.get(j);
						container1.addMultipassDependent(container);
					}
				}
			}
		});

		for (int i = 0; i < amount; i++) {
			CTMLoadingContainer<?> container = all.get(i);
			container.resolveRecursiveMultipassDependents();
		}
	}

	private static class LoadingData {
		public final List<CTMLoadingContainer<?>> all = new ObjectArrayList<>();
		public final List<CTMLoadingContainer<?>> affectsBlock = new ObjectArrayList<>();
		public final List<CTMLoadingContainer<?>> ignoresBlock = new ObjectArrayList<>();
		public final List<CTMLoadingContainer<?>> validForMultipass = new ObjectArrayList<>();
	}

	public static class LoadingResult {
		private final List<CTMLoadingContainer<?>> affectsBlock;
		private final List<CTMLoadingContainer<?>> ignoresBlock;
		private final boolean empty;

		private LoadingResult(List<CTMLoadingContainer<?>> affectsBlock, List<CTMLoadingContainer<?>> ignoresBlock, boolean empty) {
			this.affectsBlock = affectsBlock;
			this.ignoresBlock = ignoresBlock;
			this.empty = empty;
		}

		public void consumeAllAffecting(BlockState state, Consumer<CTMLoadingContainer<?>> consumer) {
			int amount = affectsBlock.size();
			for (int i = 0; i < amount; i++) {
				CTMLoadingContainer<?> container = affectsBlock.get(i);
				if (container.getProperties().affectsBlockState(state)) {
					consumer.accept(container);
				}
			}
		}

		public void consumeAllAffecting(Collection<SpriteIdentifier> spriteIds, Consumer<CTMLoadingContainer<?>> consumer) {
			int amount = ignoresBlock.size();
			for (int i = 0; i < amount; i++) {
				CTMLoadingContainer<?> container = ignoresBlock.get(i);
				for (SpriteIdentifier spriteId : spriteIds) {
					if (container.getProperties().affectsTexture(spriteId.getTextureId())) {
						consumer.accept(container);
						break;
					}
				}
			}
		}

		public boolean isEmpty() {
			return empty;
		}
	}
}
