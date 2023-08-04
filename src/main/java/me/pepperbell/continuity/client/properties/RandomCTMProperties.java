package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.Symmetry;
import me.pepperbell.continuity.client.util.RandomIndexProvider;
import net.minecraft.util.Identifier;

public class RandomCTMProperties extends BaseCTMProperties {
	protected RandomIndexProvider.Factory indexProviderFactory = RandomIndexProvider.UnweightedFactory.INSTANCE;
	protected int randomLoops = 0;
	protected Symmetry symmetry = Symmetry.NONE;
	protected boolean linked = false;

	public RandomCTMProperties(Properties properties, Identifier id, String packName, int packPriority, String method) {
		super(properties, id, packName, packPriority, method);
	}

	@Override
	public void init() {
		super.init();
		parseWeights();
		parseRandomLoops();
		parseSymmetry();
		parseLinked();
	}

	protected void parseWeights() {
		String weightsStr = properties.getProperty("weights");
		if (weightsStr == null) {
			return;
		}

		String[] weightStrs = weightsStr.trim().split("[ ,]");
		if (weightStrs.length != 0) {
			IntList weights = new IntArrayList();

			for (int i = 0; i < weightStrs.length; i++) {
				String weightStr = weightStrs[i];
				if (weightStr.isEmpty()) {
					continue;
				}

				String[] parts = weightStr.split("-", 2);
				try {
					if (parts.length == 2) {
						int min = Integer.parseInt(parts[0]);
						int max = Integer.parseInt(parts[1]);
						if (min > 0 && max > 0 && max >= min) {
							for (int weight = min; weight <= max; weight++) {
								weights.add(weight);
							}
							continue;
						}
					} else if (parts.length == 1) {
						int weight = Integer.parseInt(parts[0]);
						if (weight > 0) {
							weights.add(weight);
							continue;
						}
					}
				} catch (NumberFormatException e) {
					//
				}
				ContinuityClient.LOGGER.warn("Invalid 'weights' element '" + weightStr + "' at index '" + i + "' in file '" + id + "' in pack '" + packName + "'");
			}

			if (!weights.isEmpty()) {
				indexProviderFactory = new RandomIndexProvider.WeightedFactory(weights.toIntArray());
			}
		}
	}

	protected void parseRandomLoops() {
		String randomLoopsStr = properties.getProperty("randomLoops");
		if (randomLoopsStr == null) {
			return;
		}

		try {
			int randomLoops = Integer.parseInt(randomLoopsStr.trim());
			if (randomLoops >= 0 && randomLoops <= 9) {
				this.randomLoops = randomLoops;
				return;
			}
		} catch (NumberFormatException e) {
			//
		}
		ContinuityClient.LOGGER.warn("Invalid 'randomLoops' value '" + randomLoopsStr + "' in file '" + id + "' in pack '" + packName + "'");
	}

	protected void parseSymmetry() {
		Symmetry symmetry = PropertiesParsingHelper.parseSymmetry(properties, "symmetry", id, packName);
		if (symmetry != null) {
			this.symmetry = symmetry;
		}
	}

	protected void parseLinked() {
		String linkedStr = properties.getProperty("linked");
		if (linkedStr == null) {
			return;
		}

		linked = Boolean.parseBoolean(linkedStr.trim());
	}

	public RandomIndexProvider.Factory getIndexProviderFactory() {
		return indexProviderFactory;
	}

	public int getRandomLoops() {
		return randomLoops;
	}

	public Symmetry getSymmetry() {
		return symmetry;
	}

	public boolean getLinked() {
		return linked;
	}
}
