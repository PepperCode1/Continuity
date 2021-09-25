package me.pepperbell.continuity.api.client;

public interface CTMLoader<T extends CTMProperties> {
	CTMPropertiesFactory<T> getPropertiesFactory();

	QuadProcessorFactory<T> getProcessorFactory();

	static <T extends CTMProperties> CTMLoader<T> of(CTMPropertiesFactory<T> propertiesFactory, QuadProcessorFactory<T> processorFactory) {
		return new CTMLoader<T>() {
			@Override
			public CTMPropertiesFactory<T> getPropertiesFactory() {
				return propertiesFactory;
			}

			@Override
			public QuadProcessorFactory<T> getProcessorFactory() {
				return processorFactory;
			}
		};
	}
}
