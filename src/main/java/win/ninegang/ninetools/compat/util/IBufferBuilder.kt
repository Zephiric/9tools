package win.ninegang.ninetools.compat.util

import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormat.DrawMode
import org.joml.Matrix4f

interface IBufferBuilder {
    fun begin(drawMode: DrawMode, format: VertexFormat): IBufferBuilder
    fun vertex(x: Float, y: Float, z: Float): IBufferBuilder
    fun vertex(matrix: Matrix4f, x: Float, y: Float, z: Float): IBufferBuilder
    fun texture(u: Float, v: Float): IBufferBuilder
    fun color(r: Float, g: Float, b: Float, a: Float): IBufferBuilder
    fun color(color: Int): IBufferBuilder
    fun next(): IBufferBuilder
    fun render()
    fun renderGlobal()
    fun getBuffer(): BufferBuilder
}