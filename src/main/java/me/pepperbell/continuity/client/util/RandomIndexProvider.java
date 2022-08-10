package me.pepperbell.continuity.client.util;

public interface RandomIndexProvider {
	int getRandomIndex(int random);

	interface Factory {
		RandomIndexProvider createIndexProvider(int size);
	}

	class Unweighted implements RandomIndexProvider {
		protected int size;

		public Unweighted(int size) {
			this.size = size;
		}

		@Override
		public int getRandomIndex(int random) {
			return Math.abs(random) % size;
		}
	}

	class UnweightedFactory implements Factory {
		public static final UnweightedFactory INSTANCE = new UnweightedFactory();

		@Override
		public RandomIndexProvider createIndexProvider(int size) {
			return new Unweighted(size);
		}
	}

	class Weighted implements RandomIndexProvider {
		protected int[] weights;
		protected int weightSum;
		protected int maxIndex;

		public Weighted(int[] weights, int weightSum) {
			this.weights = weights;
			this.weightSum = weightSum;
			this.maxIndex = weights.length - 1;
		}

		@Override
		public int getRandomIndex(int random) {
			int index;
			int tempWeight = Math.abs(random) % weightSum;
			for (index = 0; index < maxIndex && tempWeight >= weights[index]; index++) {
				tempWeight -= weights[index];
			}
			return index;
		}
	}

	class WeightedFactory implements Factory {
		protected int[] weights;

		public WeightedFactory(int[] weights) {
			this.weights = weights;
		}

		@Override
		public RandomIndexProvider createIndexProvider(int size) {
			int[] newWeights = new int[size];
			int copiedLength = Math.min(weights.length, newWeights.length);
			System.arraycopy(weights, 0, newWeights, 0, copiedLength);

			int weightSum = 0;
			for (int i = 0; i < copiedLength; i++) {
				weightSum += weights[i];
			}

			if (copiedLength < newWeights.length) {
				int averageWeight = weightSum / copiedLength;
				for (int i = copiedLength; i < newWeights.length; i++) {
					newWeights[i] = averageWeight;
					weightSum += averageWeight;
				}
			}

			return new Weighted(newWeights, weightSum);
		}
	}
}
