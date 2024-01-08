package net.uhb217.glowingentity.mixin.client;

import io.github.cottonmc.cotton.gui.widget.WLabel;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.network.packet.c2s.play.UpdateDifficultyLockC2SPacket;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import net.uhb217.glowingentity.gui.TestGUI;
import net.uhb217.glowingentity.gui.TestScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public abstract class GameMenuScreenMixin extends Screen{
    protected GameMenuScreenMixin(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }
    @Shadow @Final private GameOptions settings;
    @Shadow private CyclingButtonWidget<Difficulty> difficultyButton;
    @Shadow private LockButtonWidget lockDifficultyButton;
    @Shadow private final Screen parent;
    private static final Text SKIN_CUSTOMIZATION_TEXT = Text.translatable("options.skinCustomisation");
    private static final Text SOUNDS_TEXT = Text.translatable("options.sounds");
    private static final Text VIDEO_TEXT = Text.translatable("options.video");
    private static final Text CONTROL_TEXT = Text.translatable("options.controls");
    private static final Text LANGUAGE_TEXT = Text.translatable("options.language");
    private static final Text CHAT_TEXT = Text.translatable("options.chat");
    private static final Text RESOURCE_PACK_TEXT = Text.translatable("options.resourcepack");
    private static final Text ACCESSIBILITY_TEXT = Text.translatable("options.accessibility");
    private static final Text TELEMETRY_TEXT = Text.translatable("options.telemetry");
    private static final Text CREDITS_AND_ATTRIBUTION_TEXT = Text.translatable("options.credits_and_attribution");

    private ButtonWidget createButton(Text message, Supplier<Screen> screenSupplier) {
        return ButtonWidget.builder(message, button -> this.client.setScreen((Screen)screenSupplier.get())).build();
    }
    private Widget createTopRightButton() {
        if (this.client.world != null && this.client.isIntegratedServerRunning()) {
            this.difficultyButton = OptionsScreen.createDifficultyButtonWidget(0, 0, "options.difficulty", this.client);
            if (!this.client.world.getLevelProperties().isHardcore()) {
                this.lockDifficultyButton = new LockButtonWidget(
                        0,
                        0,
                        button -> this.client
                                .setScreen(
                                        new ConfirmScreen(
                                                this::lockDifficulty,
                                                Text.translatable("difficulty.lock.title"),
                                                Text.translatable("difficulty.lock.question", new Object[]{this.client.world.getLevelProperties().getDifficulty().getTranslatableName()})
                                        )
                                )
                );
                this.difficultyButton.setWidth(this.difficultyButton.getWidth() - this.lockDifficultyButton.getWidth());
                this.lockDifficultyButton.setLocked(this.client.world.getLevelProperties().isDifficultyLocked());
                this.lockDifficultyButton.active = !this.lockDifficultyButton.isLocked();
                this.difficultyButton.active = !this.lockDifficultyButton.isLocked();
                AxisGridWidget axisGridWidget = new AxisGridWidget(150, 0, AxisGridWidget.DisplayAxis.HORIZONTAL);
                axisGridWidget.add(this.difficultyButton);
                axisGridWidget.add(this.lockDifficultyButton);
                return axisGridWidget;
            } else {
                this.difficultyButton.active = false;
                return this.difficultyButton;
            }
        } else {
            return ButtonWidget.builder(
                            Text.translatable("options.online"), button -> this.client.setScreen(OnlineOptionsScreen.create(this.client, this, this.settings))
                    )
                    .dimensions(this.width / 2 + 5, this.height / 6 - 12 + 24, 150, 20)
                    .build();
        }
    }
    private void lockDifficulty(boolean difficultyLocked) {
        this.client.setScreen(this);
        if (difficultyLocked && this.client.world != null) {
            this.client.getNetworkHandler().sendPacket(new UpdateDifficultyLockC2SPacket(true));
            this.lockDifficultyButton.setLocked(true);
            this.lockDifficultyButton.active = false;
            this.difficultyButton.active = false;
        }
    }
    private void refreshResourcePacks(ResourcePackManager resourcePackManager) {
        this.settings.refreshResourcePacks(resourcePackManager);
        this.client.setScreen(this);
    }
    @Inject(method = "init", at = @At(value = "HEAD"),cancellable = true)
    public void inject(CallbackInfo ci){
        ci.cancel();
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().marginX(5).marginBottom(4).alignHorizontalCenter();
        GridWidget.Adder adder = gridWidget.createAdder(2);
        adder.add(this.settings.getFov().createWidget(this.client.options, 0, 0, 150));
        adder.add(this.createTopRightButton());
        adder.add(EmptyWidget.ofHeight(12), 2);
        adder.add(this.createButton(SKIN_CUSTOMIZATION_TEXT, () -> new SkinOptionsScreen(this, this.settings)));
        adder.add(this.createButton(SOUNDS_TEXT, () -> new SoundOptionsScreen(this, this.settings)));
        adder.add(this.createButton(VIDEO_TEXT, () -> new VideoOptionsScreen(this, this.settings)));
        adder.add(this.createButton(CONTROL_TEXT, () -> new ControlsOptionsScreen(this, this.settings)));
        adder.add(this.createButton(LANGUAGE_TEXT, () -> new LanguageOptionsScreen(this, this.settings, this.client.getLanguageManager())));
        adder.add(this.createButton(CHAT_TEXT, () -> new ChatOptionsScreen(this, this.settings)));
        adder.add(
                this.createButton(
                        RESOURCE_PACK_TEXT,
                        () -> new PackScreen(
                                this.client.getResourcePackManager(), this::refreshResourcePacks, this.client.getResourcePackDir(), Text.translatable("resourcePack.title")
                        )
                )
        );
        adder.add(this.createButton(ACCESSIBILITY_TEXT, () -> new AccessibilityOptionsScreen(this, this.settings)));
        adder.add(this.createButton(TELEMETRY_TEXT, () -> new TelemetryInfoScreen(this, this.settings)));
        adder.add(this.createButton(CREDITS_AND_ATTRIBUTION_TEXT, () -> new CreditsAndAttributionScreen(this)));
        adder.add(ButtonWidget.builder(Text.literal("My Button"), button -> this.client.setScreen(new TestScreen(new TestGUI(this.client)))).width(200).build(), 2);
        adder.add(ButtonWidget.builder(ScreenTexts.DONE, button -> this.client.setScreen(this.parent)).width(200).build(), 2, adder.copyPositioner().marginTop(3));
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, 0, this.height / 6 - 12, this.width, this.height, 0.5F, 0.0F);
        gridWidget.forEachChild(this::addDrawableChild);

    }
}