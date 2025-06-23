package li.cil.oc.client.renderer.entity

import li.cil.oc.common.entity.Drone
import li.cil.oc.util.RenderState
import net.minecraft.client.model.Model
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.{CubeListBuilder, LayerDefinition, MeshDefinition}
import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.opengl.GL11

object ModelQuadcopter {
  /**
   * Creates the layer definition for the quadcopter model.
   * This replaces the old texture offset and addBox system from 1.12.2.
   */
  def createBodyLayer(): LayerDefinition = {
    val meshDefinition = new MeshDefinition()
    val partDefinition = meshDefinition.getRoot

    // Body parts
    val bodyBuilder = CubeListBuilder.create()
    bodyBuilder.texOffs(0, 1).addBox(-3, 1, -3, 6, 1, 6) // top
    bodyBuilder.texOffs(0, 23).addBox(-1, 0, -1, 2, 1, 2) // middle  
    bodyBuilder.texOffs(0, 17).addBox(-2, -1, -2, 4, 1, 4) // bottom
    partDefinition.addOrReplaceChild("body", bodyBuilder, PartPose.rotation(0, math.toRadians(45).toFloat, 0))

    // Wing parts
    val wing0Builder = CubeListBuilder.create()
    wing0Builder.texOffs(0, 9).addBox(1, 0, -7, 6, 1, 6) // flap0
    wing0Builder.texOffs(0, 27).addBox(2, -1, -3, 1, 3, 1) // pin0
    partDefinition.addOrReplaceChild("wing0", wing0Builder, PartPose.ZERO)

    val wing1Builder = CubeListBuilder.create()
    wing1Builder.texOffs(0, 9).addBox(1, 0, 1, 6, 1, 6) // flap1
    wing1Builder.texOffs(0, 27).addBox(2, -1, 2, 1, 3, 1) // pin1
    partDefinition.addOrReplaceChild("wing1", wing1Builder, PartPose.ZERO)

    val wing2Builder = CubeListBuilder.create()
    wing2Builder.texOffs(0, 9).addBox(-7, 0, 1, 6, 1, 6) // flap2
    wing2Builder.texOffs(0, 27).addBox(-3, -1, 2, 1, 3, 1) // pin2
    partDefinition.addOrReplaceChild("wing2", wing2Builder, PartPose.ZERO)

    val wing3Builder = CubeListBuilder.create()
    wing3Builder.texOffs(0, 9).addBox(-7, 0, -7, 6, 1, 6) // flap3
    wing3Builder.texOffs(0, 27).addBox(-3, -1, -3, 1, 3, 1) // pin3
    partDefinition.addOrReplaceChild("wing3", wing3Builder, PartPose.ZERO)

    // Light parts
    val light0Builder = CubeListBuilder.create()
    light0Builder.texOffs(24, 0).addBox(1, 0, -7, 6, 1, 6)
    partDefinition.addOrReplaceChild("light0", light0Builder, PartPose.ZERO)

    val light1Builder = CubeListBuilder.create()
    light1Builder.texOffs(24, 0).addBox(1, 0, 1, 6, 1, 6)
    partDefinition.addOrReplaceChild("light1", light1Builder, PartPose.ZERO)

    val light2Builder = CubeListBuilder.create()
    light2Builder.texOffs(24, 0).addBox(-7, 0, 1, 6, 1, 6)
    partDefinition.addOrReplaceChild("light2", light2Builder, PartPose.ZERO)

    val light3Builder = CubeListBuilder.create()
    light3Builder.texOffs(24, 0).addBox(-7, 0, -7, 6, 1, 6)
    partDefinition.addOrReplaceChild("light3", light3Builder, PartPose.ZERO)

    LayerDefinition.create(meshDefinition, 64, 32)
  }
}

final class ModelQuadcopter(root: ModelPart) extends Model(net.minecraft.client.renderer.RenderType.entitySolid _) {
  val body: ModelPart = root.getChild("body")
  val wing0: ModelPart = root.getChild("wing0")
  val wing1: ModelPart = root.getChild("wing1")
  val wing2: ModelPart = root.getChild("wing2")
  val wing3: ModelPart = root.getChild("wing3")
  val light0: ModelPart = root.getChild("light0")
  val light1: ModelPart = root.getChild("light1")
  val light2: ModelPart = root.getChild("light2")
  val light3: ModelPart = root.getChild("light3")



  private val scale = 1 / 16f
  private val up = new Vec3(0, 1, 0)

  private def doRender(drone: Drone, dt: Float) {
    // Note: In 1.20.1, transformations are handled through PoseStack in renderToBuffer method
    // This method is kept for compatibility but transformations should be moved to renderToBuffer

    // Model part rendering is now handled in renderToBuffer method
    wing0.xRot = drone.flapAngles(0)(0)
    wing0.zRot = drone.flapAngles(0)(1)
    wing1.xRot = drone.flapAngles(1)(0)
    wing1.zRot = drone.flapAngles(1)(1)
    wing2.xRot = drone.flapAngles(2)(0)
    wing2.zRot = drone.flapAngles(2)(1)
    wing3.xRot = drone.flapAngles(3)(0)
    wing3.zRot = drone.flapAngles(3)(1)

    if (drone.isRunning) {
      light0.xRot = drone.flapAngles(0)(0)
      light0.zRot = drone.flapAngles(0)(1)
      light1.xRot = drone.flapAngles(1)(0)
      light1.zRot = drone.flapAngles(1)(1)
      light2.xRot = drone.flapAngles(2)(0)
      light2.zRot = drone.flapAngles(2)(1)
      light3.xRot = drone.flapAngles(3)(0)
      light3.zRot = drone.flapAngles(3)(1)
    }
  }

  // For inventory rendering - now handled through renderToBuffer
  def setupInventoryPose(): Unit = {
    val tilt = math.toRadians(2).toFloat
    wing0.xRot = tilt
    wing0.zRot = tilt
    wing1.xRot = -tilt
    wing1.zRot = tilt
    wing2.xRot = -tilt
    wing2.zRot = -tilt
    wing3.xRot = tilt
    wing3.zRot = -tilt

    light0.xRot = tilt
    light0.zRot = tilt
    light1.xRot = -tilt
    light1.zRot = tilt
    light2.xRot = -tilt
    light2.zRot = -tilt
    light3.xRot = tilt
    light3.zRot = -tilt
  }

  override def renderToBuffer(poseStack: PoseStack, vertexConsumer: VertexConsumer, packedLight: Int, packedOverlay: Int, red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    wing0.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    wing1.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    wing2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    wing3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    
    // Render lights with special handling
    light0.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    light1.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    light2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    light3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
  }

  def renderDrone(drone: Drone, poseStack: PoseStack, vertexConsumer: VertexConsumer, packedLight: Int, packedOverlay: Int, partialTick: Float): Unit = {
    doRender(drone, partialTick)
    renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f)
  }
}
