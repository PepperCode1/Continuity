package me.pepperbell.continuity.client.processor;

import java.util.EnumSet;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;

public class BaseProcessingPredicate implements ProcessingPredicate {
	@Nullable
	protected EnumSet<Direction> faces;
	@Nullable
	protected Predicate<Biome> biomePredicate;
	@Nullable
	protected IntPredicate heightPredicate;
	@Nullable
	protected Predicate<String> blockEntityNamePredicate;

	public BaseProcessingPredicate(@Nullable EnumSet<Direction> faces, @Nullable Predicate<Biome> biomePredicate, @Nullable IntPredicate heightPredicate, @Nullable Predicate<String> blockEntityNamePredicate) {
		this.faces = faces;
		this.biomePredicate = biomePredicate;
		this.heightPredicate = heightPredicate;
		this.blockEntityNamePredicate = blockEntityNamePredicate;
	}

	@Override
	public boolean shouldProcessQuad(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, ProcessingDataProvider dataProvider) {
		if (heightPredicate != null) {
			if (!heightPredicate.test(pos.getY())) {
				return false;
			}
		}
		if (faces != null) {
			Direction face = quad.lightFace();
 			if (appearanceState.contains(Properties.AXIS)) {
 				Direction.Axis axis = appearanceState.get(Properties.AXIS);
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
			Biome biome = dataProvider.getData(ProcessingDataKeys.BIOME_CACHE).get(blockView, pos);
			if (biome == null || !biomePredicate.test(biome)) {
				return false;
			}
		}
		if (blockEntityNamePredicate != null) {
			String blockEntityName = dataProvider.getData(ProcessingDataKeys.BLOCK_ENTITY_NAME_CACHE).get(blockView, pos);
			if (blockEntityName == null || !blockEntityNamePredicate.test(blockEntityName)) {
				return false;
			}
		}
		return true;
	}

	public static BaseProcessingPredicate fromProperties(BaseCtmProperties properties) {
		return new BaseProcessingPredicate(properties.getFaces(), properties.getBiomePredicate(), properties.getHeightPredicate(), properties.getBlockEntityNamePredicate());
	}

	public static class BiomeCache {
		@Nullable
		protected Biome biome;
		protected boolean invalid = true;

		@Nullable
		public Biome get(BlockRenderView blockView, BlockPos pos) {
			if (invalid) {
				biome = ((FabricBlockView)blockView).hasBiomes() ? ((FabricBlockView)blockView).getBiomeFabric(pos).value() : null;
				invalid = false;
			}
			return biome;
		}

		public void reset() {
			invalid = true;
		}
	}

	public static class BlockEntityNameCache {
		@Nullable
		protected String blockEntityName;
		protected boolean invalid = true;

		@Nullable
		public String get(BlockRenderView blockView, BlockPos pos) {
			if (invalid) {
				BlockEntity blockEntity = blockView.getBlockEntity(pos);
				if (blockEntity instanceof Nameable nameable) {
					if (nameable.hasCustomName()) {
						blockEntityName = nameable.getCustomName().getString();
					} else {
						blockEntityName = null;
					}
				} else {
					blockEntityName = null;
				}
				invalid = false;
			}
			return blockEntityName;
		}

		public void reset() {
			invalid = true;
		}
	}
}
