package me.pepperbell.continuity.client.properties;

import java.util.Locale;
import java.util.Properties;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.util.SpriteCalculator;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class ConnectingCTMProperties extends BaseCTMProperties {
	protected ConnectionPredicate connectionPredicate;

	public ConnectingCTMProperties(Properties properties, Identifier id, String packName, int packPriority, String method) {
		super(properties, id, packName, packPriority, method);
	}

	@Override
	public void init() {
		super.init();
		parseConnect();
		detectConnect();
		validateConnect();
	}

	protected void parseConnect() {
		String connectStr = properties.getProperty("connect");
		if (connectStr != null) {
			try {
				connectionPredicate = ConnectionType.valueOf(connectStr.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				//
			}
		}
	}

	protected void detectConnect() {
		if (connectionPredicate == null) {
			if (affectsBlockStates()) {
				connectionPredicate = ConnectionType.BLOCK;
			} else if (affectsTextures()) {
				connectionPredicate = ConnectionType.TILE;
			}
		}
	}

	protected void validateConnect() {
		if (connectionPredicate == null) {
			ContinuityClient.LOGGER.error("No valid connection type provided in file '" + id + "' in pack '" + packName + "'");
			valid = false;
		}
	}

	public ConnectionPredicate getConnectionPredicate() {
		return connectionPredicate;
	}

	public enum ConnectionType implements ConnectionPredicate {
		BLOCK {
			@Override
			public boolean shouldConnect(BlockRenderView blockView, BlockState state, BlockPos pos, BlockState toState, Direction face, Sprite quadSprite) {
				return state.getBlock() == toState.getBlock();
			}
		},
		TILE {
			@Override
			public boolean shouldConnect(BlockRenderView blockView, BlockState state, BlockPos pos, BlockState toState, Direction face, Sprite quadSprite) {
				if (state == toState) {
					return true;
				}
				return quadSprite == SpriteCalculator.getSprite(toState, face);
			}
		},
		MATERIAL {
			@Override
			public boolean shouldConnect(BlockRenderView blockView, BlockState state, BlockPos pos, BlockState toState, Direction face, Sprite quadSprite) {
				return state.getMaterial() == toState.getMaterial();
			}
		},
		STATE {
			@Override
			public boolean shouldConnect(BlockRenderView blockView, BlockState state, BlockPos pos, BlockState toState, Direction face, Sprite quadSprite) {
				return state == toState;
			}
		};
	}
}
