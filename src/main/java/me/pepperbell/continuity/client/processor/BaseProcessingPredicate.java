package me.pepperbell.continuity.client.processor;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import me.pepperbell.continuity.client.properties.BaseCTMProperties;
import me.pepperbell.continuity.client.util.biome.BiomeRetriever;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;

public class BaseProcessingPredicate implements ProcessingPredicate {
	protected Set<Identifier> matchTilesSet;
	protected EnumSet<Direction> faces;
	protected Predicate<Biome> biomePredicate;
	protected IntPredicate heightPredicate;
	protected Predicate<String> blockEntityNamePredicate;

	public BaseProcessingPredicate(Set<Identifier> matchTilesSet, EnumSet<Direction> faces, Predicate<Biome> biomePredicate, IntPredicate heightPredicate, Predicate<String> blockEntityNamePredicate) {
		this.matchTilesSet = matchTilesSet;
		this.faces = faces;
		this.biomePredicate = biomePredicate;
		this.heightPredicate = heightPredicate;
		this.blockEntityNamePredicate = blockEntityNamePredicate;
	}

	@Override
	public boolean shouldProcessQuad(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos) {
		if (matchTilesSet != null) {
			if (!matchTilesSet.contains(sprite.getId())) {
				return false;
			}
		}
		if (heightPredicate != null) {
			if (!heightPredicate.test(pos.getY())) {
				return false;
			}
		}
		if (faces != null) {
			Direction face = quad.lightFace();
 			if (state.contains(Properties.AXIS)) {
 				Direction.Axis axis = state.get(Properties.AXIS);
 				if (axis == Direction.Axis.X) {
 					face = face.rotateClockwise(Direction.Axis.Z);
				} else if (axis == Direction.Axis.Z) {
					face = face.rotateCounterclockwise(Direction.Axis.X);
				}
			}
			if (!faces.contains(face)) {
				return false;
			}
		}
		if (biomePredicate != null) {
			Biome biome = BiomeRetriever.getBiome(blockView, pos);
			if (biome != null) {
				if (!biomePredicate.test(biome)) {
					return false;
				}
			} else {
				return false;
			}
		}
		if (blockEntityNamePredicate != null) {
			BlockEntity blockEntity = blockView.getBlockEntity(pos);
			if (blockEntity instanceof Nameable nameable) {
				if (nameable.hasCustomName()) {
					if (!blockEntityNamePredicate.test(nameable.getCustomName().asString())) {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	public static BaseProcessingPredicate fromProperties(BaseCTMProperties properties) {
		return new BaseProcessingPredicate(properties.getMatchTilesSet(), properties.getFaces(), properties.getBiomePredicate(), properties.getHeightPredicate(), properties.getBlockEntityNamePredicate());
	}
}
