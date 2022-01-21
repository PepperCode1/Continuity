package me.pepperbell.continuity.client.properties.overlay;

import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import me.pepperbell.continuity.client.properties.ConnectingCTMProperties;
import me.pepperbell.continuity.client.properties.section.OverlayPropertiesSection;
import me.pepperbell.continuity.client.util.PropertiesParsingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

public class StandardOverlayCTMProperties extends ConnectingCTMProperties implements OverlayPropertiesSection.Provider {
	protected OverlayPropertiesSection overlaySection;
	protected Set<Identifier> connectTilesSet;
	protected Predicate<BlockState> connectBlocksPredicate;

	public StandardOverlayCTMProperties(Properties properties, Identifier id, String packName, int packPriority, String method) {
		super(properties, id, packName, packPriority, method);
		overlaySection = new OverlayPropertiesSection(properties, id, packName);
	}

	@Override
	public void init() {
		super.init();
		overlaySection.init();
		parseConnectTiles();
		parseConnectBlocks();
	}

	@Override
	public OverlayPropertiesSection getOverlayPropertiesSection() {
		return overlaySection;
	}

	protected void parseConnectTiles() {
		connectTilesSet = PropertiesParsingHelper.parseMatchTiles(properties, "connectTiles", id, packName);
	}

	protected void parseConnectBlocks() {
		connectBlocksPredicate = PropertiesParsingHelper.parseBlockStates(properties, "connectBlocks", id, packName);
	}

	public Set<Identifier> getConnectTilesSet() {
		return connectTilesSet;
	}

	public Predicate<BlockState> getConnectBlocksPredicate() {
		return connectBlocksPredicate;
	}
}
