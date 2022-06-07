package me.pepperbell.continuity.client.model;

import me.pepperbell.continuity.impl.client.ContinuityFeatureStatesImpl;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;

public class ModelObjectsContainer {
	public static final ThreadLocal<ModelObjectsContainer> THREAD_LOCAL = ThreadLocal.withInitial(ModelObjectsContainer::new);

	public final CTMBakedModel.CTMQuadTransform ctmQuadTransform = new CTMBakedModel.CTMQuadTransform();
	public final EmissiveBakedModel.EmissiveBlockQuadTransform emissiveBlockQuadTransform = new EmissiveBakedModel.EmissiveBlockQuadTransform();
	public final EmissiveBakedModel.EmissiveItemQuadTransform emissiveItemQuadTransform = new EmissiveBakedModel.EmissiveItemQuadTransform();

	public final ContinuityFeatureStatesImpl featureStates = new ContinuityFeatureStatesImpl();
	public final MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();

	public static ModelObjectsContainer get() {
		return THREAD_LOCAL.get();
	}
}
