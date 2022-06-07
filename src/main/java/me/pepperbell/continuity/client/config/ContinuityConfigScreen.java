package me.pepperbell.continuity.client.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ContinuityConfigScreen extends Screen {
	private final Screen parent;
	private final ContinuityConfig config;

	public ContinuityConfigScreen(Screen parent, ContinuityConfig config) {
		super(new TranslatableText(getTranslationKey("title")));
		this.parent = parent;
		this.config = config;
	}

	@Override
	protected void init() {
		addDrawableChild(createBooleanOptionButton(width / 2 - 100 - 110, height / 2 - 10 - 12, 200, 20, config.connectedTextures));
		addDrawableChild(createBooleanOptionButton(width / 2 - 100 + 110, height / 2 - 10 - 12, 200, 20, config.emissiveTextures));
		addDrawableChild(createBooleanOptionButton(width / 2 - 100 - 110, height / 2 - 10 + 12, 200, 20, config.customBlockLayers));

		addDrawableChild(new ButtonWidget(width / 2 - 100, height - 40, 200, 20, ScreenTexts.DONE, button -> close()));
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

	@Override
	public void removed() {
		config.save();
		config.onChange();
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

	private ButtonWidget createBooleanOptionButton(int x, int y, int width, int height, Option<Boolean> option) {
		String translationKey = getTranslationKey(option.getKey());
		Text text = new TranslatableText(translationKey);
		Text tooltipText = new TranslatableText(getTooltipKey(translationKey));
		return new ButtonWidget(x, y, width, height, ScreenTexts.composeToggleText(text, option.get()),
				button -> {
					boolean newValue = !option.get();
					button.setMessage(ScreenTexts.composeToggleText(text, newValue));
					option.set(newValue);
				},
				createDefaultTooltipSupplier(tooltipText)
		);
	}
}
