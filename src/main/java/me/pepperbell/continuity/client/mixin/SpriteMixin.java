package me.pepperbell.continuity.client.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import me.pepperbell.continuity.client.mixinterface.SpriteExtension;
import net.minecraft.client.texture.Sprite;

@Mixin(Sprite.class)
public class SpriteMixin implements SpriteExtension {
	@Unique
	private Sprite emissiveSprite;

	@Override
	@Nullable
	public Sprite getEmissiveSprite() {
		return emissiveSprite;
	}

	@Override
	public void setEmissiveSprite(Sprite sprite) {
		emissiveSprite = sprite;
	}
}
