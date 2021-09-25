package me.pepperbell.continuity.client.processor.overlay;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import me.pepperbell.continuity.client.processor.BaseProcessingPredicate;
import me.pepperbell.continuity.client.properties.BaseCTMProperties;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;

public class OverlayProcessingPredicate extends BaseProcessingPredicate {
	public OverlayProcessingPredicate(Set<Identifier> matchTilesSet, EnumSet<Direction> faces, Predicate<Biome> biomePredicate, IntPredicate heightPredicate, Predicate<String> blockEntityNamePredicate) {
		super(matchTilesSet, faces, biomePredicate, heightPredicate, blockEntityNamePredicate);
	}

	@Override
	public boolean shouldProcessQuad(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos) {
		if (!super.shouldProcessQuad(quad, sprite, blockView, state, pos)) {
			return false;
		}
		return QuadUtil.isQuadUnitSquare(quad);
	}

	public static OverlayProcessingPredicate fromProperties(BaseCTMProperties properties) {
		return new OverlayProcessingPredicate(properties.getMatchTilesSet(), properties.getFaces(), properties.getBiomePredicate(), properties.getHeightPredicate(), properties.getBlockEntityNamePredicate());
	}
}
