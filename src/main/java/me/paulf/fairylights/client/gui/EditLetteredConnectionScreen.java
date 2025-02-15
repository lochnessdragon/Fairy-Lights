package me.paulf.fairylights.client.gui;

import org.lwjgl.glfw.GLFW;

import me.paulf.fairylights.FairyLights;
import me.paulf.fairylights.client.gui.component.ColorButton;
import me.paulf.fairylights.client.gui.component.PaletteButton;
import me.paulf.fairylights.client.gui.component.StyledTextFieldWidget;
import me.paulf.fairylights.client.gui.component.ToggleButton;
import me.paulf.fairylights.server.connection.Connection;
import me.paulf.fairylights.server.connection.Lettered;
import me.paulf.fairylights.server.net.serverbound.EditLetteredConnectionMessage;
import me.paulf.fairylights.util.matrix.MatrixStack;
import me.paulf.fairylights.util.styledstring.StyledString;
import me.paulf.fairylights.util.styledstring.StylingPresence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public final class EditLetteredConnectionScreen<C extends Connection & Lettered> extends Screen {
    public static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation(FairyLights.ID, "textures/gui/widgets.png");

    private final C connection;

    private StyledTextFieldWidget textField;

    private Button doneBtn;

    private Button cancelBtn;

    private ColorButton colorBtn;

    private ToggleButton boldBtn;

    private ToggleButton italicBtn;

    private ToggleButton underlineBtn;

    private ToggleButton strikethroughBtn;

    private PaletteButton paletteBtn;

    public EditLetteredConnectionScreen(final C connection) {
        super(NarratorChatListener.NO_TITLE);
        this.connection = connection;
    }

    @Override
    public void init() {
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        final int pad = 4;
        final int buttonWidth = 150;
        this.doneBtn = this.addButton(new Button(this.width / 2 - pad - buttonWidth, this.height / 4 + 120 + 12, buttonWidth, 20, new TranslatableComponent("gui.done"), b -> {
            FairyLights.NETWORK.sendToServer(new EditLetteredConnectionMessage<>(this.connection, this.textField.getValue()));
            this.onClose();
        }));
        this.cancelBtn = this.addButton(new Button(this.width / 2 + pad, this.height / 4 + 120 + 12, buttonWidth, 20, new TranslatableComponent("gui.cancel"), b -> this.onClose()));
        final int textFieldX = this.width / 2 - 150;
        final int textFieldY = this.height / 2 - 10;
        int buttonX = textFieldX;
        final int buttonY = textFieldY - 25;
        final int bInc = 24;
        this.colorBtn = this.addButton(new ColorButton(buttonX, buttonY, StringTextComponent.field_240750_d_, b -> this.paletteBtn.field_230694_p_ = !this.paletteBtn.field_230694_p_));
        this.paletteBtn = this.addButton(new PaletteButton(buttonX - 4, buttonY - 30, this.colorBtn, new TranslatableComponent("fairylights.color"), b -> this.textField.updateStyling(this.colorBtn.getDisplayColor(), true)));
        this.boldBtn = this.addButton(new ToggleButton(buttonX += bInc, buttonY, 40, 0, TextComponent.EMPTY, b -> this.updateStyleButton(ChatFormatting.BOLD, this.boldBtn)));
        this.italicBtn = this.addButton(new ToggleButton(buttonX += bInc, buttonY, 60, 0, TextComponent.EMPTY, b -> this.updateStyleButton(ChatFormatting.ITALIC, this.italicBtn)));
        this.underlineBtn = this.addButton(new ToggleButton(buttonX += bInc, buttonY, 80, 0, TextComponent.EMPTY, b -> this.updateStyleButton(ChatFormatting.UNDERLINE, this.underlineBtn)));
        this.strikethroughBtn = this.addButton(new ToggleButton(buttonX += bInc, buttonY, 100, 0, TextComponent.EMPTY, b -> this.updateStyleButton(ChatFormatting.STRIKETHROUGH, this.strikethroughBtn)));
        this.textField = new StyledTextFieldWidget(this.font, this.colorBtn, this.boldBtn, this.italicBtn, this.underlineBtn, this.strikethroughBtn, textFieldX, textFieldY, 300, 20, new TranslatableComponent("fairylights.letteredText"));
        this.textField.setValue(this.connection.getText());
        this.textField.setCaretStart();
        this.textField.setIsBlurable(false);
        this.textField.registerChangeListener(this::validateText);
        this.textField.setCharInputTransformer(this.connection.getInputTransformer());
        this.textField.func_230996_d_(true);
        this.children.add(this.textField);
        this.paletteBtn.visible = false;
        final StylingPresence ss = this.connection.getSupportedStyling();
        this.colorBtn.field_230694_p_ = ss.hasColor();
        this.boldBtn.field_230694_p_ = ss.hasBold();
        this.italicBtn.field_230694_p_ = ss.hasItalic();
        this.underlineBtn.field_230694_p_ = ss.hasUnderline();
        this.strikethroughBtn.field_230694_p_ = ss.hasStrikethrough();
        this.func_212928_a(this.textField);
    }

    private void validateText(final StyledString text) {
        this.doneBtn.field_230693_o_ = this.connection.isSupportedText(text) && !this.connection.getText().equals(text);
    }

    @Override
    public void func_231164_f_() {
        super.func_231164_f_();
        this.field_230706_i_.field_195559_v.func_197967_a(false);
    }

    @Override
    public void func_231023_e_() {
        final Minecraft mc = Minecraft.func_71410_x();
        final int x = (int) (mc.field_71417_B.func_198024_e() * mc.func_228018_at_().func_198107_o() / mc.func_228018_at_().func_198105_m());
        final int y = (int) (mc.field_71417_B.func_198026_f() * mc.func_228018_at_().func_198087_p() / mc.func_228018_at_().func_198083_n());
        this.textField.update(x, y);
    }

    @Override
    public boolean func_231046_a_(final int keyCode, final int scanCode, final int modifiers) {
        this.paletteBtn.field_230694_p_ = false;
        if (isControlOp(keyCode, GLFW.GLFW_KEY_B)) {
            this.toggleStyleButton(TextFormatting.BOLD, this.boldBtn);
            return true;
        } else if (isControlOp(keyCode, GLFW.GLFW_KEY_I)) {
            this.toggleStyleButton(TextFormatting.ITALIC, this.italicBtn);
            return true;
        } else if (isControlOp(keyCode, GLFW.GLFW_KEY_U)) {
            this.toggleStyleButton(TextFormatting.UNDERLINE, this.underlineBtn);
            return true;
        } else if (isControlOp(keyCode, GLFW.GLFW_KEY_S)) {
            this.toggleStyleButton(TextFormatting.STRIKETHROUGH, this.strikethroughBtn);
            return true;
        } else if (super.func_231046_a_(keyCode, scanCode, modifiers)) {
            return true;
        } else if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && this.doneBtn.field_230693_o_) {
            this.doneBtn.func_230930_b_();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.cancelBtn.func_230930_b_();
            return true;
        }
        return false;
    }

    private void toggleStyleButton(final TextFormatting styling, final ToggleButton btn) {
        btn.setValue(!btn.getValue());
        this.updateStyleButton(styling, btn);
    }

    @Override
    public boolean func_231044_a_(final double mouseX, final double mouseY, final int button) {
        if (super.func_231044_a_(mouseX, mouseY, button)) {
            return true;
        }
        this.paletteBtn.field_230694_p_ = false;
        return false;
    }

    private void updateStyleButton(final TextFormatting styling, final ToggleButton btn) {
        if (btn.field_230694_p_) {
            this.textField.updateStyling(styling, btn.getValue());
        }
    }

    @Override
    public void func_230430_a_(final MatrixStack stack, final int mouseX, final int mouseY, final float delta) {
        this.func_230446_a_(stack);
        func_238472_a_(stack, this.field_230712_o_, new TranslationTextComponent("fairylights.editLetteredConnection"), this.field_230708_k_ / 2, 20, 0xFFFFFF);
        super.func_230430_a_(stack, mouseX, mouseY, delta);
        this.textField.func_230430_a_(stack, mouseX, mouseY, delta);
        final String allowed = this.connection.getAllowedDescription();
        if (!allowed.isEmpty()) {
            func_238475_b_(stack, this.field_230712_o_,
                new TranslationTextComponent("fairylights.editLetteredConnection.allowed_characters", allowed)
                    .func_240699_a_(TextFormatting.GRAY),
                this.textField.field_230690_l_,
                this.textField.field_230691_m_ + 24,
                0xFFFFFFFF
            );
        }
    }

    public static boolean isControlOp(final int key, final int controlKey) {
        return key == controlKey && Screen.func_231172_r_() && !Screen.func_231173_s_() && !Screen.func_231174_t_();
    }
}
