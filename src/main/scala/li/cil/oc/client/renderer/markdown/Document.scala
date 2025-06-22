package li.cil.oc.client.renderer.markdown

import li.cil.oc.api
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment
import li.cil.oc.client.renderer.markdown.segment.Segment
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import org.lwjgl.opengl.GL11

import scala.collection.Iterable
import scala.util.matching.Regex

/**
 * Primitive Markdown parser, only supports a very small subset. Used for
 * parsing documentation into segments, to be displayed in a GUI somewhere.
 *
 * General usage is: parse a string using parse(), render it using render().
 *
 * The parser generates a list of segments, each segment representing a part
 * of the document, with a specific formatting / render type. For example,
 * links are their own segments, a bold section in a link would be its own
 * section and so on.
 * The data structure is essentially a very flat multi-tree, where the segments
 * returned are the leaves, and the roots are the individual lines, represented
 * as text segments.
 * Formatting is done by accumulating formatting information over the parent
 * nodes, up to the root.
 */
object Document {
  /**
   * Parses a plain text document into a list of segments.
   */
  def parse(document: Iterable[String]): Segment = {
    var segments: Iterable[Segment] = document.map(line => new segment.TextSegment(null, Option(line).fold("")(_.reverse.dropWhile(_.isWhitespace).reverse)))
    for ((pattern, factory) <- segmentTypes) {
      segments = segments.flatMap(_.refine(pattern, factory))
    }
    for (window <- segments.sliding(2) if window.size == 2) {
      window.head.next = window.last
    }
    segments.head
  }

  /**
   * Compute the overall height of a document, e.g. for computation of scroll offsets.
   */
  def height(document: Segment, maxWidth: Int, renderer: Font): Int = {
    var currentX = 0
    var currentY = 0
    var segment = document
    while (segment != null) {
      currentY += segment.nextY(currentX, maxWidth, renderer)
      currentX = segment.nextX(currentX, maxWidth, renderer)
      segment = segment.next
    }
    currentY
  }

  /**
   * Line height for a normal line of text.
   */
  def lineHeight(renderer: Font): Int = renderer.lineHeight + 1

  /**
   * Renders a list of segments and tooltips if a segment with a tooltip is hovered.
   * Returns the hovered interactive segment, if any.
   */
  def render(document: Segment, x: Int, y: Int, maxWidth: Int, maxHeight: Int, yOffset: Int, renderer: Font, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val mc = Minecraft.getInstance

    RenderState.pushAttrib()

    // Clear depth mask, then create masks in foreground above and below scroll area.
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, false)
    RenderSystem.enableDepthTest()
    RenderSystem.depthFunc(GL11.GL_LEQUAL)
    RenderSystem.depthMask(true)
    RenderSystem.colorMask(false, false, false, false)

    val poseStack = new PoseStack()
    poseStack.pushPose()
    poseStack.translate(0, 0, 500)
    val tesselator = Tesselator.getInstance
    val builder = tesselator.getBuilder
    builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
    builder.vertex(0, y, 0).endVertex()
    builder.vertex(mc.getWindow.getGuiScaledWidth, y, 0).endVertex()
    builder.vertex(mc.getWindow.getGuiScaledWidth, 0, 0).endVertex()
    builder.vertex(0, 0, 0).endVertex()
    builder.vertex(0, mc.getWindow.getGuiScaledHeight, 0).endVertex()
    builder.vertex(mc.getWindow.getGuiScaledWidth, mc.getWindow.getGuiScaledHeight, 0).endVertex()
    builder.vertex(mc.getWindow.getGuiScaledWidth, y + maxHeight, 0).endVertex()
    builder.vertex(0, y + maxHeight, 0).endVertex()
    tesselator.end()
    poseStack.popPose()
    RenderSystem.colorMask(true, true, true, true)

    // Actual rendering.
    var hovered: Option[InteractiveSegment] = None
    var indent = 0
    var currentY = y - yOffset
    val minY = y - lineHeight(renderer)
    val maxY = y + maxHeight + lineHeight(renderer)
    var segment = document
    while (segment != null) {
      val segmentHeight = segment.nextY(indent, maxWidth, renderer)
      if (currentY + segmentHeight >= minY && currentY <= maxY) {
        val result = segment.render(x, currentY, indent, maxWidth, renderer, mouseX, mouseY)
        hovered = hovered.orElse(result)
      }
      currentY += segmentHeight
      indent = segment.nextX(indent, maxWidth, renderer)
      segment = segment.next
    }
    if (mouseX < x || mouseX > x + maxWidth || mouseY < y || mouseY > y + maxHeight) hovered = None
    hovered.foreach(_.notifyHover())

    RenderState.popAttrib()
    RenderSystem.setShaderTexture(0, 0)

    hovered
  }

  // ----------------------------------------------------------------------- //

  private def HeaderSegment(s: Segment, m: Regex.Match) = new segment.HeaderSegment(s, m.group(2), m.group(1).length)

  private def CodeSegment(s: Segment, m: Regex.Match) = new segment.CodeSegment(s, m.group(2))

  private def LinkSegment(s: Segment, m: Regex.Match) = new segment.LinkSegment(s, m.group(1), m.group(2))

  private def BoldSegment(s: Segment, m: Regex.Match) = new segment.BoldSegment(s, m.group(2))

  private def ItalicSegment(s: Segment, m: Regex.Match) = new segment.ItalicSegment(s, m.group(2))

  private def StrikethroughSegment(s: Segment, m: Regex.Match) = new segment.StrikethroughSegment(s, m.group(1))

  private def ImageSegment(s: Segment, m: Regex.Match) = {
    try Option(api.Manual.imageFor(m.group(2))) match {
      case Some(renderer) => new segment.RenderSegment(s, m.group(1), renderer)
      case _ => new segment.TextSegment(s, "No renderer found for: " + m.group(2))
    } catch {
      case t: Throwable => new segment.TextSegment(s, Option(t.toString).getOrElse("Unknown error."))
    }
  }

  // ----------------------------------------------------------------------- //

  private val segmentTypes = Array(
    """^(#+)\s(.*)""".r -> HeaderSegment _, // headers: # ...
    """(`)(.*?)\1""".r -> CodeSegment _, // code: `...`
    """!\[([^\[]*)\]\(([^\)]+)\)""".r -> ImageSegment _, // images: ![...](...)
    """\[([^\[]+)\]\(([^\)]+)\)""".r -> LinkSegment _, // links: [...](...)
    """(\*\*|__)(\S.*?\S|$)\1""".r -> BoldSegment _, // bold: **...** | __...__
    """(\*|_)(\S.*?\S|$)\1""".r -> ItalicSegment _, // italic: *...* | _..._
    """~~(\S.*?\S|$)~~""".r -> StrikethroughSegment _ // strikethrough: ~~...~~
  )
}
