package me.pepperbell.continuity.client.properties.overlay;

import java.util.Properties;

import me.pepperbell.continuity.client.properties.RandomCTMProperties;
import me.pepperbell.continuity.client.properties.section.OverlayPropertiesSection;
import net.minecraft.util.Identifier;

public class RandomOverlayCTMProperties extends RandomCTMProperties implements OverlayPropertiesSection.Provider {
	protected OverlayPropertiesSection overlaySection;

	public RandomOverlayCTMProperties(Properties properties, Identifier id, String packName, int packPriority, String method) {
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
