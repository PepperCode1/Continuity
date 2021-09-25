package me.pepperbell.continuity.client.properties.section;

import java.util.Locale;
import java.util.Properties;

import me.pepperbell.continuity.client.ContinuityClient;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;

public class OverlayPropertiesSection {
	protected Properties properties;
	protected Identifier id;
	protected String packName;

	protected int tintIndex = -1;
	protected BlockState tintBlock;
	protected BlendMode layer = BlendMode.CUTOUT_MIPPED;

	public OverlayPropertiesSection(Properties properties, Identifier id, String packName) {
		this.properties = properties;
		this.id = id;
		this.packName = packName;
	}

	public void init() {
		parseTintIndex();
		parseTintBlock();
		parseLayer();
	}

	protected void parseTintIndex() {
		String tintIndexStr = properties.getProperty("tintIndex");
		if (tintIndexStr != null) {
			tintIndexStr = tintIndexStr.trim();
			try {
				int tintIndex = Integer.parseInt(tintIndexStr);
				if (tintIndex >= 0) {
					this.tintIndex = tintIndex;
					return;
				}
			} catch (NumberFormatException e) {
				//
			}
			ContinuityClient.LOGGER.warn("Invalid 'tintIndex' value '" + tintIndexStr + "' in file '" + id + "' in pack '" + packName + "'");
		}
	}

	protected void parseTintBlock() {
		String tintBlockStr = properties.getProperty("tintBlock");
		if (tintBlockStr != null) {
			tintBlockStr = tintBlockStr.trim();
			String[] parts = tintBlockStr.split(":", 3);
			if (parts.length != 0) {
				Identifier blockId;
				try {
					if (parts.length == 1) {
						blockId = new Identifier(parts[0]);
					} else if (parts.length == 2) {
						if (parts[1].contains("=")) {
							blockId = new Identifier(parts[0]);
						} else {
							blockId = new Identifier(parts[0], parts[1]);
						}
					} else {
						blockId = new Identifier(parts[0], parts[1]);
					}
				} catch (InvalidIdentifierException e) {
					ContinuityClient.LOGGER.warn("Invalid 'tintBlock' value '" + tintBlockStr + "' in file '" + id + "' in pack '" + packName + "'", e);
					return;
				}
				Block block = Registry.BLOCK.get(blockId);
				if (block != Blocks.AIR) {
					tintBlock = block.getDefaultState();
				} else {
					ContinuityClient.LOGGER.warn("Invalid block '" + blockId + "' in 'tintBlock' value '" + tintBlockStr + "' in file '" + id + "' in pack '" + packName + "'");
				}
			}
		}
	}

	protected void parseLayer() {
		String layerStr = properties.getProperty("layer");
		if (layerStr != null) {
			layerStr = layerStr.trim().toLowerCase(Locale.ROOT);
			if (layerStr.equals("cutout_mipped")) {
				layer = BlendMode.CUTOUT_MIPPED;
			} else if (layerStr.equals("cutout")) {
				layer = BlendMode.CUTOUT;
			} else if (layerStr.equals("translucent")) {
				layer = BlendMode.TRANSLUCENT;
			} else {
				ContinuityClient.LOGGER.warn("Invalid 'layer' value '" + layerStr + " in file '" + id + "' in pack '" + packName + "'");
			}
		}
	}

	public int getTintIndex() {
		return tintIndex;
	}

	public BlockState getTintBlock() {
		return tintBlock;
	}

	public BlendMode getLayer() {
		return layer;
	}

	public interface Provider {
		OverlayPropertiesSection getOverlayPropertiesSection();
	}
}
