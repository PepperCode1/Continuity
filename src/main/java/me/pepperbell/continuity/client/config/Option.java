package me.pepperbell.continuity.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

public interface Option<T> {
	String getKey();

	T get();

	void set(T value);

	JsonElement toJson();

	void fromJson(JsonElement json) throws JsonParseException;

	abstract class BaseOption<T> implements Option<T> {
		protected final String key;
		protected T value;

		public BaseOption(String key, T defaultValue) {
			this.key = key;
			value = defaultValue;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public T get() {
			return value;
		}

		@Override
		public void set(T value) {
			this.value = value;
		}
	}

	class BooleanOption extends BaseOption<Boolean> {
		public BooleanOption(String key, Boolean defaultValue) {
			super(key, defaultValue);
		}

		@Override
		public JsonElement toJson() {
			return new JsonPrimitive(get());
		}

		@Override
		public void fromJson(JsonElement json) throws JsonParseException {
			if (json.isJsonPrimitive()) {
				set(json.getAsBoolean());
			} else {
				throw new JsonParseException("Json must be a primitive");
			}
		}
	}
}
