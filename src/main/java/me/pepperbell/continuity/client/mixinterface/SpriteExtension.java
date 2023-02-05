package me.pepperbell.continuity.client.mixinterface;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.texture.Sprite;

public interface SpriteExtension {
	@Nullable
	Sprite continuity$getEmissiveSprite();

	void continuity$setEmissiveSprite(Sprite sprite);
}
