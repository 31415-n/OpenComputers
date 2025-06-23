package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections
import li.cil.oc.client.Textures
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.model._
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.block.model.ItemTransform
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraftforge.client.model.data.ModelData
import org.joml.Vector3f


/**
 * Base trait for smart block models in OpenComputers.
 * Provides common functionality for custom block model rendering in 1.20.1.
 */
trait SmartBlockModelBase extends BakedModel {
  
  override def getOverrides: ItemOverrides = ItemOverrides.EMPTY

  override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource): util.List[BakedQuad] = Collections.emptyList()

  override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource, data: ModelData, renderType: net.minecraft.client.renderer.RenderType): util.List[BakedQuad] = {
    getQuads(state, side, rand)
  }

  override def useAmbientOcclusion(): Boolean = true

  override def isGui3d: Boolean = true

  override def usesBlockLight(): Boolean = false

  override def isCustomRenderer: Boolean = false

  // Get particle texture from our texture atlas
  override def getParticleIcon: TextureAtlasSprite = Textures.getSprite(Textures.Block.GenericTop)

  override def getParticleIcon(data: ModelData): TextureAtlasSprite = getParticleIcon

  override def getTransforms: ItemTransforms = DefaultBlockTransforms

  /**
   * Default item transforms for blocks in 1.20.1.
   */
  protected final val DefaultBlockTransforms = {
    val gui = new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f))
    val ground = new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0.1875f, 0), new Vector3f(0.25f, 0.25f, 0.25f))
    val fixed = new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f))
    val thirdperson_righthand = new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 0.15625f, 0), new Vector3f(0.375f, 0.375f, 0.375f))
    val firstperson_righthand = new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.40f, 0.40f, 0.40f))
    val firstperson_lefthand = new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.40f, 0.40f, 0.40f))

    new ItemTransforms(
      ItemTransform.NO_TRANSFORM,
      thirdperson_righthand,
      firstperson_lefthand,
      firstperson_righthand,
      ItemTransform.NO_TRANSFORM,
      gui,
      ground,
      fixed,
      ItemTransform.NO_TRANSFORM
    )
  }

  /**
   * Get the missing model fallback.
   */
  protected def missingModel: BakedModel = {
    Minecraft.getInstance().getModelManager.getMissingModel
  }

  // Standard faces for a unit cube (vertices in world coordinates)
  protected final val UnitCube = Array(
    Array(new Vec3(0, 0, 1), new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1)), // DOWN
    Array(new Vec3(0, 1, 0), new Vec3(0, 1, 1), new Vec3(1, 1, 1), new Vec3(1, 1, 0)), // UP
    Array(new Vec3(1, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 0), new Vec3(0, 1, 0)), // NORTH
    Array(new Vec3(0, 1, 1), new Vec3(0, 0, 1), new Vec3(1, 0, 1), new Vec3(1, 1, 1)), // SOUTH
    Array(new Vec3(0, 1, 0), new Vec3(0, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 1, 1)), // WEST
    Array(new Vec3(1, 1, 1), new Vec3(1, 0, 1), new Vec3(1, 0, 0), new Vec3(1, 1, 0))  // EAST
  )

  // UV mapping planes for each face direction
  protected final val Planes = Array(
    (new Vec3(1, 0, 0), new Vec3(0, 0, -1)),  // DOWN
    (new Vec3(1, 0, 0), new Vec3(0, 0, 1)),   // UP
    (new Vec3(-1, 0, 0), new Vec3(0, -1, 0)), // NORTH
    (new Vec3(1, 0, 0), new Vec3(0, -1, 0)),  // SOUTH
    (new Vec3(0, 0, 1), new Vec3(0, -1, 0)),  // WEST
    (new Vec3(0, 0, -1), new Vec3(0, -1, 0))  // EAST
  )

  protected final val White = 0xFFFFFF

  /**
   * Generate vertices for a box with the specified bounds.
   */
  protected def makeBox(from: Vec3, to: Vec3): Array[Array[Vec3]] = {
    val minX = math.min(from.x, to.x)
    val minY = math.min(from.y, to.y)
    val minZ = math.min(from.z, to.z)
    val maxX = math.max(from.x, to.x)
    val maxY = math.max(from.y, to.y)
    val maxZ = math.max(from.z, to.z)
    
    UnitCube.map(face => face.map(vertex => new Vec3(
      math.max(minX, math.min(maxX, vertex.x)),
      math.max(minY, math.min(maxY, vertex.y)),
      math.max(minZ, math.min(maxZ, vertex.z))
    )))
  }

  /**
   * Rotate a vector around an axis.
   */
  protected def rotateVector(v: Vec3, angle: Double, axis: Vec3): Vec3 = {
    def scale(v: Vec3, s: Double) = new Vec3(v.x * s, v.y * s, v.z * s)
    val cosAngle = math.cos(angle)
    val sinAngle = math.sin(angle)
    scale(v, cosAngle)
      .add(scale(axis.cross(v), sinAngle))
      .add(scale(axis, axis.dot(v) * (1 - cosAngle)))
  }

  /**
   * Rotate a face around an axis.
   */
  protected def rotateFace(face: Array[Vec3], angle: Double, axis: Vec3, around: Vec3 = new Vec3(0.5, 0.5, 0.5)): Array[Vec3] = {
    face.map(v => rotateVector(v.subtract(around), angle, axis).add(around))
  }

  /**
   * Rotate an entire box around an axis.
   */
  protected def rotateBox(box: Array[Array[Vec3]], angle: Double, axis: Vec3 = new Vec3(0, 1, 0), around: Vec3 = new Vec3(0.5, 0.5, 0.5)): Array[Array[Vec3]] = {
    box.map(face => rotateFace(face, angle, axis, around))
  }

  /**
   * Create BakedQuads from box vertices with textures and optional color.
   */
  protected def bakeQuads(box: Array[Array[Vec3]], texture: Array[TextureAtlasSprite], color: Option[Int]): Array[BakedQuad] = {
    val colorRGB = color.getOrElse(White)
    bakeQuads(box, texture, colorRGB)
  }

  /**
   * Create BakedQuads from box vertices with textures and color.
   */
  protected def bakeQuads(box: Array[Array[Vec3]], texture: Array[TextureAtlasSprite], colorRGB: Int): Array[BakedQuad] = {
    Direction.values.map(side => {
      val vertices = box(side.get3DDataValue())
      val data = quadData(vertices, side, texture(side.get3DDataValue()), colorRGB, 0)
      new BakedQuad(data, -1, side, texture(side.get3DDataValue()), true)
    })
  }

  /**
   * Create a single BakedQuad for a unit cube face.
   */
  protected def bakeQuad(side: Direction, texture: TextureAtlasSprite, color: Option[Int], rotation: Int): BakedQuad = {
    val colorRGB = color.getOrElse(White)
    val vertices = UnitCube(side.get3DDataValue())
    val data = quadData(vertices, side, texture, colorRGB, rotation)
    new BakedQuad(data, -1, side, texture, true)
  }

  /**
   * Generate raw vertex data for a quad.
   */
  protected def quadData(vertices: Array[Vec3], facing: Direction, texture: TextureAtlasSprite, colorRGB: Int, rotation: Int): Array[Int] = {
    val (uAxis, vAxis) = Planes(facing.get3DDataValue())
    val rot = (rotation + 4) % 4
    
    vertices.flatMap(vertex => {
      var u = vertex.dot(uAxis)
      var v = vertex.dot(vAxis)
      if (uAxis.x + uAxis.y + uAxis.z < 0) u = 1 + u
      if (vAxis.x + vAxis.y + vAxis.z < 0) v = 1 + v
      
      for (_ <- 0 until rot) {
        val tmp = u
        u = v
        v = (-(tmp - 0.5)) + 0.5
      }
      
      rawData(vertex.x, vertex.y, vertex.z, facing, texture, texture.getU(u * 16), texture.getV(v * 16), colorRGB)
    })
  }

  /**
   * Generate raw vertex data for a single vertex.
   */
  protected def rawData(x: Double, y: Double, z: Double, face: Direction, texture: TextureAtlasSprite, u: Float, v: Float, colorRGB: Int): Array[Int] = {
    val normal = face.getNormal
    val vx = (normal.getX * 127) & 0xFF
    val vy = (normal.getY * 127) & 0xFF
    val vz = (normal.getZ * 127) & 0xFF

    Array(
      java.lang.Float.floatToRawIntBits(x.toFloat),
      java.lang.Float.floatToRawIntBits(y.toFloat),
      java.lang.Float.floatToRawIntBits(z.toFloat),
      getFaceShadeColor(face, colorRGB),
      java.lang.Float.floatToRawIntBits(u),
      java.lang.Float.floatToRawIntBits(v),
      vx | (vy << 0x08) | (vz << 0x10)
    )
  }

  /**
   * Apply face shading to color.
   */
  protected def getFaceShadeColor(face: Direction, colorRGB: Int): Int = {
    // In 1.20.1, lighting is handled by the rendering pipeline
    val brightness = getFaceBrightness(face)
    val b = (colorRGB >> 16) & 0xFF
    val g = (colorRGB >> 8) & 0xFF
    val r = colorRGB & 0xFF
    0xFF000000 | shade(r, brightness) << 16 | shade(g, brightness) << 8 | shade(b, brightness)
  }

  private def shade(value: Int, brightness: Float): Int = (brightness * value).toInt max 0 min 255

  /**
   * Get brightness multiplier for a face direction.
   */
  protected def getFaceBrightness(face: Direction): Float = {
    face match {
      case Direction.DOWN => 0.5f
      case Direction.UP => 1.0f
      case Direction.NORTH | Direction.SOUTH => 0.8f
      case Direction.WEST | Direction.EAST => 0.6f
    }
  }
}