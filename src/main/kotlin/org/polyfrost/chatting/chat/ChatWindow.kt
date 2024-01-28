package org.polyfrost.chatting.chat

import cc.polyfrost.oneconfig.config.annotations.Exclude
import cc.polyfrost.oneconfig.gui.animations.*
import cc.polyfrost.oneconfig.hud.BasicHud
import cc.polyfrost.oneconfig.internal.hud.HudCore
import cc.polyfrost.oneconfig.libs.universal.UGraphics.GL
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack
import cc.polyfrost.oneconfig.utils.dsl.*
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ChatComponentText
import org.polyfrost.chatting.config.ChattingConfig
import org.polyfrost.chatting.utils.EaseOutQuart
import org.polyfrost.chatting.utils.ModCompatHooks

class ChatWindow : BasicHud(true, 0f, 1080 - 27f - 45f) {

    @Exclude
    private val exampleList: List<ChatLine> = listOf(
        ChatLine(0, ChatComponentText("§bChatting"), 1),
        ChatLine(0, ChatComponentText(""), 2),
        ChatLine(0, ChatComponentText("§aThis is a movable chat"), 3),
        ChatLine(0, ChatComponentText("§eDrag me around!"), 4),
        ChatLine(0, ChatComponentText("Click to drag"), 5)
    )

    @Exclude
    var widthAnimation: Animation = DummyAnimation(0f)

    @Exclude
    var heightAnimation: Animation = DummyAnimation(0f)

    @Exclude
    var height = 0

    @Exclude
    var animationWidth = 0f

    @Exclude
    var animationHeight = 0f

    override fun draw(matrices: UMatrixStack?, x: Float, y: Float, scale: Float, example: Boolean) {
        if (!example) return
        nanoVG(true){
            GL.pushMatrix()
            GL.translate(x, y + scale, 0f)
            GL.scale(scale, scale, 1f)
            for (chat in exampleList) {
                ModCompatHooks.redirectDrawString(chat.chatComponent.formattedText, 0f, 0f, -1, chat, false)
                GL.translate(0f, 9f, 0f)
            }
            GL.popMatrix()
        }

    }

    fun drawBG() {
        animationWidth = widthAnimation.get()
        animationHeight = heightAnimation.get()
        val widthEnd = position.width + (if (mc.ingameGUI.chatGUI.chatOpen) 20 else 0) * scale
        val heightEnd = if (height == 0) 0f else (height + paddingY * 2f) * scale
        val duration = ChattingConfig.bgDuration
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        if (widthEnd != widthAnimation.end) {
            if (ChattingConfig.smoothBG)
                widthAnimation = EaseOutQuart(duration, animationWidth, widthEnd, false)
            else
                animationWidth = widthEnd
        }
        if (heightEnd != heightAnimation.end) {
            if (ChattingConfig.smoothBG)
                heightAnimation = EaseOutQuart(duration, animationHeight, heightEnd, false)
            else
                animationHeight = heightEnd
        }
        if (animationHeight <= 0.3f || !background || HudCore.editing) return
        nanoVG(true) {
            drawBackground(position.x, position.bottomY - animationHeight, animationWidth, animationHeight, scale)
        }
        GlStateManager.disableBlend()
        GlStateManager.disableAlpha()
    }

    fun getAlphaBG(): Int {
        return bgColor.alpha
    }

    fun getPaddingX(): Float {
        return paddingX
    }

    fun getPaddingY(): Float {
        return paddingY
    }

    override fun shouldDrawBackground(): Boolean {
        return HudCore.editing
    }

    override fun getWidth(scale: Float, example: Boolean): Float {
        return (GuiNewChat.calculateChatboxWidth(mc.gameSettings.chatWidth) + 4 + ModCompatHooks.chatHeadOffset) * scale
    }

    override fun getHeight(scale: Float, example: Boolean): Float {
        return 9f * 5 * scale
    }

}