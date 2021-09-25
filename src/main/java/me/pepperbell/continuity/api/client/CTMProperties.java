package me.pepperbell.continuity.api.client;

import java.util.Collection;

import net.minecraft.block.BlockState;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public interface CTMProperties extends Comparable<CTMProperties> {
	boolean affectsTextures();

	boolean affectsTexture(Identifier id);

	boolean affectsBlockStates();

	boolean affectsBlockState(BlockState state);

	Collection<SpriteIdentifier> getTextureDependencies();

	default boolean isValidForMultipass() {
		return true;
	}
}
