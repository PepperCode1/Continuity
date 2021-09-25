package me.pepperbell.continuity.client.handler;

import me.pepperbell.continuity.impl.client.ProcessingDataKeyRegistryImpl;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;

public class ClientStartedHandler implements ClientLifecycleEvents.ClientStarted {
	@Override
	public void onClientStarted(MinecraftClient client) {
		ProcessingDataKeyRegistryImpl.INSTANCE.lockRegistration();
	}
}
