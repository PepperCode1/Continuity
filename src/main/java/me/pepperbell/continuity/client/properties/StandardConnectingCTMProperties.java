package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import net.minecraft.util.Identifier;

public class StandardConnectingCTMProperties extends ConnectingCTMProperties {
	protected boolean innerSeams = false;

	public StandardConnectingCTMProperties(Properties properties, Identifier id, String packName, int packPriority, String method) {
		super(properties, id, packName, packPriority, method);
	}

	@Override
	public void init() {
		super.init();
		parseInnerSeams();
	}

	protected void parseInnerSeams() {
		String innerSeamsStr = properties.getProperty("innerSeams");
		if (innerSeamsStr != null) {
			innerSeams = Boolean.parseBoolean(innerSeamsStr.trim());
		}
	}

	public boolean getInnerSeams() {
		return innerSeams;
	}
}
