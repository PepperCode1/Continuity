package me.pepperbell.continuity.client.properties;

import java.util.Collections;
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
import me.pepperbell.continuity.client.resource.ResourcePackUtil;
import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
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

	protected static final int DIRECTION_AMOUNT = Direction.values().length;

	protected Properties properties;
	protected Identifier id;
	protected String packName;
	protected int packPriority;
	protected String method;

	protected Set<Identifier> matchTilesSet;
	protected Predicate<BlockState> matchBlocksPredicate;
	protected int weight = 0;
	protected List<Identifier> tiles = Collections.emptyList();
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
		parseFaces();
		parseBiomes();
		parseHeights();
		parseLegacyHeights();
		parseName();
		parseResourceCondition();
	}

	protected void parseMatchTiles() {
		matchTilesSet = PropertiesParsingHelper.parseMatchTiles(properties, "matchTiles", id, packName);
		if (matchTilesSet != null && matchTilesSet.isEmpty()) {
			valid = false;
		}
	}

	protected void parseMatchBlocks() {
		matchBlocksPredicate = PropertiesParsingHelper.parseBlockStates(properties, "matchBlocks", id, packName);
		if (matchBlocksPredicate == PropertiesParsingHelper.EMPTY_BLOCK_STATE_PREDICATE) {
			valid = false;
		}
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
		if (weightStr == null) {
			return;
		}

		try {
			weight = Integer.parseInt(weightStr.trim());
		} catch (NumberFormatException e) {
			ContinuityClient.LOGGER.warn("Invalid 'weight' value '" + weightStr + "' in file '" + id + "' in pack '" + packName + "'");
		}
	}

	protected void parseTiles() {
		String tilesStr = properties.getProperty("tiles");
		if (tilesStr == null) {
			ContinuityClient.LOGGER.error("No 'tiles' value provided in file '" + id + "' in pack '" + packName + "'");
			valid = false;
			return;
		}

		String[] tileStrs = tilesStr.trim().split("[ ,]");
		if (tileStrs.length != 0) {
			String basePath = FilenameUtils.getPath(id.getPath());
			ImmutableList.Builder<Identifier> listBuilder = ImmutableList.builder();

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

					String[] rangeParts = tileStr.split("-");
					if (rangeParts.length != 0) {
						if (rangeParts.length == 2) {
							try {
								int min = Integer.parseInt(rangeParts[0]);
								int max = Integer.parseInt(rangeParts[1]);
								if (min <= max) {
									for (int tile = min; tile <= max; tile++) {
										listBuilder.add(new Identifier(id.getNamespace(), basePath + tile + ".png"));
									}
									continue;
								}
							} catch (NumberFormatException | InvalidIdentifierException e) {
								//
							}
						} else if (rangeParts.length == 1) {
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

								try {
									listBuilder.add(new Identifier(namespace, path));
									continue;
								} catch (InvalidIdentifierException e) {
									//
								}
							} else {
								continue;
							}
						}
						ContinuityClient.LOGGER.warn("Invalid 'tiles' element '" + tileStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'");
					}
				}
			}

			tiles = listBuilder.build();
		}
	}

	protected void parseFaces() {
		String facesStr = properties.getProperty("faces");
		if (facesStr == null) {
			return;
		}

		String[] faceStrs = facesStr.trim().split("[ ,]");
		if (faceStrs.length != 0) {
			faces = EnumSet.noneOf(Direction.class);

			for (int i = 0; i < faceStrs.length; i++) {
				String faceStr = faceStrs[i];
				if (!faceStr.isEmpty()) {
					String faceStr1 = faceStr.toUpperCase(Locale.ROOT);
					if (faceStr1.equals("BOTTOM")) {
						faces.add(Direction.DOWN);
					} else if (faceStr1.equals("TOP")) {
						faces.add(Direction.UP);
					} else if (faceStr1.equals("SIDES")) {
						Iterators.addAll(faces, Direction.Type.HORIZONTAL.iterator());
					} else if (faceStr1.equals("ALL")) {
						faces = null;
						return;
					} else {
						try {
							faces.add(Direction.valueOf(faceStr1));
						} catch (IllegalArgumentException e) {
							ContinuityClient.LOGGER.warn("Unknown 'faces' element '" + faceStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'");
						}
					}
				}
			}

			if (faces.isEmpty()) {
				valid = false;
			} else if (faces.size() == DIRECTION_AMOUNT) {
				faces = null;
			}
		} else {
			valid = false;
		}
	}

	protected void parseBiomes() {
		String biomesStr = properties.getProperty("biomes");
		if (biomesStr == null) {
			return;
		}

		biomesStr = biomesStr.trim();
		if (!biomesStr.isEmpty()) {
			boolean negate = false;
			if (biomesStr.charAt(0) == '!') {
				negate = true;
				biomesStr = biomesStr.substring(1);
			}

			String[] biomeStrs = biomesStr.split(" ");
			if (biomeStrs.length != 0) {
				ImmutableSet.Builder<BiomeHolder> biomeSetBuilder = ImmutableSet.builder();

				for (int i = 0; i < biomeStrs.length; i++) {
					String biomeStr = biomeStrs[i];
					if (!biomeStr.isEmpty()) {
						try {
							Identifier biomeId = new Identifier(biomeStr.toLowerCase(Locale.ROOT));
							biomeSetBuilder.add(BiomeHolderManager.getOrCreateHolder(biomeId));
						} catch (InvalidIdentifierException e) {
							ContinuityClient.LOGGER.warn("Invalid 'biomes' element '" + biomeStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'", e);
						}
					}
				}

				ImmutableSet<BiomeHolder> biomeSet = biomeSetBuilder.build();
				if (!biomeSet.isEmpty()) {
					BiomeHolder[] biomeArray = biomeSet.toArray(BiomeHolder[]::new);
					biomePredicate = biome -> {
						for (BiomeHolder holder : biomeArray) {
							if (holder.getBiome() == biome) {
								return true;
							}
						}
						return false;
					};
					if (negate) {
						biomePredicate = biomePredicate.negate();
					}
				} else {
					if (!negate) {
						valid = false;
					}
				}
			} else {
				if (!negate) {
					valid = false;
				}
			}
		} else {
			valid = false;
		}
	}

	protected void parseHeights() {
		String heightsStr = properties.getProperty("heights");
		if (heightsStr == null) {
			return;
		}

		String[] heightStrs = heightsStr.trim().split("[ ,]");
		if (heightStrs.length != 0) {
			ImmutableList.Builder<IntPredicate> predicateListBuilder = ImmutableList.builder();

			for (int i = 0; i < heightStrs.length; i++) {
				String heightStr = heightStrs[i];
				if (!heightStr.isEmpty()) {
					String[] parts = heightStr.split("\\.\\.", 2);
					if (parts.length != 0) {
						if (parts.length == 2) {
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
						} else if (parts.length == 1) {
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
			}

			ImmutableList<IntPredicate> predicateList = predicateListBuilder.build();
			if (!predicateList.isEmpty()) {
				IntPredicate[] predicateArray = predicateList.toArray(IntPredicate[]::new);
				heightPredicate = y -> {
					for (IntPredicate predicate : predicateArray) {
						if (predicate.test(y)) {
							return true;
						}
					}
					return false;
				};
			} else {
				valid = false;
			}
		} else {
			valid = false;
		}
	}

	protected void parseLegacyHeights() {
		if (heightPredicate == null) {
			String minHeightStr = properties.getProperty("minHeight");
			String maxHeightStr = properties.getProperty("maxHeight");
			boolean hasMinHeight = minHeightStr != null;
			boolean hasMaxHeight = maxHeightStr != null;
			if (hasMinHeight || hasMaxHeight) {
				int min = 0;
				int max = 0;
				if (hasMinHeight) {
					try {
						min = Integer.parseInt(minHeightStr.trim());
					} catch (NumberFormatException e) {
						hasMinHeight = false;
						ContinuityClient.LOGGER.warn("Invalid 'minHeight' value '" + minHeightStr + "' in file '" + id + "' in pack '" + packName + "'");
					}
				}
				if (hasMaxHeight) {
					try {
						max = Integer.parseInt(maxHeightStr.trim());
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
		if (nameStr == null) {
			return;
		}

		nameStr = StringEscapeUtils.escapeJava(nameStr.trim());

		boolean isPattern;
		boolean caseInsensitive;
		if (nameStr.startsWith("regex:")) {
			nameStr = nameStr.substring(6);
			isPattern = false;
			caseInsensitive = false;
		} else if (nameStr.startsWith("iregex:")) {
			nameStr = nameStr.substring(7);
			isPattern = false;
			caseInsensitive = true;
		} else if (nameStr.startsWith("pattern:")) {
			nameStr = nameStr.substring(8);
			isPattern = true;
			caseInsensitive = false;
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

	protected void parseResourceCondition() {
		String conditionsStr = properties.getProperty("resourceCondition");
		if (conditionsStr == null) {
			return;
		}

		String[] conditionStrs = conditionsStr.trim().split("\\|");
		if (conditionStrs.length != 0) {
			DefaultResourcePack defaultPack = ResourcePackUtil.getDefaultResourcePack();

			for (int i = 0; i < conditionStrs.length; i++) {
				String conditionStr = conditionStrs[i];
				if (!conditionStr.isEmpty()) {
					String[] parts = conditionStr.split("@", 2);
					if (parts.length != 0) {
						String resourceStr = parts[0];
						Identifier resourceId;
						try {
							resourceId = new Identifier(resourceStr);
						} catch (InvalidIdentifierException e) {
							ContinuityClient.LOGGER.warn("Invalid resource '" + resourceStr + "' in 'resourceCondition' element '" + conditionStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'");
							continue;
						}

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
							ContinuityClient.LOGGER.warn("Unknown pack '" + packStr + "' in 'resourceCondition' element '" + conditionStr + "' at index " + i + " in file '" + id + "' in pack '" + packName + "'");
						}
					}
				}
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
