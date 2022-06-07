package me.pepperbell.continuity.client.properties;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.api.client.CTMProperties;
import me.pepperbell.continuity.api.client.CTMPropertiesFactory;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.resource.InvalidIdentifierStateHolder;
import me.pepperbell.continuity.client.resource.ResourcePackUtil;
import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import me.pepperbell.continuity.client.util.BooleanState;
import me.pepperbell.continuity.client.util.MathUtil;
import me.pepperbell.continuity.client.util.TextureUtil;
import me.pepperbell.continuity.client.util.biome.BiomeHolder;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

public class BaseCTMProperties implements CTMProperties {
	public static final Identifier SPECIAL_SKIP_ID = ContinuityClient.asId("special/skip");
	public static final Identifier SPECIAL_DEFAULT_ID = ContinuityClient.asId("special/default");
	public static final SpriteIdentifier SPECIAL_SKIP_SPRITE_ID = TextureUtil.toSpriteId(SPECIAL_SKIP_ID);
	public static final SpriteIdentifier SPECIAL_DEFAULT_SPRITE_ID = TextureUtil.toSpriteId(SPECIAL_DEFAULT_ID);

	protected Properties properties;
	protected Identifier id;
	protected String packName;
	protected int packPriority;
	protected String method;

	protected Set<Identifier> matchTilesSet;
	protected Predicate<BlockState> matchBlocksPredicate;
	protected int weight = 0;
	protected List<Identifier> tiles;
	protected EnumSet<Direction> faces;
	protected Predicate<Biome> biomePredicate;
	protected IntPredicate heightPredicate;
	protected Predicate<String> blockEntityNamePredicate;

	protected boolean valid = true;
	protected Set<SpriteIdentifier> textureDependencies;
	protected List<SpriteIdentifier> spriteIds;

	public BaseCTMProperties(Properties properties, Identifier id, String packName, int packPriority, String method) {
		this.properties = properties;
		this.id = id;
		this.packName = packName;
		this.packPriority = packPriority;
		this.method = method;
	}

	@Override
	public boolean affectsTextures() {
		return matchTilesSet != null;
	}

	@Override
	public boolean affectsTexture(Identifier id) {
		if (matchTilesSet != null) {
			return matchTilesSet.contains(id);
		}
		return false;
	}

	@Override
	public boolean affectsBlockStates() {
		return matchBlocksPredicate != null;
	}

	@Override
	public boolean affectsBlockState(BlockState state) {
		if (matchBlocksPredicate != null) {
			return matchBlocksPredicate.test(state);
		}
		return false;
	}

	@Override
	public Set<SpriteIdentifier> getTextureDependencies() {
		if (textureDependencies == null) {
			resolveTiles();
		}
		return textureDependencies;
	}

	/*
	-1 this < o
	0 this == o
	1 this > o
	 */
	@Override
	public int compareTo(@NotNull CTMProperties o) {
		if (o instanceof BaseCTMProperties o1) {
			int c = MathUtil.signum(weight - o1.weight);
			if (c != 0) {
				return c;
			}
		}
		if (affectsTextures() && !o.affectsTextures()) {
			return 1;
		}
		if (!affectsTextures() && o.affectsTextures()) {
			return -1;
		}
		if (o instanceof BaseCTMProperties o1) {
			int c = MathUtil.signum(packPriority - o1.packPriority);
			if (c != 0) {
				return c;
			}
			return o1.getId().compareTo(getId());
		}
		return 0;
	}

	public void init() {
		parseMatchTiles();
		parseMatchBlocks();
		detectMatches();
		validateMatches();
		parseWeight();
		parseTiles();
		validateTiles();
		parseFaces();
		parseBiomes();
		parseHeights();
		parseName();
		parseResourceCondition();
	}

	protected void parseMatchTiles() {
		matchTilesSet = PropertiesParsingHelper.parseMatchTiles(properties, "matchTiles", id, packName, true);
	}

	protected void parseMatchBlocks() {
		matchBlocksPredicate = PropertiesParsingHelper.parseBlockStates(properties, "matchBlocks", id, packName, true);
	}

	protected void detectMatches() {
		String baseName = FilenameUtils.getBaseName(id.getPath());
		if (matchBlocksPredicate == null) {
			if (baseName.startsWith("block_")) {
				try {
					Identifier id = new Identifier(baseName.substring(6));
					Block block = Registry.BLOCK.get(id);
					if (block != Blocks.AIR) {
						matchBlocksPredicate = state -> state.getBlock() == block;
					}
				} catch (InvalidIdentifierException e) {
					//
				}
			}
		}
	}

	protected void validateMatches() {
		if (matchTilesSet == null && matchBlocksPredicate == null) {
			ContinuityClient.LOGGER.error("No tile or block matches provided in file '" + id + "' in pack '" + packName + "'");
			valid = false;
		}
	}

	protected void parseWeight() {
		String weightStr = properties.getProperty("weight");
		if (weightStr != null) {
			weightStr = weightStr.trim();
			try {
				weight = Integer.parseInt(weightStr);
			} catch (NumberFormatException e) {
				ContinuityClient.LOGGER.warn("Invalid 'weight' value '" + weightStr + "' in file '" + id + "' in pack '" + packName + "'");
			}
		}
	}

	protected void parseTiles() {
		String tilesStr = properties.getProperty("tiles");
		if (tilesStr != null) {
			tilesStr = tilesStr.trim();
			String[] tileStrs = tilesStr.split("[ ,]");
			if (tileStrs.length != 0) {
				String basePath = FilenameUtils.getPath(id.getPath());
				ImmutableList.Builder<Identifier> listBuilder = ImmutableList.builder();
				BooleanState invalidIdentifierState = InvalidIdentifierStateHolder.get();
				invalidIdentifierState.enable();
				for (int i = 0; i < tileStrs.length; i++) {
					String tileStr = tileStrs[i];
					if (!tileStr.isEmpty()) {
						if (tileStr.endsWith("<skip>") || tileStr.endsWith("<skip>.png")) {
							listBuilder.add(SPECIAL_SKIP_ID);
							continue;
						} else if (tileStr.endsWith("<default>") || tileStr.endsWith("<default>.png")) {
							listBuilder.add(SPECIAL_DEFAULT_ID);
							continue;
						}
						if (tileStr.contains("-")) {
							String[] tileStrRange = tileStr.split("-");
							if (tileStrRange.length == 2) {
								try {
									int min = Integer.parseInt(tileStrRange[0]);
									int max = Integer.parseInt(tileStrRange[1]);
									if (min <= max) {
										for (int t = min; t <= max; t++) {
											listBuilder.add(new Identifier(id.getNamespace(), basePath + t + ".png"));
										}
										continue;
									}
								} catch (NumberFormatException e) {
									//
								}
								ContinuityClient.LOGGER.warn("Invalid 'tiles' element '" + tileStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'");
							}
						} else {
							String[] parts = tileStr.split(":", 2);
							if (parts.length != 0) {
								String namespace;
								String path;
								if (parts.length > 1) {
									namespace = parts[0];
									path = parts[1];
								} else {
									namespace = id.getNamespace();
									path = parts[0];
								}
								if (!path.endsWith(".png")) {
									path = path + ".png";
								}
								if (path.startsWith("./")) {
									path = basePath + path.substring(2);
								} else if (path.startsWith("~/")) {
									path = "optifine/" + path.substring(2);
								} else if (path.startsWith("/")) {
									path = "optifine/" + path.substring(1);
								} else if (!path.startsWith("textures/") && !path.startsWith("optifine/")) {
									path = basePath + path;
								}
								listBuilder.add(new Identifier(namespace, path));
							}
						}
					}
				}
				invalidIdentifierState.disable();
				ImmutableList<Identifier> list = listBuilder.build();
				if (!list.isEmpty()) {
					tiles = list;
				}
			}
		}
	}

	protected void validateTiles() {
		if (tiles == null) {
			ContinuityClient.LOGGER.error("No tiles provided in file '" + id + "' in pack '" + packName + "'");
			valid = false;
		}
	}

	protected void parseFaces() {
		String facesStr = properties.getProperty("faces");
		if (facesStr != null) {
			facesStr = facesStr.trim();
			String[] faceStrs = facesStr.split("[ ,]");
			if (faceStrs.length != 0) {
				for (int i = 0; i < faceStrs.length; i++) {
					String faceStr = faceStrs[i];
					if (!faceStr.isEmpty()) {
						faceStr = faceStr.toUpperCase(Locale.ROOT);
						if (faceStr.equals("BOTTOM")) {
							faceStr = "DOWN";
						} else if (faceStr.equals("TOP")) {
							faceStr = "UP";
						}
						try {
							Direction direction = Direction.valueOf(faceStr);
							if (faces == null) {
								faces = EnumSet.noneOf(Direction.class);
							}
							faces.add(direction);
							continue;
						} catch (IllegalArgumentException e) {
							//
						}
						if (faceStr.equals("SIDES")) {
							if (faces == null) {
								faces = EnumSet.noneOf(Direction.class);
							}
							Iterators.addAll(faces, Direction.Type.HORIZONTAL.iterator());
							continue;
						} else if (faceStr.equals("ALL")) {
							faces = null;
							return;
						}
						ContinuityClient.LOGGER.warn("Unknown 'faces' element '" + faceStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'");
					}
				}
			}
		}
	}

	protected void parseBiomes() {
		String biomesStr = properties.getProperty("biomes");
		if (biomesStr != null) {
			biomesStr = biomesStr.trim();
			boolean negate = false;
			if (biomesStr.charAt(0) == '!') {
				negate = true;
				biomesStr = biomesStr.substring(1);
			}
			String[] biomeStrs = biomesStr.split(" ");
			if (biomeStrs.length != 0) {
				ImmutableSet.Builder<BiomeHolder> setBuilder = ImmutableSet.builder();
				for (int i = 0; i < biomeStrs.length; i++) {
					String biomeStr = biomeStrs[i];
					if (!biomeStr.isEmpty()) {
						try {
							Identifier biomeId = new Identifier(biomeStr.toLowerCase(Locale.ROOT));
							setBuilder.add(BiomeHolderManager.getOrCreateHolder(biomeId));
						} catch (InvalidIdentifierException e) {
							ContinuityClient.LOGGER.warn("Invalid 'biomes' element '" + biomeStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'", e);
						}
					}
				}
				ImmutableSet<BiomeHolder> set = setBuilder.build();
				if (!set.isEmpty()) {
					biomePredicate = biome -> {
						for (BiomeHolder holder : set) {
							if (holder.getBiome() == biome) {
								return true;
							}
						}
						return false;
					};
					if (negate) {
						biomePredicate = biomePredicate.negate();
					}
				}
			}
		}
	}

	protected void parseHeights() {
		String heightsStr = properties.getProperty("heights");
		if (heightsStr != null) {
			heightsStr = heightsStr.trim();
			String[] heightStrs = heightsStr.split("[ ,]");
			if (heightStrs.length != 0) {
				ImmutableList.Builder<IntPredicate> predicateListBuilder = ImmutableList.builder();
				for (int i = 0; i < heightStrs.length; i++) {
					String heightStr = heightStrs[i];
					if (!heightStr.isEmpty()) {
						String[] parts = heightStr.split("\\.\\.", 2);
						if (parts.length > 1) {
							try {
								if (parts[1].isEmpty()) {
									int min = Integer.parseInt(parts[0]);
									predicateListBuilder.add(y -> y >= min);
								} else if (parts[0].isEmpty()) {
									int max = Integer.parseInt(parts[1]);
									predicateListBuilder.add(y -> y <= max);
								} else {
									int min = Integer.parseInt(parts[0]);
									int max = Integer.parseInt(parts[1]);
									if (min < max) {
										predicateListBuilder.add(y -> y >= min && y <= max);
									} else if (min > max) {
										predicateListBuilder.add(y -> y >= max && y <= min);
									} else {
										predicateListBuilder.add(y -> y == min);
									}
								}
								continue;
							} catch (NumberFormatException e) {
								//
							}
						} else {
							String heightStr1 = heightStr.replaceAll("[()]", "");
							if (!heightStr1.isEmpty()) {
								int separatorIndex = heightStr1.indexOf('-', heightStr1.charAt(0) == '-' ? 1 : 0);
								try {
									if (separatorIndex == -1) {
										int height = Integer.parseInt(heightStr1);
										predicateListBuilder.add(y -> y == height);
									} else {
										int min = Integer.parseInt(heightStr1.substring(0, separatorIndex));
										int max = Integer.parseInt(heightStr1.substring(separatorIndex + 1));
										if (min < max) {
											predicateListBuilder.add(y -> y >= min && y <= max);
										} else if (min > max) {
											predicateListBuilder.add(y -> y >= max && y <= min);
										} else {
											predicateListBuilder.add(y -> y == min);
										}
									}
									continue;
								} catch (NumberFormatException e) {
									//
								}
							}
						}
						ContinuityClient.LOGGER.warn("Invalid 'heights' element '" + heightStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'");
					}
				}
				ImmutableList<IntPredicate> predicateList = predicateListBuilder.build();
				if (!predicateList.isEmpty()) {
					int amount = predicateList.size();
					heightPredicate = y -> {
						for (int i = 0; i < amount; i++) {
							if (predicateList.get(i).test(y)) {
								return true;
							}
						}
						return false;
					};
				}
			}
		}

		if (heightPredicate == null) {
			String minHeightStr = properties.getProperty("minHeight");
			String maxHeightStr = properties.getProperty("maxHeight");
			boolean hasMinHeight = minHeightStr != null;
			boolean hasMaxHeight = maxHeightStr != null;
			if (hasMinHeight || hasMaxHeight) {
				int min = 0;
				int max = 0;
				if (hasMinHeight) {
					minHeightStr = minHeightStr.trim();
					try {
						min = Integer.parseInt(minHeightStr);
					} catch (NumberFormatException e) {
						hasMinHeight = false;
						ContinuityClient.LOGGER.warn("Invalid 'minHeight' value '" + minHeightStr + "' in file '" + id + "' in pack '" + packName + "'");
					}
				}
				if (hasMaxHeight) {
					maxHeightStr = maxHeightStr.trim();
					try {
						max = Integer.parseInt(maxHeightStr);
					} catch (NumberFormatException e) {
						hasMaxHeight = false;
						ContinuityClient.LOGGER.warn("Invalid 'maxHeight' value '" + minHeightStr + "' in file '" + id + "' in pack '" + packName + "'");
					}
				}
				int finalMin = min;
				int finalMax = max;
				if (hasMinHeight && hasMaxHeight) {
					if (finalMin < finalMax) {
						heightPredicate = y -> y >= finalMin && y <= finalMax;
					} else if (finalMin > finalMax) {
						heightPredicate = y -> y >= finalMax && y <= finalMin;
					} else {
						heightPredicate = y -> y == finalMin;
					}
				} else if (hasMinHeight) {
					heightPredicate = y -> y >= finalMin;
				} else if (hasMaxHeight) {
					heightPredicate = y -> y <= finalMax;
				}
			}
		}
	}

	protected void parseName() {
		String nameStr = properties.getProperty("name");
		if (nameStr != null) {
			nameStr = nameStr.trim();
			nameStr = StringEscapeUtils.escapeJava(nameStr);

			boolean isPattern = false;
			boolean caseInsensitive = false;
			if (nameStr.startsWith("regex:")) {
				nameStr = nameStr.substring(6);
			} else if (nameStr.startsWith("iregex:")) {
				nameStr = nameStr.substring(7);
				caseInsensitive = true;
			} else if (nameStr.startsWith("pattern:")) {
				nameStr = nameStr.substring(8);
				isPattern = true;
			} else if (nameStr.startsWith("ipattern:")) {
				nameStr = nameStr.substring(9);
				isPattern = true;
				caseInsensitive = true;
			} else {
				blockEntityNamePredicate = nameStr::equals;
				return;
			}

			String patternStr = nameStr;
			if (isPattern) {
				patternStr = Pattern.quote(patternStr);
				patternStr = patternStr.replace("?", "\\E.\\Q");
				patternStr = patternStr.replace("*", "\\E.*\\Q");
			}
			Pattern pattern = Pattern.compile(patternStr, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
			blockEntityNamePredicate = blockEntityName -> pattern.matcher(blockEntityName).matches();
		}
	}

	protected void parseResourceCondition() {
		String conditionsStr = properties.getProperty("resourceCondition");
		if (conditionsStr != null) {
			conditionsStr = conditionsStr.trim();
			String[] conditionStrs = conditionsStr.split("\\|");
			if (conditionStrs.length != 0) {
				DefaultResourcePack defaultPack = ResourcePackUtil.getDefaultResourcePack();
				BooleanState invalidIdentifierState = InvalidIdentifierStateHolder.get();
				invalidIdentifierState.enable();
				for (int i = 0; i < conditionStrs.length; i++) {
					String conditionStr = conditionStrs[i];
					if (!conditionStr.isEmpty()) {
						String[] parts = conditionStr.split("@", 2);
						if (parts.length != 0) {
							Identifier resourceId = new Identifier(parts[0]);
							String packStr;
							if (parts.length > 1) {
								packStr = parts[1];
							} else {
								packStr = null;
							}

							if (packStr == null || packStr.equals("default")) {
								ResourcePack pack = ResourcePackUtil.getProvidingResourcePack(resourceId);
								if (pack != null && pack != defaultPack) {
									valid = false;
									break;
								}
							} else if (packStr.equals("programmer_art")) {
								ResourcePack pack = ResourcePackUtil.getProvidingResourcePack(resourceId);
								if (pack != null && !pack.getName().equals("Programmer Art")) {
									valid = false;
									break;
								}
							} else {
								ContinuityClient.LOGGER.warn("Invalid pack '" + packStr + "' in 'resourceCondition' element '" + conditionStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'");
							}
						}
					}
				}
				invalidIdentifierState.disable();
			}
		}
	}

	protected boolean isValid() {
		return valid;
	}

	protected void resolveTiles() {
		textureDependencies = new ObjectOpenHashSet<>();
		spriteIds = new ObjectArrayList<>();
		ResourceRedirectHandler redirectHandler = ResourceRedirectHandler.get();
		for (Identifier tile : tiles) {
			SpriteIdentifier spriteId;
			if (tile.equals(SPECIAL_SKIP_ID)) {
				spriteId = SPECIAL_SKIP_SPRITE_ID;
			} else if (tile.equals(SPECIAL_DEFAULT_ID)) {
				spriteId = SPECIAL_DEFAULT_SPRITE_ID;
			} else {
				String namespace = tile.getNamespace();
				String path = tile.getPath();
				if (path.startsWith("textures/")) {
					path = path.substring(9);
					if (path.endsWith(".png")) {
						path = path.substring(0, path.length() - 4);
					}

					spriteId = TextureUtil.toSpriteId(new Identifier(namespace, path));
					textureDependencies.add(spriteId);
				} else if (redirectHandler != null) {
					path = redirectHandler.getSourceSpritePath(path);

					spriteId = TextureUtil.toSpriteId(new Identifier(namespace, path));
					textureDependencies.add(spriteId);
				} else {
					spriteId = TextureUtil.MISSING_SPRITE_ID;
				}
			}
			spriteIds.add(spriteId);
		}
	}

	public Identifier getId() {
		return id;
	}

	public String getPackName() {
		return packName;
	}

	public String getMethod() {
		return method;
	}

	public Set<Identifier> getMatchTilesSet() {
		return matchTilesSet;
	}

	public Predicate<BlockState> getMatchBlocksPredicate() {
		return matchBlocksPredicate;
	}

	public int getWeight() {
		return weight;
	}

	public EnumSet<Direction> getFaces() {
		return faces;
	}

	public Predicate<Biome> getBiomePredicate() {
		return biomePredicate;
	}

	public IntPredicate getHeightPredicate() {
		return heightPredicate;
	}

	public Predicate<String> getBlockEntityNamePredicate() {
		return blockEntityNamePredicate;
	}

	public List<SpriteIdentifier> getSpriteIds() {
		if (spriteIds == null) {
			resolveTiles();
		}
		return spriteIds;
	}

	public static <T extends BaseCTMProperties> CTMPropertiesFactory<T> wrapFactory(CTMPropertiesFactory<T> factory) {
		return (properties, id, packName, packPriority, method) -> {
			T ctmProperties = factory.createProperties(properties, id, packName, packPriority, method);
			if (ctmProperties == null) {
				return null;
			}
			ctmProperties.init();
			if (ctmProperties.isValid()) {
				return ctmProperties;
			}
			return null;
		};
	}
}
