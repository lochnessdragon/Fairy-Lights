package me.paulf.fairylights.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.paulf.fairylights.client.gui.EditLetteredConnectionScreen;
import me.paulf.fairylights.util.FLMath;
import me.paulf.fairylights.util.styledstring.StyledString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.ArrayUtils;

import static net.minecraft.util.text.TextFormatting.AQUA;
import static net.minecraft.util.text.TextFormatting.BLACK;
import static net.minecraft.util.text.TextFormatting.BLUE;
import static net.minecraft.util.text.TextFormatting.DARK_AQUA;
import static net.minecraft.util.text.TextFormatting.DARK_BLUE;
import static net.minecraft.util.text.TextFormatting.DARK_GRAY;
import static net.minecraft.util.text.TextFormatting.DARK_GREEN;
import static net.minecraft.util.text.TextFormatting.DARK_PURPLE;
import static net.minecraft.util.text.TextFormatting.DARK_RED;
import static net.minecraft.util.text.TextFormatting.GOLD;
import static net.minecraft.util.text.TextFormatting.GRAY;
import static net.minecraft.util.text.TextFormatting.GREEN;
import static net.minecraft.util.text.TextFormatting.LIGHT_PURPLE;
import static net.minecraft.util.text.TextFormatting.RED;
import static net.minecraft.util.text.TextFormatting.WHITE;
import static net.minecraft.util.text.TextFormatting.YELLOW;

public class PaletteButton extends Button {
    private static final int TEX_U = 0;

    private static final int TEX_V = 40;

    private static final int SELECT_U = 28;

    private static final int SELECT_V = 40;

    private static final int COLOR_U = 34;

    private static final int COLOR_V = 40;

    private static final int COLOR_WIDTH = 6;

    private static final int COLOR_HEIGHT = 6;

    private static final TextFormatting[] IDX_COLOR = {WHITE, GRAY, DARK_GRAY, BLACK, RED, DARK_RED, YELLOW, GOLD, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GREEN, BLUE, DARK_BLUE, AQUA, DARK_AQUA};

    private static final int[] COLOR_IDX = FLMath.invertMap(IDX_COLOR, TextFormatting::ordinal);

    private final ColorButton colorBtn;

    public PaletteButton(final int x, final int y, final ColorButton colorBtn, final ITextComponent msg, final Button.IPressable pressable) {
        super(x, y, 28, 28, msg, pressable);
        this.colorBtn = colorBtn;
    }

    @Override
    public void func_230930_b_() {
        this.colorBtn.setDisplayColor(IDX_COLOR[(ArrayUtils.indexOf(IDX_COLOR, this.colorBtn.getDisplayColor()) + 1) % IDX_COLOR.length]);
        super.func_230930_b_();
    }

    @Override
    public void func_230982_a_(final double mouseX, final double mouseY) {
        final int idx = this.getMouseOverIndex(mouseX, mouseY);
        if (idx > -1) {
            this.colorBtn.setDisplayColor(IDX_COLOR[idx]);
            super.func_230930_b_();
        }
    }

    @Override
    public void func_230431_b_(final MatrixStack stack, final int mouseX, final int mouseY, final float delta) {
        if (this.field_230694_p_) {
            Minecraft.func_71410_x().func_110434_K().func_110577_a(EditLetteredConnectionScreen.WIDGETS_TEXTURE);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.func_238474_b_(stack, this.field_230690_l_, this.field_230691_m_, TEX_U, TEX_V, this.field_230688_j_, this.field_230689_k_);
            if (this.colorBtn.hasDisplayColor()) {
                final int idx = COLOR_IDX[this.colorBtn.getDisplayColor().ordinal()];
                final int selectX = this.field_230690_l_ + 2 + (idx % 4) * 6;
                final int selectY = this.field_230691_m_ + 2 + (idx / 4) * 6;
                this.func_238474_b_(stack, selectX, selectY, SELECT_U, SELECT_V, COLOR_WIDTH, COLOR_HEIGHT);
            }
            for (int i = 0; i < IDX_COLOR.length; i++) {
                final TextFormatting color = IDX_COLOR[i];
                final int rgb = StyledString.getColor(color);
                final float r = (rgb >> 16 & 0xFF) / 255F;
                final float g = (rgb >> 8 & 0xFF) / 255F;
                final float b = (rgb & 0xFF) / 255F;
                RenderSystem.color4f(r, g, b, 1.0F);
                this.func_238474_b_(stack, this.field_230690_l_ + 2 + (i % 4) * 6, this.field_230691_m_ + 2 + i / 4 * 6, COLOR_U, COLOR_V, COLOR_WIDTH, COLOR_HEIGHT);
            }
            final int selectIndex = this.getMouseOverIndex(mouseX, mouseY);
            if (selectIndex > -1) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.color4f(1, 1, 1, 0.5F);
                final int hoverSelectX = this.field_230690_l_ + 2 + selectIndex % 4 * 6;
                final int hoverSelectY = this.field_230691_m_ + 2 + selectIndex / 4 * 6;
                this.func_238474_b_(stack, hoverSelectX, hoverSelectY, SELECT_U, SELECT_V, COLOR_WIDTH, COLOR_HEIGHT);
                RenderSystem.disableBlend();
            }
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private int getMouseOverIndex(final double mouseX, final double mouseY) {
        final int relX = MathHelper.func_76128_c(mouseX - this.field_230690_l_ - 3);
        final int relY = MathHelper.func_76128_c(mouseY - this.field_230691_m_ - 3);
        if (relX < 0 || relY < 0 || relX > 22 || relY > 22) {
            return -1;
        }
        final int bucketX = relX % 6;
        final int bucketY = relY % 6;
        if (bucketX > 3 || bucketY > 3) {
            return -1;
        }
        final int x = relX / 6;
        final int y = relY / 6;
        return x + y * 4;
    }
}
