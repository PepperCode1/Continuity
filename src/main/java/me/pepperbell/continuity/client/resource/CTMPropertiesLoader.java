package me.pepperbell.continuity.client.resource;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.pepperbell.continuity.api.client.CTMLoader;
import me.pepperbell.continuity.api.client.CTMLoaderRegistry;
import me.pepperbell.continuity.api.client.CTMProperties;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.util.BooleanState;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class CTMPropertiesLoader {
	private static final List<CTMLoadingContainer<?>> ALL = new ObjectArrayList<>();
	private static final List<CTMLoadingContainer<?>> AFFECTS_BLOCK = new ObjectArrayList<>();
	private static final List<CTMLoadingContainer<?>> IGNORES_BLOCK = new ObjectArrayList<>();
	private static final List<CTMLoadingContainer<?>> VALID_FOR_MULTIPASS = new ObjectArrayList<>();

	private static final OptionalListCreator<CTMLoadingContainer<?>> LIST_CREATOR = new OptionalListCreator<>();

	@ApiStatus.Internal
	public static void loadAll(ResourceManager resourceManager) {
		int packPriority = 0;
		Iterator<ResourcePack> iterator = resourceManager.streamResourcePacks().iterator();
		BooleanState invalidIdentifierState = InvalidIdentifierStateHolder.get();
		invalidIdentifierState.enable();
		while (iterator.hasNext()) {
			ResourcePack pack = iterator.next();
			loadAll(pack, packPriority);
			packPriority++;
		}
		invalidIdentifierState.disable();
		resolveMultipassDependents();
	}

	private static void loadAll(ResourcePack pack, int packPriority) {
		String packName = pack.getName();
		for (String namespace : pack.getNamespaces(ResourceType.CLIENT_RESOURCES)) {
			Collection<Identifier> ids = pack.findResources(ResourceType.CLIENT_RESOURCES, namespace, "optifine/ctm", Integer.MAX_VALUE, path -> path.endsWith(".properties"));
			for (Identifier id : ids) {
				try (InputStream stream = pack.open(ResourceType.CLIENT_RESOURCES, id)) {
					Properties properties = new Properties();
					properties.load(stream);
					load(properties, id, packName, packPriority);
				} catch (Exception e) {
					ContinuityClient.LOGGER.error("Failed to load CTM properties from file '" + id + "' in pack '" + packName + "'", e);
				}
			}
		}
	}

	private static void load(Properties properties, Identifier id, String packName, int packPriority) {
		String method = properties.getProperty("method", "ctm").trim();
		CTMLoader<?> loader = CTMLoaderRegistry.get().getLoader(method);
		if (loader != null) {
			load(loader, properties, id, packName, packPriority, method);
		} else {
			ContinuityClient.LOGGER.error("Unknown 'method' value '" + method + "' in file '" + id + "' in pack '" + packName + "'");
		}
	}

	private static <T extends CTMProperties> void load(CTMLoader<T> loader, Properties properties, Identifier id, String packName, int packPriority, String method) {
		T ctmProperties = loader.getPropertiesFactory().createProperties(properties, id, packName, packPriority, method);
		if (ctmProperties != null) {
			CTMLoadingContainer<T> container = new CTMLoadingContainer<>(loader, ctmProperties);
			ALL.add(container);
			if (ctmProperties.affectsBlockStates()) {
				AFFECTS_BLOCK.add(container);
			} else {
				IGNORES_BLOCK.add(container);
			}
			if (ctmProperties.affectsTextures() && ctmProperties.isValidForMultipass()) {
				VALID_FOR_MULTIPASS.add(container);
			}
		}
	}

	private static void resolveMultipassDependents() {
		if (isEmpty()) {
			return;
		}

		Object2ObjectOpenHashMap<Identifier, CTMLoadingContainer<?>> texture2ContainerMap = new Object2ObjectOpenHashMap<>();
		Object2ObjectOpenHashMap<Identifier, List<CTMLoadingContainer<?>>> texture2ContainerListMap = new Object2ObjectOpenHashMap<>();

		int amount = ALL.size();
		for (int i = 0; i < amount; i++) {
			CTMLoadingContainer<?> container = ALL.get(i);
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

		int amount1 = VALID_FOR_MULTIPASS.size();
		ObjectIterator<Object2ObjectMap.Entry<Identifier, CTMLoadingContainer<?>>> iterator = texture2ContainerMap.object2ObjectEntrySet().fastIterator();
		while (iterator.hasNext()) {
			Object2ObjectMap.Entry<Identifier, CTMLoadingContainer<?>> entry = iterator.next();
			Identifier textureId = entry.getKey();
			CTMLoadingContainer<?> container1 = entry.getValue();

			for (int i = 0; i < amount1; i++) {
				CTMLoadingContainer<?> container = VALID_FOR_MULTIPASS.get(i);
				if (container.getProperties().affectsTexture(textureId)) {
					container1.addMultipassDependent(container);
				}
			}
		}
		ObjectIterator<Object2ObjectMap.Entry<Identifier, List<CTMLoadingContainer<?>>>> iterator1 = texture2ContainerListMap.object2ObjectEntrySet().fastIterator();
		while (iterator1.hasNext()) {
			Object2ObjectMap.Entry<Identifier, List<CTMLoadingContainer<?>>> entry = iterator1.next();
			Identifier textureId = entry.getKey();
			List<CTMLoadingContainer<?>> containerList = entry.getValue();
			int amount2 = containerList.size();

			for (int i = 0; i < amount1; i++) {
				CTMLoadingContainer<?> container = VALID_FOR_MULTIPASS.get(i);
				if (container.getProperties().affectsTexture(textureId)) {
					for (int j = 0; j < amount2; j++) {
						CTMLoadingContainer<?> container1 = containerList.get(j);
						container1.addMultipassDependent(container);
					}
				}
			}
		}

		for (int i = 0; i < amount; i++) {
			CTMLoadingContainer<?> container = ALL.get(i);
			container.resolveRecursiveMultipassDependents();
		}
	}

	public static void consumeAllAffecting(BlockState state, Consumer<CTMLoadingContainer<?>> consumer) {
		int amount = AFFECTS_BLOCK.size();
		for (int i = 0; i < amount; i++) {
			CTMLoadingContainer<?> container = AFFECTS_BLOCK.get(i);
			if (container.getProperties().affectsBlockState(state)) {
				consumer.accept(container);
			}
		}
	}

	@Nullable
	public static List<CTMLoadingContainer<?>> getAllAffecting(BlockState state) {
		consumeAllAffecting(state, LIST_CREATOR);
		return LIST_CREATOR.get();
	}

	public static void consumeAllAffecting(Collection<SpriteIdentifier> spriteIds, Consumer<CTMLoadingContainer<?>> consumer) {
		int amount = IGNORES_BLOCK.size();
		for (int i = 0; i < amount; i++) {
			CTMLoadingContainer<?> container = IGNORES_BLOCK.get(i);
			for (SpriteIdentifier spriteId : spriteIds) {
				if (container.getProperties().affectsTexture(spriteId.getTextureId())) {
					consumer.accept(container);
					break;
				}
			}
		}
	}

	@Nullable
	public static List<CTMLoadingContainer<?>> getAllAffecting(Collection<SpriteIdentifier> spriteIds) {
		consumeAllAffecting(spriteIds, LIST_CREATOR);
		return LIST_CREATOR.get();
	}

	public static boolean isEmpty() {
		return ALL.isEmpty();
	}

	@ApiStatus.Internal
	public static void clearAll() {
		ALL.clear();
		AFFECTS_BLOCK.clear();
		IGNORES_BLOCK.clear();
		VALID_FOR_MULTIPASS.clear();
	}

	private static class OptionalListCreator<T> implements Consumer<T> {
		private ObjectArrayList<T> list = null;

		@Override
		public void accept(T t) {
			if (list == null) {
				list = new ObjectArrayList<>();
			}
			list.add(t);
		}

		@Nullable
		public ObjectArrayList<T> get() {
			ObjectArrayList<T> list = this.list;
			this.list = null;
			return list;
		}
	}
}
