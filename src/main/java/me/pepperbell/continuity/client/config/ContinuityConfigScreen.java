package me.pepperbell.continuity.client.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ContinuityConfigScreen extends Screen {
	private final Screen parent;
	private final ContinuityConfig config;

	private List<Value<?>> values;

	public ContinuityConfigScreen(Screen parent, ContinuityConfig config) {
		super(Component.translatable(getTranslationKey("title")));
		this.parent = parent;
		this.config = config;
	}

	@Override
	protected void init() {
		Value<Boolean> connectedTextures = Value.of(config.connectedTextures, Value.Flag.RELOAD_LEVEL_EXTRACTOR);
		Value<Boolean> emissiveTextures = Value.of(config.emissiveTextures, Value.Flag.RELOAD_LEVEL_EXTRACTOR);

		values = List.of(connectedTextures, emissiveTextures);

		addRenderableWidget(startBooleanValueButton(connectedTextures)
				.bounds(width / 2 - 100 - 110, height / 2 - 10, 200, 20)
				.build());
		addRenderableWidget(startBooleanValueButton(emissiveTextures)
				.bounds(width / 2 - 100 + 110, height / 2 - 10, 200, 20)
				.build());

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
				button -> {
					saveValues();
					onClose();
				})
				.bounds(width / 2 - 75 - 79, height - 40, 150, 20)
				.build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
				.bounds(width / 2 - 75 + 79, height - 40, 150, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, title, width / 2, 30, 0xFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
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

	static String getTranslationKey(String optionKey) {
		return "options.continuity." + optionKey;
	}

	static String getTooltipKey(String translationKey) {
		return translationKey + ".tooltip";
	}

	private Button.Builder startBooleanValueButton(Value<Boolean> value) {
		String translationKey = getTranslationKey(value.getOption().getKey());
		Component component = Component.translatable(translationKey);
		Component tooltipComponent = Component.translatable(getTooltipKey(translationKey));

		return Button.builder(CommonComponents.optionNameValue(component, CommonComponents.optionStatus(value.get())),
				button -> {
					boolean newValue = !value.get();
					value.set(newValue);
					Component valueText = CommonComponents.optionStatus(newValue);
					if (value.isChanged()) {
						valueText = valueText.copy().withStyle(style -> style.withBold(true));
					}
					button.setMessage(CommonComponents.optionNameValue(component, valueText));
				})
				.tooltip(Tooltip.create(tooltipComponent));
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
			RELOAD_LEVEL_EXTRACTOR {
				@Override
				public void onSave() {
					Minecraft.getInstance().levelExtractor.allChanged();
				}
			};

			public abstract void onSave();
		}
	}
}
