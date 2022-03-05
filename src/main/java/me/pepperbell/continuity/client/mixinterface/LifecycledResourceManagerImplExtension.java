package me.pepperbell.continuity.client.mixinterface;

import net.minecraft.util.Identifier;

public interface LifecycledResourceManagerImplExtension {
	void addRedirect(Identifier from, Identifier to);
}
