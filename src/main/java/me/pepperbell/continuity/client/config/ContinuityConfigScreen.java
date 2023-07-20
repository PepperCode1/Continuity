package me.pepperbell.continuity.client.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

public class ContinuityConfigScreen extends Screen {
	private final Screen parent;
	private final ContinuityConfig config;

	private List<Value<?>> values;

	public ContinuityConfigScreen(Screen parent, ContinuityConfig config) {
		super(Text.translatable(getTranslationKey("title")));
		this.parent = parent;
		this.config = config;
	}

	@Override
	protected void init() {
		Value<Boolean> connectedTextures = Value.of(config.connectedTextures, Value.Flag.RELOAD_WORLD_RENDERER);
		Value<Boolean> emissiveTextures = Value.of(config.emissiveTextures, Value.Flag.RELOAD_WORLD_RENDERER);
		Value<Boolean> customBlockLayers = Value.of(config.customBlockLayers, Value.Flag.RELOAD_WORLD_RENDERER);

		values = List.of(connectedTextures, emissiveTextures, customBlockLayers);

		addDrawableChild(createBooleanValueButton(connectedTextures,
				width / 2 - 100 - 110, height / 2 - 10 - 12, 200, 20));
		addDrawableChild(createBooleanValueButton(emissiveTextures,
				width / 2 - 100 + 110, height / 2 - 10 - 12, 200, 20));
		addDrawableChild(createBooleanValueButton(customBlockLayers,
				width / 2 - 100 - 110, height / 2 - 10 + 12, 200, 20));

		addDrawableChild(new ButtonWidget(width / 2 - 75 - 79, height - 40, 150, 20, ScreenTexts.DONE, button -> {
			saveValues();
			close();
		}));
		addDrawableChild(new ButtonWidget(width / 2 - 75 + 79, height - 40, 150, 20, ScreenTexts.CANCEL, button -> close()));
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		renderBackground(matrices);
		drawCenteredText(matrices, textRenderer, title, width / 2, 30, 0xFFFFFF);
		super.render(matrices, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void saveValues() {
		EnumSet<Value.Flag> flags = EnumSet.noneOf(Value.Flag.class);

		for (Value<?> value : values) {
			if (value.isChanged()) {
				value.saveToOption();
				flags.addAll(value.getFlags());
			}
		}

		config.save();

		for (Value.Flag flag : flags) {
			flag.onSave();
		}
	}

	private static String getTranslationKey(String optionKey) {
		return "options.continuity." + optionKey;
	}

	private static String getTooltipKey(String translationKey) {
		return translationKey + ".tooltip";
	}

	private ButtonWidget.TooltipSupplier createDefaultTooltipSupplier(StringVisitable text) {
		return (button, matrices, mouseX, mouseY) -> {
			renderOrderedTooltip(matrices, textRenderer.wrapLines(text, width / 100 * 100 / 2), mouseX, mouseY);
		};
	}

	private ButtonWidget createBooleanValueButton(Value<Boolean> value, int x, int y, int width, int height) {
		String translationKey = getTranslationKey(value.getOption().getKey());
		Text text = Text.translatable(translationKey);
		Text tooltipText = Text.translatable(getTooltipKey(translationKey));

		return new ButtonWidget(x, y, width, height, ScreenTexts.composeGenericOptionText(text, ScreenTexts.onOrOff(value.get())),
				button -> {
					boolean newValue = !value.get();
					value.set(newValue);
					Text valueText = ScreenTexts.onOrOff(newValue);
					if (value.isChanged()) {
						valueText = valueText.copy().styled(style -> style.withBold(true));
					}
					button.setMessage(ScreenTexts.composeGenericOptionText(text, valueText));
				},
				createDefaultTooltipSupplier(tooltipText)
		);
	}

	private static class Value<T> {
		private final Option<T> option;
		private final Set<Flag> flags;
		private final T originalValue;
		private T value;

		public Value(Option<T> option, Set<Flag> flags) {
			this.option = option;
			this.flags = flags;
			originalValue = this.option.get();
			value = originalValue;
		}

		public static <T> Value<T> of(Option<T> option, Flag... flags) {
			EnumSet<Flag> flagSet = EnumSet.noneOf(Flag.class);
			Collections.addAll(flagSet, flags);
			return new Value<>(option, flagSet);
		}

		public Option<T> getOption() {
			return option;
		}

		public Set<Flag> getFlags() {
			return flags;
		}

		public T get() {
			return value;
		}

		public void set(T value) {
			this.value = value;
		}

		public boolean isChanged() {
			return !value.equals(originalValue);
		}

		public void saveToOption() {
			option.set(value);
		}

		public enum Flag {
			RELOAD_WORLD_RENDERER {
				@Override
				public void onSave() {
					MinecraftClient.getInstance().worldRenderer.reload();
				}
			};

			public abstract void onSave();
		}
	}
}
