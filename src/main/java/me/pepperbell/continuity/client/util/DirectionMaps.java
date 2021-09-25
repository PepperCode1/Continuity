package me.pepperbell.continuity.client.util;

import org.apache.commons.lang3.ArrayUtils;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.util.math.Direction;

public final class DirectionMaps {
	public static final Direction[][][] DIRECTION_MAPS = new Direction[6][8][];
	static {
		for (Direction face : Direction.values()) {
			Direction textureUp;
			if (face == Direction.UP) {
				textureUp = Direction.NORTH;
			} else if (face == Direction.DOWN) {
				textureUp = Direction.SOUTH;
			} else {
				textureUp = Direction.UP;
			}

			Direction textureLeft;
			if (face.getDirection() == Direction.AxisDirection.NEGATIVE) {
				textureLeft = textureUp.rotateClockwise(face.getAxis());
			} else {
				textureLeft = textureUp.rotateCounterclockwise(face.getAxis());
			}

			Direction[][] map = DIRECTION_MAPS[face.ordinal()];

			map[0] = new Direction[] { textureLeft, textureUp.getOpposite(), textureLeft.getOpposite(), textureUp }; // l d r u
			map[1] = map[0].clone(); // d r u l
			ArrayUtils.shift(map[1], -1);
			map[2] = map[1].clone(); // r u l d
			ArrayUtils.shift(map[2], -1);
			map[3] = map[2].clone(); // u l d r
			ArrayUtils.shift(map[3], -1);

			map[4] = map[0].clone(); // u r d l ; v - 1 ; h - 3
			ArrayUtils.reverse(map[4]);
			map[5] = map[4].clone(); // l u r d ; v - 0 ; h - 2
			ArrayUtils.shift(map[5], 1);
			map[6] = map[5].clone(); // d l u r ; v - 3 ; h - 1
			ArrayUtils.shift(map[6], 1);
			map[7] = map[6].clone(); // r d l u ; v - 2 ; h - 0
			ArrayUtils.shift(map[7], 1);
		}
	}

	public static Direction[][] getMap(Direction direction) {
		return DIRECTION_MAPS[direction.ordinal()];
	}

	public static Direction[] getDirections(QuadView quad) {
		return getMap(quad.lightFace())[QuadUtil.getTextureOrientation(quad)];
	}
}
