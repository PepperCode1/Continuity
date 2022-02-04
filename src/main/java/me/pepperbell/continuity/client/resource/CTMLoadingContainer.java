package me.pepperbell.continuity.client.resource;

import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.api.client.CTMLoader;
import me.pepperbell.continuity.api.client.CTMProperties;
import me.pepperbell.continuity.api.client.QuadProcessor;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;

public class CTMLoadingContainer<T extends CTMProperties> implements Comparable<CTMLoadingContainer<?>> {
	private final CTMLoader<T> loader;
	private final T properties;

	private Set<CTMLoadingContainer<?>> multipassDependents;
	private Set<CTMLoadingContainer<?>> recursiveMultipassDependents;

	private QuadProcessor cachedProcessor;

	public CTMLoadingContainer(CTMLoader<T> loader, T properties) {
		this.loader = loader;
		this.properties = properties;
	}

	public CTMLoader<T> getLoader() {
		return loader;
	}

	public T getProperties() {
		return properties;
	}

	public void addMultipassDependent(CTMLoadingContainer<?> dependent) {
		if (multipassDependents == null) {
			multipassDependents = new ObjectOpenHashSet<>();
		}
		multipassDependents.add(dependent);
	}

	public void resolveRecursiveMultipassDependents() {
		if (multipassDependents != null) {
			recursiveMultipassDependents = new ObjectOpenHashSet<>();
			addDependentsRecursively(this);
		}
	}

	protected void addDependentsRecursively(CTMLoadingContainer<?> root) {
		if (multipassDependents != null) {
			for (CTMLoadingContainer<?> dependent : multipassDependents) {
				if (dependent != root) {
					if (root.recursiveMultipassDependents.add(dependent)) {
						dependent.addDependentsRecursively(root);
					}
				}
			}
		}
	}

	@Nullable
	public Set<CTMLoadingContainer<?>> getRecursiveMultipassDependents() {
		return recursiveMultipassDependents;
	}

	public QuadProcessor toProcessor(Function<SpriteIdentifier, Sprite> textureGetter) {
		if (cachedProcessor == null) {
			cachedProcessor = loader.getProcessorFactory().createProcessor(properties, textureGetter);
		}
		return cachedProcessor;
	}

	@Override
	public int compareTo(@NotNull CTMLoadingContainer<?> o) {
		return properties.compareTo(o.properties);
	}
}
