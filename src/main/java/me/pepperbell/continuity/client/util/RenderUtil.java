package me.pepperbell.continuity.client.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.fabric.compat.FabricQuadView;
import me.pepperbell.continuity.client.ContinuityClient;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public final class RenderUtil {
	private static final BlockColors BLOCK_COLORS = MinecraftClient.getInstance().getBlockColors();
	private static final BakedModelManager MODEL_MANAGER = MinecraftClient.getInstance().getBakedModelManager();

	private static final ThreadLocal<MaterialFinder> MATERIAL_FINDER = ThreadLocal.withInitial(() -> RendererAccess.INSTANCE.getRenderer().materialFinder());

	private static final BlendModeGetter BLEND_MODE_GETTER = createBlendModeGetter();

	private static SpriteFinder blockAtlasSpriteFinder;

	private static BlendModeGetter createBlendModeGetter() {
		if (FabricLoader.getInstance().isModLoaded("frex")) {
			try {
				Field frexQuadField = FabricQuadView.class.getDeclaredField("wrapped");
				frexQuadField.setAccessible(true);
				return quad -> {
					try {
						io.vram.frex.api.material.RenderMaterial frexMaterial = ((io.vram.frex.api.mesh.QuadView) frexQuadField.get(quad)).material();
						return switch (frexMaterial.preset()) {
							case MaterialConstants.PRESET_DEFAULT -> BlendMode.DEFAULT;
							case MaterialConstants.PRESET_SOLID -> BlendMode.SOLID;
							case MaterialConstants.PRESET_CUTOUT_MIPPED -> BlendMode.CUTOUT_MIPPED;
							case MaterialConstants.PRESET_CUTOUT -> BlendMode.CUTOUT;
							case MaterialConstants.PRESET_TRANSLUCENT -> BlendMode.TRANSLUCENT;
							case MaterialConstants.PRESET_NONE -> {
								if (frexMaterial.transparency() != MaterialConstants.TRANSPARENCY_NONE) {
									yield BlendMode.TRANSLUCENT;
								} else if (frexMaterial.cutout() == MaterialConstants.CUTOUT_NONE) {
									yield BlendMode.SOLID;
								} else {
									yield frexMaterial.unmipped() ? BlendMode.CUTOUT : BlendMode.CUTOUT_MIPPED;
								}
							}
							default -> BlendMode.DEFAULT;
						};
					} catch (Exception e) {
						//
					}
					return BlendMode.DEFAULT;
				};
			} catch (Exception e) {
				ContinuityClient.LOGGER.error("Detected FREX but failed to load quad wrapper field", e);
			}
		} else if (FabricLoader.getInstance().isModLoaded("indium")) {
			return quad -> ((link.infra.indium.renderer.RenderMaterialImpl) quad.material()).blendMode(0);
		} else if (RendererAccess.INSTANCE.getRenderer() instanceof IndigoRenderer) {
			return quad -> ((RenderMaterialImpl) quad.material()).blendMode(0);
		}
		return quad -> BlendMode.DEFAULT;
	}

	public static int getTintColor(@Nullable BlockState state, BlockRenderView blockView, BlockPos pos, int tintIndex) {
		if (state == null || tintIndex == -1) {
			return -1;
		}
		return 0xFF000000 | BLOCK_COLORS.getColor(state, blockView, pos, tintIndex);
	}

	public static MaterialFinder getMaterialFinder() {
		return MATERIAL_FINDER.get().clear();
	}

	public static BlendMode getBlendMode(QuadView quad) {
		return BLEND_MODE_GETTER.getBlendMode(quad);
	}

	public static SpriteFinder getSpriteFinder() {
		return blockAtlasSpriteFinder;
	}

	private interface BlendModeGetter {
		BlendMode getBlendMode(QuadView quad);
	}

	public static class ReloadListener implements SimpleSynchronousResourceReloadListener {
		public static final Identifier ID = ContinuityClient.asId("render_util");
		public static final List<Identifier> DEPENDENCIES = List.of(ResourceReloadListenerKeys.MODELS);
		private static final ReloadListener INSTANCE = new ReloadListener();

		public static void init() {
			ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
		}

		@Override
		public void reload(ResourceManager manager) {
			blockAtlasSpriteFinder = SpriteFinder.get(MODEL_MANAGER.getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		}

		@Override
		public Identifier getFabricId() {
			return ID;
		}

		@Override
		public Collection<Identifier> getFabricDependencies() {
			return DEPENDENCIES;
		}
	}
}
