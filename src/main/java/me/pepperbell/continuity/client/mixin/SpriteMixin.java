package me.pepperbell.continuity.client.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import me.pepperbell.continuity.client.mixinterface.SpriteExtension;
import net.minecraft.client.texture.Sprite;

@Mixin(Sprite.class)
public class SpriteMixin implements SpriteExtension {
	@Unique
	private Sprite continuity$emissiveSprite;

	@Override
	@Nullable
	public Sprite continuity$getEmissiveSprite() {
		return continuity$emissiveSprite;
	}

	@Override
	public void continuity$setEmissiveSprite(Sprite sprite) {
		continuity$emissiveSprite = sprite;
	}
}
