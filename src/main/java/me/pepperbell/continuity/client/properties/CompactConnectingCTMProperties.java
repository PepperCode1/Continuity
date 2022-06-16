package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.util.Identifier;

public class CompactConnectingCTMProperties extends StandardConnectingCTMProperties {
	protected Int2IntMap tileReplacementMap;

	public CompactConnectingCTMProperties(Properties properties, Identifier id, String packName, int packPriority, String method) {
		super(properties, id, packName, packPriority, method);
	}

	@Override
	public void init() {
		super.init();
		parseTileReplacements();
	}

	protected void parseTileReplacements() {
		for (String key : properties.stringPropertyNames()) {
			if (key.startsWith("ctm.")) {
				String indexStr = key.substring(4);
				int index;
				try {
					index = Integer.parseInt(indexStr);
				} catch (NumberFormatException e) {
					continue;
				}
				if (index < 0) {
					continue;
				}

				String valueStr = properties.getProperty(key);
				int value;
				try {
					value = Integer.parseInt(valueStr);
				} catch (NumberFormatException e) {
					ContinuityClient.LOGGER.warn("Invalid '" + key + "' value '" + valueStr + "' in file '" + id + "' in pack '" + packName + "'");
					continue;
				}
				// TODO: deduplicate code
				if (value < 0) {
					ContinuityClient.LOGGER.warn("Invalid '" + key + "' value '" + valueStr + "' in file '" + id + "' in pack '" + packName + "'");
					continue;
				}

				if (tileReplacementMap == null) {
					tileReplacementMap = new Int2IntArrayMap();
				}
				tileReplacementMap.put(index, value);
			}
		}
	}

	@Override
	public boolean isValidForMultipass() {
		return false;
	}

	public Int2IntMap getTileReplacementMap() {
		return tileReplacementMap;
	}
}
