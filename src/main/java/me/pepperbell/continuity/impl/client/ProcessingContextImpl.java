package me.pepperbell.continuity.impl.client;

import java.util.List;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.ProcessingDataKey;
import me.pepperbell.continuity.api.client.ProcessingDataKeyRegistry;
import me.pepperbell.continuity.api.client.QuadProcessor;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

public class ProcessingContextImpl implements QuadProcessor.ProcessingContext {
	protected final List<Consumer<QuadEmitter>> emitterConsumers = new ObjectArrayList<>();
	protected final List<Mesh> meshes = new ObjectArrayList<>();
	protected final MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
	protected final Object[] processingData = new Object[ProcessingDataKeyRegistry.get().getRegisteredAmount()];

	protected boolean hasExtraQuads;

	@Override
	public void addEmitterConsumer(Consumer<QuadEmitter> consumer) {
		emitterConsumers.add(consumer);
	}

	@Override
	public void addMesh(Mesh mesh) {
		meshes.add(mesh);
	}

	@Override
	public QuadEmitter getExtraQuadEmitter() {
		return meshBuilder.getEmitter();
	}

	@Override
	public void markHasExtraQuads() {
		hasExtraQuads = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getData(ProcessingDataKey<T> key) {
		int index = key.getRawId();
		T data = (T) processingData[index];
		if (data == null) {
			data = key.getValueSupplier().get();
			processingData[index] = data;
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	public <T> T getDataOrNull(ProcessingDataKey<T> key) {
		return (T) processingData[key.getRawId()];
	}

	public void accept(RenderContext context) {
		if (!emitterConsumers.isEmpty()) {
			QuadEmitter quadEmitter = context.getEmitter();
			int amount = emitterConsumers.size();
			for (int i = 0; i < amount; i++) {
				emitterConsumers.get(i).accept(quadEmitter);
			}
		}
		if (!meshes.isEmpty()) {
			Consumer<Mesh> meshConsumer = context.meshConsumer();
			int amount = meshes.size();
			for (int i = 0; i < amount; i++) {
				meshConsumer.accept(meshes.get(i));
			}
		}
		if (hasExtraQuads) {
			context.meshConsumer().accept(meshBuilder.build());
		}
	}

	public void prepare() {
		hasExtraQuads = false;
	}

	public void reset() {
		emitterConsumers.clear();
		meshes.clear();
		resetData();
	}

	protected void resetData() {
		List<ProcessingDataKey<?>> allResettable = ProcessingDataKeyRegistryImpl.INSTANCE.getAllResettable();
		int amount = allResettable.size();
		for (int i = 0; i < amount; i++) {
			resetData(allResettable.get(i));
		}
	}

	protected <T> void resetData(ProcessingDataKey<T> key) {
		T value = getDataOrNull(key);
		if (value != null) {
			key.getValueResetAction().accept(value);
		}
	}
}
