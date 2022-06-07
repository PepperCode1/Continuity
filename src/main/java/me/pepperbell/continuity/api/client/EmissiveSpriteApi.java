package me.pepperbell.continuity.api.client;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.impl.client.EmissiveSpriteApiImpl;
import net.minecraft.client.texture.Sprite;

@ApiStatus.NonExtendable
public interface EmissiveSpriteApi {
	static EmissiveSpriteApi get() {
		return EmissiveSpriteApiImpl.INSTANCE;
	}

	@Nullable
	Sprite getEmissiveSprite(Sprite sprite);
}
