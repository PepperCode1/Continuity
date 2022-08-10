package me.pepperbell.continuity.client.processor.simple;

import java.util.Random;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.processor.DirectionMaps;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.properties.StandardConnectingCTMProperties;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class CTMSpriteProvider implements SpriteProvider {
	// Indices for this array are formed from these bit values:
	// 128 64  32
	// 1   *   16
	// 2   4   8
	public static final int[] SPRITE_INDEX_MAP = new int[] {
			0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
			1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
			0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
			1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
			36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
			16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22,
			36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
			37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45,
			0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
			1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
			0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
			1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
			36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
			16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44,
			36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
			37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26,
	};

	protected Sprite[] sprites;
	protected ConnectionPredicate connectionPredicate;
	protected boolean innerSeams;
	protected boolean useTextureOrientation;

	public CTMSpriteProvider(Sprite[] sprites, ConnectionPredicate connectionPredicate, boolean innerSeams, boolean useTextureOrientation) {
		this.sprites = sprites;
		this.connectionPredicate = connectionPredicate;
		this.innerSeams = innerSeams;
		this.useTextureOrientation = useTextureOrientation;
	}

	@Override
	@Nullable
	public Sprite getSprite(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, ProcessingDataProvider dataProvider) {
		Direction[] directions = useTextureOrientation ? DirectionMaps.getDirections(quad) : DirectionMaps.getMap(quad.lightFace())[0];
		BlockPos.Mutable mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS_KEY);
		int connections = getConnections(connectionPredicate, innerSeams, directions, mutablePos, blockView, state, pos, quad.lightFace(), sprite);
		return sprites[SPRITE_INDEX_MAP[connections]];
	}

	public static int getConnections(ConnectionPredicate connectionPredicate, boolean innerSeams, Direction[] directions, BlockPos.Mutable mutablePos, BlockRenderView blockView, BlockState state, BlockPos pos, Direction face, Sprite quadSprite) {
		int connections = 0;
		for (int i = 0; i < 4; i++) {
			mutablePos.set(pos, directions[i]);
			if (connectionPredicate.shouldConnect(blockView, state, pos, mutablePos, face, quadSprite, innerSeams)) {
				connections |= 1 << (i * 2);
			}
		}
		for (int i = 0; i < 4; i++) {
			int index1 = i;
			int index2 = (i + 1) % 4;
			if (((connections >> index1 * 2) & 1) == 1 && ((connections >> index2 * 2) & 1) == 1) {
				mutablePos.set(pos, directions[index1]).move(directions[index2]);
				if (connectionPredicate.shouldConnect(blockView, state, pos, mutablePos, face, quadSprite, innerSeams)) {
					connections |= 1 << (i * 2 + 1);
				}
			}
		}
		return connections;
	}

	public static class Factory implements SpriteProvider.Factory<StandardConnectingCTMProperties> {
		protected boolean useTextureOrientation;

		public Factory(boolean useTextureOrientation) {
			this.useTextureOrientation = useTextureOrientation;
		}

		@Override
		public SpriteProvider createSpriteProvider(Sprite[] sprites, StandardConnectingCTMProperties properties) {
			return new CTMSpriteProvider(sprites, properties.getConnectionPredicate(), properties.getInnerSeams(), useTextureOrientation);
		}

		@Override
		public int getTextureAmount(StandardConnectingCTMProperties properties) {
			return 47;
		}
	}
}
