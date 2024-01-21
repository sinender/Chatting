package org.polyfrost.chatting.mixin;

import cc.polyfrost.oneconfig.libs.universal.UMouse;
import cc.polyfrost.oneconfig.utils.Notifications;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import org.lwjgl.input.Mouse;
import org.polyfrost.chatting.Chatting;
import org.polyfrost.chatting.chat.ChatSearchingManager;
import org.polyfrost.chatting.chat.ChatWindow;
import org.polyfrost.chatting.config.ChattingConfig;
import org.polyfrost.chatting.hook.GuiNewChatHook;
import org.polyfrost.chatting.utils.ModCompatHooks;
import org.polyfrost.chatting.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.util.List;

@Mixin(value = GuiNewChat.class, priority = 990)
public abstract class GuiNewChatMixin extends Gui implements GuiNewChatHook {
    @Unique
    private int chatting$right = 0;
    @Unique
    private boolean chatting$isHovering;
    @Unique
    private boolean chatting$chatCheck;
    @Unique
    private int chatting$textOpacity;
    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    @Final
    private List<ChatLine> drawnChatLines;
    @Shadow
    public abstract boolean getChatOpen();
    @Shadow
    public abstract int getLineCount();

    @Shadow
    private int scrollPos;

    @Shadow public abstract int getChatWidth();

    @Unique
    private static final ResourceLocation COPY = new ResourceLocation("chatting:copy.png");
    @Unique
    private static final ResourceLocation DELETE = new ResourceLocation("chatting:delete.png");

    @Unique
    private boolean chatting$lineInBounds = false;
    @Unique
    private ChatLine chatting$chatLine;

    /*?
    @Unique
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    @ModifyArg(method = "setChatLine", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ChatLine;<init>(ILnet/minecraft/util/IChatComponent;I)V"))
    private IChatComponent handleAddDrawnLine(IChatComponent iChatComponent) {
        if (!ChattingConfig.INSTANCE.getShowTimestamp()) return iChatComponent;
        String time = " §7["+ sdf.format(new Date(System.currentTimeMillis())) + "]§r";
        iChatComponent.appendSibling(new ChatComponentText(time));
        return iChatComponent;
    }

     */

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void checkScreenshotKeybind(int j2, CallbackInfo ci) {
        if (Chatting.INSTANCE.getKeybind().isPressed()) {
            Chatting.INSTANCE.setDoTheThing(true);
        }
        chatting$chatCheck = false;
    }

    @Unique
    private int chatting$updateCounter;

    @ModifyVariable(method = "drawChat", at = @At("HEAD"), argsOnly = true)
    private int setUpdateCounterWhenYes(int updateCounter) {
        return (chatting$updateCounter = Chatting.INSTANCE.getDoTheThing() ? 0 : updateCounter);
    }

    @ModifyVariable(method = "drawChat", at = @At("STORE"), index = 2)
    private int setChatLimitWhenYes(int linesToDraw) {
        return Chatting.INSTANCE.getDoTheThing()
                ? GuiNewChat.calculateChatboxHeight(mc.gameSettings.chatHeightFocused) / 9
                : linesToDraw;
    }

    @ModifyVariable(method = "drawChat", at = @At("STORE"), ordinal = 0)
    private ChatLine captureChatLine(ChatLine chatLine) {
        chatting$chatLine = chatLine;
        return chatLine;
    }

    @ModifyArgs(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/util/MathHelper;clamp_double(DDD)D"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableBlend()V")))
    private void captureDrawRect(Args args, int updateCounter) {
        int opacity = chatting$getOpacity(updateCounter);
        if (opacity != Integer.MIN_VALUE) {
            args.set(4, ColorUtils.setAlpha(ChattingConfig.INSTANCE.getChatBackgroundColor().getRGB(), opacity / 2));
        }
        if (mc.currentScreen instanceof GuiChat) {
            int left = args.get(0);
            int top = args.get(1);
            int right = args.get(2);
            if (isHovered(left, top, right - left, 9)) {
                chatting$isHovering = true;
                chatting$lineInBounds = true;
                args.set(4, ChattingConfig.INSTANCE.getHoveredChatBackgroundColor().getRGB());
            }
        }
    }

    @Unique
    private int chatting$getOpacity(int updateCounter) {
        if (chatting$chatLine != null) {
            float f = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;
            int n = updateCounter - chatting$chatLine.getUpdatedCounter();
            if (n < 200 || getChatOpen()) {
                int backgroundAlpha = ChattingConfig.INSTANCE.getChatBackgroundColor().getAlpha() * 2;
                double d = (double)n / 200.0;
                d = 1.0 - d;
                d *= 10.0;
                d = MathHelper.clamp_double(d, 0.0, 1.0);
                d *= d;
                int o = (int)(backgroundAlpha * d);
                if (getChatOpen()) {
                    o = backgroundAlpha;
                }
                o = (int)((float)o * f);
                if (o <= 3) {
                    o = 0;
                }
                return o;
            } else {
                return 0;
            }
        }
        return Integer.MIN_VALUE;
    }

    @ModifyArgs(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private void drawChatBox(Args args) {
        if (mc.currentScreen instanceof GuiChat) {
            float f = ChattingConfig.INSTANCE.getChatWindow().getScale();
            int left = 0;
            int top = (int) ((float) args.get(2) - 1);
            int right = MathHelper.ceiling_float_int((float)getChatWidth()) + 4;
            int bottom = (int) ((float) args.get(2) + 8);
            if ((chatting$isHovering && chatting$lineInBounds) || isHovered(left, top, right + 20, 9)) {
                chatting$isHovering = true;
                drawCopyChatBox(right, top);
            }
        }
        chatting$lineInBounds = false;
    }

    private boolean isHovered(int x, int y, int width, int height) {
        ChatWindow hud = ChattingConfig.INSTANCE.getChatWindow();
        ScaledResolution scaleResolution = new ScaledResolution(mc);
        int scale = scaleResolution.getScaleFactor();
        int mouseX = Mouse.getX();
        int mouseY = mc.displayHeight - Mouse.getY();
        int actualX = (int) (((int) hud.position.getX() + x * hud.getScale()) * scale);
        int actualY = (int) (((int) hud.position.getBottomY() + y * hud.getScale()) * scale);
        return mouseX >= actualX && mouseX < actualX + width * hud.getScale() * scale && mouseY >= actualY && mouseY < actualY + height * hud.getScale() * scale;
    }

    @ModifyVariable(method = "drawChat", at = @At("STORE"), ordinal = 0)
    private double modifyYeah(double value) {
        chatting$textOpacity = chatting$getOpacity(chatting$updateCounter);
        if (chatting$textOpacity == Integer.MIN_VALUE) {
            chatting$textOpacity = 0;
        }
        return value;
    }
    /*/
    @Inject(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"))
    private void drawPre(int updateCounter, CallbackInfo ci) {
        RenderUtils.timestampPre();
    }

    @Inject(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;popMatrix()V"))
    private void drawPost(int updateCounter, CallbackInfo ci) {
        RenderUtils.timestampPost();
    }

     */

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void checkStuff(int j2, CallbackInfo ci) {
        if (!chatting$chatCheck && chatting$isHovering) {
            chatting$isHovering = false;
        }
    }

    @Override
    public int chatting$getRight() {
        return chatting$right;
    }

    @Override
    public boolean chatting$isHovering() {
        return chatting$isHovering;
    }

    private void drawCopyChatBox(int right, int top) {
        chatting$chatCheck = true;
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        int posLeft = right + 1;
        int posRight = right + 10;
        if (ChattingConfig.INSTANCE.getChatCopy()) {
            mc.getTextureManager().bindTexture(COPY);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1f);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            chatting$right = right;
            drawModalRectWithCustomSizedTexture(posLeft, top, 0f, 0f, 9, 9, 9, 9);
            int color = isHovered(posLeft, top, posRight - posLeft, 9) ? ChattingConfig.INSTANCE.getChatButtonHoveredBackgroundColor().getRGB() : ChattingConfig.INSTANCE.getChatButtonBackgroundColor().getRGB();
            drawRect(posLeft, top, posRight, top + 9, color);
            posLeft += 10;
            posRight += 10;
            GlStateManager.disableAlpha();
            GlStateManager.disableRescaleNormal();
        }
        if (ChattingConfig.INSTANCE.getChatDelete()) {
            mc.getTextureManager().bindTexture(DELETE);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1f);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            drawModalRectWithCustomSizedTexture(posLeft, top, 0f, 0f, 9, 9, 9, 9);
            int color = isHovered(posLeft, top, posRight - posLeft, 9) ? ChattingConfig.INSTANCE.getChatButtonHoveredBackgroundColor().getRGB() : ChattingConfig.INSTANCE.getChatButtonBackgroundColor().getRGB();
            drawRect(posLeft, top, posRight, top + 9, color);
            GlStateManager.disableAlpha();
            GlStateManager.disableRescaleNormal();
        }
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    public ChatLine chatting$getHoveredLine(int mouseY) {
        if (this.getChatOpen()) {
            ScaledResolution scaledresolution = new ScaledResolution(this.mc);
            int i = scaledresolution.getScaleFactor();
            ChatWindow hud = ChattingConfig.INSTANCE.getChatWindow();
            float f = hud.getScale();
            int k = (int) (mouseY / i - (scaledresolution.getScaledHeight() - hud.position.getBottomY()) + ModCompatHooks.getYOffset() - ModCompatHooks.getChatPosition());
            k = MathHelper.floor_float((float) k / f);

            if (k >= 0) {
                int l = Math.min(this.getLineCount(), this.drawnChatLines.size());

                if (k < this.mc.fontRendererObj.FONT_HEIGHT * l + l) {
                    int i1 = k / this.mc.fontRendererObj.FONT_HEIGHT + this.scrollPos;

                    if (i1 >= 0 && i1 < this.drawnChatLines.size()) {
                        List<ChatLine> m = ChatSearchingManager.filterMessages(ChatSearchingManager.INSTANCE.getLastSearch(), this.drawnChatLines);
                        return m != null ? m.get(i1) : null;
                    }

                }
            }
        }
        return null;
    }

    @Override
    public Transferable chatting$getChattingChatComponent(int mouseY) {
        ChatLine subLine = chatting$getHoveredLine(mouseY);
        if (subLine != null) {
            ChatLine fullLine = this.chatting$getFullMessage(subLine);
            if (GuiScreen.isShiftKeyDown()) {
                if (fullLine != null) {
                    BufferedImage image = Chatting.INSTANCE.screenshotLine(subLine);
                    if (image != null) RenderUtils.copyToClipboard(image);
                }
                return null;
            }
            ChatLine line = GuiScreen.isCtrlKeyDown() ? subLine : fullLine;
            String message = line == null ? "Could not find chat message." : line.getChatComponent().getFormattedText();
            String actualMessage = GuiScreen.isAltKeyDown() ? message : EnumChatFormatting.getTextWithoutFormattingCodes(message);
            Notifications.INSTANCE.send("Chatting", line == null ? "Could not find chat message." : "Copied following text: " + actualMessage);
            return new StringSelection(actualMessage);
        }
        return null;
    }

    @Override
    public int chatting$getTextOpacity() {
        return chatting$textOpacity;
    }
}