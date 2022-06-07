package me.pepperbell.continuity.client.properties.overlay;

import java.util.Properties;

import me.pepperbell.continuity.client.properties.StandardConnectingCTMProperties;
import net.minecraft.util.Identifier;

public class StandardConnectingOverlayCTMProperties extends StandardConnectingCTMProperties implements OverlayPropertiesSection.Provider {
	protected OverlayPropertiesSection overlaySection;

	public StandardConnectingOverlayCTMProperties(Properties properties, Identifier id, String packName, int packPriority, String method) {
		super(properties, id, packName, packPriority, method);
		overlaySection = new OverlayPropertiesSection(properties, id, packName);
	}

	@Override
	public void init() {
		super.init();
		overlaySection.init();
	}

	@Override
	public OverlayPropertiesSection getOverlayPropertiesSection() {
		return overlaySection;
	}
}
