package me.pepperbell.continuity.client.properties;

import me.pepperbell.continuity.api.client.CTMPropertiesFactory;
import me.pepperbell.continuity.client.ContinuityClient;

public interface TileAmountValidator<T extends BaseCTMProperties> {
	boolean validateTileAmount(int amount, T properties);

	static <T extends BaseCTMProperties> CTMPropertiesFactory<T> wrapFactory(CTMPropertiesFactory<T> factory, TileAmountValidator<T> validator) {
		return (properties, id, packName, packPriority, method) -> {
			T ctmProperties = factory.createProperties(properties, id, packName, packPriority, method);
			if (ctmProperties == null) {
				return null;
			}
			if (validator.validateTileAmount(ctmProperties.getTileAmount(), ctmProperties)) {
				return ctmProperties;
			}
			return null;
		};
	}

	class Exactly<T extends BaseCTMProperties> implements TileAmountValidator<T> {
		protected final int targetAmount;

		public Exactly(int targetAmount) {
			this.targetAmount = targetAmount;
		}

		@Override
		public boolean validateTileAmount(int amount, T properties) {
			if (amount == targetAmount) {
				return true;
			}
			ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires exactly " + targetAmount + " tiles but " + amount + " were provided in file '" + properties.getId() + "' in pack '" + properties.getPackName() + "'");
			return false;
		}
	}

	class AtLeast<T extends BaseCTMProperties> implements TileAmountValidator<T> {
		protected final int targetAmount;

		public AtLeast(int targetAmount) {
			this.targetAmount = targetAmount;
		}

		@Override
		public boolean validateTileAmount(int amount, T properties) {
			if (amount >= targetAmount) {
				return true;
			}
			ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires at least " + targetAmount + " tiles but only " + amount + " were provided in file '" + properties.getId() + "' in pack '" + properties.getPackName() + "'");
			return false;
		}
	}
}
