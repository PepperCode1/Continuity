package me.pepperbell.continuity.client.mixinterface;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.texture.Sprite;

public interface SpriteExtension {
	@Nullable
	Sprite getEmissiveSprite();

	void setEmissiveSprite(Sprite sprite);
}
