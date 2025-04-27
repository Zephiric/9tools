package win.ninegang.ninetools.compat.util

import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormat.DrawMode
import org.joml.Matrix4f

class BufferBuilderImpl: IBufferBuilder {
    val t: Tessellator = Tessellator.getInstance()
    private lateinit var buffer: BufferBuilder
    private var lastVertexConsumer: VertexConsumer? = null

    override fun begin(var1: DrawMode, var2: VertexFormat): IBufferBuilder {
        buffer = t.begin(var1, var2)
        return this
    }

    override fun vertex(var1: Float, var2: Float, var3: Float): IBufferBuilder {
        lastVertexConsumer = buffer.vertex(var1, var2, var3)
        return this
    }

    override fun vertex(matrix: Matrix4f, var1: Float, var2: Float, var3: Float): IBufferBuilder {
        lastVertexConsumer = buffer.vertex(matrix, var1, var2, var3)
        return this
    }

    override fun texture(var1: Float, var2: Float): IBufferBuilder {
        lastVertexConsumer = buffer.texture(var1, var2)
        return this
    }

    override fun color(r: Float, g: Float, b: Float, a: Float): IBufferBuilder {
        lastVertexConsumer = lastVertexConsumer!!.color(r, g, b, a)
        return this
    }

    override fun color(color: Int): IBufferBuilder {
        lastVertexConsumer = buffer.color(color)
        return this
    }

    override fun next(): IBufferBuilder {
        return this
    }

    override fun render() {
        BufferRenderer.draw(buffer.end())
    }

    override fun renderGlobal() {
        BufferRenderer.drawWithGlobalProgram(buffer.end())
    }

    override fun getBuffer(): BufferBuilder {
        return buffer
    }
}