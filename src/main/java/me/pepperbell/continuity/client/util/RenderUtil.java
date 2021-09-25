package me.pepperbell.continuity.client.util;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public final class RenderUtil {
	private static final BakedModelManager MODEL_MANAGER = MinecraftClient.getInstance().getBakedModelManager();
	private static final BlockColors BLOCK_COLORS = MinecraftClient.getInstance().getBlockColors();

	private static final ThreadLocal<MaterialFinder> MATERIAL_FINDER = ThreadLocal.withInitial(() -> RendererAccess.INSTANCE.getRenderer().materialFinder());

	public static MaterialFinder getMaterialFinder() {
		return MATERIAL_FINDER.get().clear();
	}

	public static SpriteFinder getSpriteFinder() {
		return SpriteFinder.get(MODEL_MANAGER.getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
	}

	public static int getTintColor(BlockState state, BlockRenderView blockView, BlockPos pos, int tintIndex) {
		if (state == null || tintIndex == -1) {
			return -1;
		}
		return 0xFF000000 | BLOCK_COLORS.getColor(state, blockView, pos, tintIndex);
	}
}
