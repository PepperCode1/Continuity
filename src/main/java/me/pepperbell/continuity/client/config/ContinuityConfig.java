package me.pepperbell.continuity.client.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import net.fabricmc.loader.api.FabricLoader;

public class ContinuityConfig {
	protected static final Logger LOGGER = LoggerFactory.getLogger("Continuity Config");
	protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final ContinuityConfig INSTANCE = new ContinuityConfig(FabricLoader.getInstance().getConfigDir().resolve("continuity.json").toFile());
	static {
		INSTANCE.load();
	}

	protected final File file;
	protected final Object2ObjectLinkedOpenHashMap<String, Option<?>> optionMap = new Object2ObjectLinkedOpenHashMap<>();
	protected final Map<String, Option<?>> optionMapView = Collections.unmodifiableMap(optionMap);

	public final Option.BooleanOption connectedTextures = addOption(new Option.BooleanOption("connected_textures", true));
	public final Option.BooleanOption emissiveTextures = addOption(new Option.BooleanOption("emissive_textures", true));
	public final Option.BooleanOption customBlockLayers = addOption(new Option.BooleanOption("custom_block_layers", true));
	public final Option.BooleanOption useManualCulling = addOption(new Option.BooleanOption("use_manual_culling", true));

	public ContinuityConfig(File file) {
		this.file = file;
	}

	public void load() {
		if (file.exists()) {
			try (FileReader reader = new FileReader(file)) {
				fromJson(JsonParser.parseReader(reader));
			} catch (Exception e) {
				LOGGER.error("Could not load config from file '" + file.getAbsolutePath() + "'", e);
			}
		}
		save();
	}

	public void save() {
		try (FileWriter writer = new FileWriter(file)) {
			GSON.toJson(toJson(), writer);
		} catch (Exception e) {
			LOGGER.error("Could not save config to file '" + file.getAbsolutePath() + "'", e);
		}
	}

	protected void fromJson(JsonElement json) throws JsonParseException {
		if (json.isJsonObject()) {
			JsonObject object = json.getAsJsonObject();
			ObjectBidirectionalIterator<Object2ObjectMap.Entry<String, Option<?>>> iterator = optionMap.object2ObjectEntrySet().fastIterator();
			while (iterator.hasNext()) {
				Object2ObjectMap.Entry<String, Option<?>> entry = iterator.next();
				JsonElement element = object.get(entry.getKey());
				if (element != null) {
					try {
						entry.getValue().fromJson(element);
					} catch (JsonParseException e) {
						LOGGER.error("Could not read option '" + entry.getKey() + "'", e);
					}
				}
			}
		} else {
			throw new JsonParseException("Json must be an object");
		}
	}

	protected JsonElement toJson() {
		JsonObject object = new JsonObject();
		ObjectBidirectionalIterator<Object2ObjectMap.Entry<String, Option<?>>> iterator = optionMap.object2ObjectEntrySet().fastIterator();
		while (iterator.hasNext()) {
			Object2ObjectMap.Entry<String, Option<?>> entry = iterator.next();
			object.add(entry.getKey(), entry.getValue().toJson());
		}
		return object;
	}

	protected <T extends Option<?>> T addOption(T option) {
		Option<?> old = optionMap.put(option.getKey(), option);
		if (old != null) {
			LOGGER.warn("Option with key '" + old.getKey() + "' was overridden");
		}
		return option;
	}

	@Nullable
	public Option<?> getOption(String key) {
		return optionMap.get(key);
	}

	public Map<String, Option<?>> getOptionMapView() {
		return optionMapView;
	}
}
