package win.ninegang.ninetools.compat.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import win.ninegang.ninetools.compat.util.Wrapper;
import win.ninegang.ninetools.compat.util.IBufferBuilder;
import win.ninegang.ninetools.compat.util.BufferBuilderImpl;

import java.awt.*;

public class Engine2d implements Wrapper {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final float[][] roundedCache = new float[][]{new float[3], new float[3], new float[3], new float[3]};
    private static final IBufferBuilder bufferBuilder = new BufferBuilderImpl();

    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    public static void endRender() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static float[] getColor(Color c) {
        return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f};
    }

    private static void _populateRC(float a, float b, float c, int i) {
        roundedCache[i][0] = a;
        roundedCache[i][1] = b;
        roundedCache[i][2] = c;
    }

    private static void renderRoundedQuadInternal(Matrix4f matrix, float cr, float cg, float cb, float ca,
                                                  float fromX, float fromY, float toX, float toY,
                                                  float radC1, float radC2, float radC3, float radC4, float samples) {

        IBufferBuilder buffer = bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        _populateRC(toX - radC4, toY - radC4, radC4, 0);
        _populateRC(toX - radC2, fromY + radC2, radC2, 1);
        _populateRC(fromX + radC1, fromY + radC1, radC1, 2);
        _populateRC(fromX + radC3, toY - radC3, radC3, 3);

        for (int i = 0; i < 4; i++) {
            float[] current = roundedCache[i];
            float rad = current[2];
            for (float r = i * 90f; r <= (i + 1) * 90f; r += 90 / samples) {
                float rad1 = Math.toRadians(r);
                float sin = Math.sin(rad1) * rad;
                float cos = Math.cos(rad1) * rad;

                buffer.vertex(matrix, current[0] + sin, current[1] + cos, 0.0F)
                        .color(cr, cg, cb, ca)
                        .next();
            }
        }

        buffer.renderGlobal();
    }

    public static void renderRoundedQuad(MatrixStack matrices, Color c, double fromX, double fromY, double toX, double toY,
                                         float radTL, float radTR, float radBL, float radBR, float samples) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[] color1 = getColor(c);
        float r = color1[0];
        float g = color1[1];
        float b = color1[2];
        float a = color1[3];

        setupRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        renderRoundedQuadInternal(matrix, r, g, b, a, (float) fromX, (float) fromY, (float) toX, (float) toY,
                radTL, radTR, radBL, radBR, samples);
        endRender();
    }

    public static void renderRoundedQuad(MatrixStack stack, Color c, double x, double y, double x1, double y1,
                                         float rad, float samples) {
        renderRoundedQuad(stack, c, x, y, x1, y1, rad, rad, rad, rad, samples);
    }

    /**
     * Renders a regular colored quad
     *
     * @param matrices The context MatrixStack
     * @param color    The color of the quad
     * @param x1       The start X coordinate
     * @param y1       The start Y coordinate
     * @param x2       The end X coordinate
     * @param y2       The end Y coordinate
     */
    public static void renderQuad(MatrixStack matrices, Color color, double x1, double y1, double x2, double y2) {
        double j;
        if (x1 < x2) {
            j = x1;
            x1 = x2;
            x2 = j;
        }

        if (y1 < y2) {
            j = y1;
            y1 = y2;
            y2 = j;
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[] colorFloat = getColor(color);

        IBufferBuilder buffer = bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, (float) x1, (float) y2, 0.0F)
                .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                .next();
        buffer.vertex(matrix, (float) x2, (float) y2, 0.0F)
                .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                .next();
        buffer.vertex(matrix, (float) x2, (float) y1, 0.0F)
                .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                .next();
        buffer.vertex(matrix, (float) x1, (float) y1, 0.0F)
                .color(colorFloat[0], colorFloat[1], colorFloat[2], colorFloat[3])
                .next();

        setupRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        buffer.renderGlobal();
        endRender();
    }
    private static void renderRoundedOutlineInternal
            (Matrix4f matrix, float cr, float cg, float cb, float ca,
             float fromX, float fromY, float toX, float toY,
             float radC1, float radC2, float radC3, float radC4,
             float width, float samples) {

        IBufferBuilder buffer = bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        _populateRC(toX - radC4, toY - radC4, radC4, 0);
        _populateRC(toX - radC2, fromY + radC2, radC2, 1);
        _populateRC(fromX + radC1, fromY + radC1, radC1, 2);
        _populateRC(fromX + radC3, toY - radC3, radC3, 3);

        for (int i = 0; i < 4; i++) {
            float[] current = roundedCache[i];
            float rad = current[2];
            for (float r = i * 90f; r <= (i + 1) * 90f; r += 90 / samples) {
                float rad1 = Math.toRadians(r);
                float sin1 = Math.sin(rad1);
                float sin = sin1 * rad;
                float cos1 = Math.cos(rad1);
                float cos = cos1 * rad;
                buffer.vertex(matrix, current[0] + sin, current[1] + cos, 0.0F)
                        .color(cr, cg, cb, ca)
                        .next();
                buffer.vertex(matrix, current[0] + sin + sin1 * width, current[1] + cos + cos1 * width, 0.0F)
                        .color(cr, cg, cb, ca)
                        .next();
            }
        }


        float[] current = roundedCache[0];
        float rad = current[2];
        buffer.vertex(matrix, current[0], current[1] + rad, 0.0F)
                .color(cr, cg, cb, ca)
                .next();
        buffer.vertex(matrix, current[0], current[1] + rad + width, 0.0F)
                .color(cr, cg, cb, ca)
                .next();

        buffer.renderGlobal();
    }

    /**
     * Renders a round outline
     *
     * @param matrices     MatrixStack
     * @param c            Color of the outline
     * @param fromX        From X coordinate
     * @param fromY        From Y coordinate
     * @param toX          To X coordinate
     * @param toY          To Y coordinate
     * @param radTL        Radius of the top left corner
     * @param radTR        Radius of the top right corner
     * @param radBL        Radius of the bottom left corner
     * @param radBR        Radius of the bottom right corner
     * @param outlineWidth Width of the outline
     * @param samples      Amount of samples to use per corner
     */
    public static void renderRoundedOutline(MatrixStack matrices, Color c, double fromX, double fromY, double toX, double toY,
                                            float radTL, float radTR, float radBL, float radBR, float outlineWidth, float samples) {

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[] color1 = getColor(c);
        float r = color1[0];
        float g = color1[1];
        float b = color1[2];
        float a = color1[3];

        setupRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        renderRoundedOutlineInternal(matrix, r, g, b, a, (float) fromX, (float) fromY, (float) toX, (float) toY,
                radTL, radTR, radBL, radBR, outlineWidth, samples);

        endRender();
    }

    /**
     * Renders a round outline with uniform corner radius
     *
     * @param matrices MatrixStack
     * @param c        Color of the outline
     * @param fromX    From X coordinate
     * @param fromY    From Y coordinate
     * @param toX      To X coordinate
     * @param toY      To Y coordinate
     * @param rad      Radius of the corners
     * @param width    Width of the outline
     * @param samples  Amount of samples to use per corner
     */
    public static void renderRoundedOutline(MatrixStack matrices, Color c, double fromX, double fromY, double toX, double toY,
                                            float rad, float width, float samples) {
        renderRoundedOutline(matrices, c, fromX, fromY, toX, toY, rad, rad, rad, rad, width, samples);
    }
}