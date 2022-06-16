package me.pepperbell.continuity.client.properties;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.Symmetry;
import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;

public final class PropertiesParsingHelper {
	@Nullable
	public static ImmutableSet<Identifier> parseMatchTiles(Properties properties, String propertyKey, Identifier fileLocation, String packName, boolean nullIfEmpty) {
		String matchTilesStr = properties.getProperty(propertyKey);
		if (matchTilesStr != null) {
			matchTilesStr = matchTilesStr.trim();
			String[] matchTileStrs = matchTilesStr.split(" ");
			if (matchTileStrs.length != 0) {
				String basePath = FilenameUtils.getPath(fileLocation.getPath());
				ResourceRedirectHandler redirectHandler = ResourceRedirectHandler.get();
				ImmutableSet.Builder<Identifier> setBuilder = ImmutableSet.builder();

				for (int i = 0; i < matchTileStrs.length; i++) {
					String matchTileStr = matchTileStrs[i];
					if (!matchTileStr.isEmpty()) {
						String[] parts = matchTileStr.split(":", 2);
						if (parts.length != 0) {
							String namespace;
							String path;
							if (parts.length > 1) {
								namespace = parts[0];
								path = parts[1];
							} else {
								namespace = fileLocation.getNamespace();
								path = parts[0];
							}

							if (path.endsWith(".png")) {
								path = path.substring(0, path.length() - 4);
							}
							if (path.startsWith("./")) {
								path = basePath + path.substring(2);
							} else if (path.startsWith("~/")) {
								path = "optifine/" + path.substring(2);
							} else if (path.startsWith("/")) {
								path = "optifine/" + path.substring(1);
							} else if (!path.contains("/")) {
								namespace = null;
								path = "textures/block/" + path;
							}
							if (path.startsWith("textures/")) {
								path = path.substring(9);
							} else if (path.startsWith("optifine/")) {
								if (redirectHandler == null) {
									continue;
								}
								path = redirectHandler.getSourceSpritePath(path + ".png");
							}

							try {
								setBuilder.add(new Identifier(namespace, path));
								continue;
							} catch (InvalidIdentifierException e) {
								//
							}
						}
						ContinuityClient.LOGGER.warn("Invalid '" + propertyKey + "' element '" + matchTileStr + "' at index " + i + " in file '" + fileLocation + "' in pack '" + packName + "'");
					}
				}

				ImmutableSet<Identifier> set = setBuilder.build();
				if (!set.isEmpty() || !nullIfEmpty) {
					return set;
				}
			}
		}
		return null;
	}

	@Nullable
	public static Predicate<BlockState> parseBlockStates(Properties properties, String propertyKey, Identifier fileLocation, String packName, boolean nullIfEmpty) {
		String blockStatesStr = properties.getProperty(propertyKey);
		if (blockStatesStr != null) {
			blockStatesStr = blockStatesStr.trim();
			String[] blockStateStrs = blockStatesStr.split(" ");
			if (blockStateStrs.length != 0) {
				ImmutableList.Builder<Predicate<BlockState>> predicateListBuilder = ImmutableList.builder();

				Block:
				for (int i = 0; i < blockStateStrs.length; i++) {
					String blockStateStr = blockStateStrs[i].trim();
					if (!blockStateStr.isEmpty()) {
						String[] parts = blockStateStr.split(":", 3);
						if (parts.length != 0) {
							Identifier blockId;
							int startIndex;
							try {
								if (parts.length == 1 || parts[1].contains("=")) {
									blockId = new Identifier(parts[0]);
									startIndex = 1;
								} else {
									blockId = new Identifier(parts[0], parts[1]);
									startIndex = 2;
								}
							} catch (InvalidIdentifierException e) {
								ContinuityClient.LOGGER.warn("Invalid '" + propertyKey + "' element '" + blockStateStr + "' at index " + i + " in file '" + fileLocation + "' in pack '" + packName + "'", e);
								continue;
							}

							Block block = Registry.BLOCK.get(blockId);
							if (block != Blocks.AIR) {
								if (parts.length > startIndex) {
									ImmutableMap.Builder<Property<?>, ImmutableList<Comparable<?>>> propertyMapBuilder = ImmutableMap.builder();

									for (int j = startIndex; j < parts.length; j++) {
										String part = parts[j];
										String[] propertyParts = part.split("=", 2);
										if (propertyParts.length == 2) {
											String propertyName = propertyParts[0];
											Property<?> property = block.getStateManager().getProperty(propertyName);
											if (property != null) {
												String propertyValuesStr = propertyParts[1];
												String[] propertyValueStrs = propertyValuesStr.split(",");
												if (propertyValueStrs.length != 0) {
													ImmutableList.Builder<Comparable<?>> valuesListBuilder = ImmutableList.builder();

													for (String propertyValueStr : propertyValueStrs) {
														Optional<? extends Comparable<?>> optional = property.parse(propertyValueStr);
														if (optional.isPresent()) {
															valuesListBuilder.add(optional.get());
														} else {
															ContinuityClient.LOGGER.warn("Invalid block property value '" + propertyValueStr + "' for property '" + propertyName + "' for block '" + blockId + "' in '" + propertyKey + "' element '" + blockStateStr + "' at index " + i + " in file '" + fileLocation + "' in pack '" + packName + "'");
															continue Block;
														}
													}

													ImmutableList<Comparable<?>> valuesList = valuesListBuilder.build();
													if (!valuesList.isEmpty()) {
														propertyMapBuilder.put(property, valuesList);
													}
												}
											} else {
												ContinuityClient.LOGGER.warn("Unknown block property '" + propertyName + "' for block '" + blockId + "' in '" + propertyKey + "' element '" + blockStateStr + "' at index " + i + " in file '" + fileLocation + "' in pack '" + packName + "'");
												continue Block;
											}
										} else {
											ContinuityClient.LOGGER.warn("Invalid block property definition for block '" + blockId + "' in '" + propertyKey + "' element '" + blockStateStr + "' at index " + i + " in file '" + fileLocation + "' in pack '" + packName + "'");
											continue Block;
										}
									}

									ImmutableMap<Property<?>, ImmutableList<Comparable<?>>> propertyMap = propertyMapBuilder.build();
									if (!propertyMap.isEmpty()) {
										predicateListBuilder.add(state -> {
											if (state.getBlock() == block) {
												Outer:
												for (Map.Entry<Property<?>, ImmutableList<Comparable<?>>> entry : propertyMap.entrySet()) {
													Comparable<?> targetValue = state.get(entry.getKey());
													for (Comparable<?> value : entry.getValue()) {
														if (targetValue == value) {
															continue Outer;
														}
													}
													return false;
												}
												return true;
											}
											return false;
										});
										continue;
									}
								}
								predicateListBuilder.add(state -> state.getBlock() == block);
							} else {
								ContinuityClient.LOGGER.warn("Unknown block '" + blockId + "' in '" + propertyKey + "' element '" + blockStateStr + "' at index " + i + " in file '" + fileLocation + "' in pack '" + packName + "'");
							}
						}
					}
				}

				ImmutableList<Predicate<BlockState>> predicateList = predicateListBuilder.build();
				if (!predicateList.isEmpty()) {
					int amount = predicateList.size();
					return state -> {
						for (int i = 0; i < amount; i++) {
							if (predicateList.get(i).test(state)) {
								return true;
							}
						}
						return false;
					};
				} else if (!nullIfEmpty) {
					return state -> false;
				}
			}
		}
		return null;
	}

	@Nullable
	public static Symmetry parseSymmetry(Properties properties, String propertyKey, Identifier fileLocation, String packName) {
		String symmetryStr = properties.getProperty(propertyKey);
		if (symmetryStr != null) {
			try {
				return Symmetry.valueOf(symmetryStr.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				ContinuityClient.LOGGER.warn("Unknown '" + propertyKey + "' value '" + symmetryStr + "' in file '" + fileLocation + "' in pack '" + packName + "'");
			}
		}
		return null;
	}
}
