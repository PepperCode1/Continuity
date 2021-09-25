package me.pepperbell.continuity.api.client;

public interface ProcessingDataProvider {
	<T> T getData(ProcessingDataKey<T> key);
}
