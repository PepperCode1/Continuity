package me.pepperbell.continuity.client.processor;

import net.minecraft.client.texture.Sprite;

public abstract class ConnectingQuadProcessor extends AbstractQuadProcessor {
	protected ConnectionPredicate connectionPredicate;

	public ConnectingQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate) {
		super(sprites, processingPredicate);
		this.connectionPredicate = connectionPredicate;
	}
}
