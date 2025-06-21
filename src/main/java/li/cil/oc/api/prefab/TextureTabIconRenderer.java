package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Simple implementation of a tab icon renderer using a full texture as its graphic.
 * <br>
 * This renderer displays a texture in a 16x16 area as specified by the TabIconRenderer interface.
 * The texture is rendered using the modern Minecraft 1.20.1 rendering pipeline with proper
 * shader texture binding and vertex buffer management.
 */
@SuppressWarnings("UnusedDeclaration")
public class TextureTabIconRenderer implements TabIconRenderer {
    /**
     * The resource location of the texture to render.
     */
    private final ResourceLocation location;

    /**
     * Creates a new texture tab icon renderer for the specified texture.
     *
     * @param location the resource location of the texture to render
     */
    public TextureTabIconRenderer(ResourceLocation location) {
        this.location = location;
    }

    /**
     * Renders the texture in a 16x16 area starting at (0,0,0).
     * <br>
     * This implementation uses the modern Minecraft 1.20.1 rendering API with
     * RenderSystem for shader texture binding and Tesselator for vertex buffer management.
     * The texture is rendered as a quad covering the full 16x16 icon area.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void render() {
        // Bind the texture using the modern shader system
        RenderSystem.setShaderTexture(0, location);
        
        // Get the tesselator and buffer builder for rendering
        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder buffer = tesselator.getBuilder();
        
        // Begin building a quad with position and texture coordinates
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        
        // Define the quad vertices for a 16x16 area
        // Bottom-left vertex (0, 16)
        buffer.vertex(0.0, 16.0, 0.0).uv(0.0f, 1.0f).endVertex();
        // Bottom-right vertex (16, 16)
        buffer.vertex(16.0, 16.0, 0.0).uv(1.0f, 1.0f).endVertex();
        // Top-right vertex (16, 0)
        buffer.vertex(16.0, 0.0, 0.0).uv(1.0f, 0.0f).endVertex();
        // Top-left vertex (0, 0)
        buffer.vertex(0.0, 0.0, 0.0).uv(0.0f, 0.0f).endVertex();
        
        // Finish and render the quad
        tesselator.end();
    }
}
