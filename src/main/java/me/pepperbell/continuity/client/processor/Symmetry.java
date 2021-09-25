package me.pepperbell.continuity.client.processor;

import net.minecraft.util.math.Direction;

public enum Symmetry {
	NONE,
	OPPOSITE,
	ALL;

	public Direction getActualFace(Direction face) {
		if (this == Symmetry.OPPOSITE) {
			if (face.getDirection() == Direction.AxisDirection.POSITIVE) {
				face = face.getOpposite();
			}
		} else if (this == Symmetry.ALL) {
			face = Direction.DOWN;
		}
		return face;
	}
}
