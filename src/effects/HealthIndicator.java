package effects;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

public class HealthIndicator {

    private AssetManager asset;
    @SuppressWarnings("unused")
	private float level = 1;
    private FloatBuffer colorBuffer;
    private Mesh mesh;
    private FloatBuffer vertexBuffer;
    private Spatial geometry;

    public HealthIndicator(AssetManager asset) {
        this.asset = asset;
    }

    public Spatial getSpatial() {

        if (geometry == null) {
            mesh = new Mesh();

            int vCount = 16;
            int tCount = 8;

            vertexBuffer = BufferUtils.createVector3Buffer(vCount);
            colorBuffer = BufferUtils.createFloatBuffer(vCount * 4);
            IntBuffer indexBuffer = BufferUtils.createIntBuffer(tCount * 3);

            mesh.setBuffer(Type.Position, 3, vertexBuffer);
            mesh.setBuffer(Type.Color, 4, colorBuffer);

            setLevel(1);

            indexBuffer.put(0).put(1).put(2);
            indexBuffer.put(1).put(3).put(2);
            indexBuffer.put(4).put(5).put(6);
            indexBuffer.put(5).put(7).put(6);

            indexBuffer.put(8 + 0).put(8 + 1).put(8 + 2);
            indexBuffer.put(8 + 1).put(8 + 3).put(8 + 2);
            indexBuffer.put(8 + 4).put(8 + 5).put(8 + 6);
            indexBuffer.put(8 + 5).put(8 + 7).put(8 + 6);

            mesh.setBuffer(Type.Index, 3, indexBuffer);

            geometry = new Geometry("Health indicator", mesh);
            Material mat = new Material(asset, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setBoolean("VertexColor", true);
            geometry.setMaterial(mat);
            mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            geometry.setQueueBucket(Bucket.Transparent);

        }
        return geometry;

    }

    public void setLevel(float level) {
        this.level = level;

        float height = 0.1f;
        float halfWidth = 0.5f;
        float halfGap = 0.00f;//0.05f;
        float blackAlpha = 0.6f;

        boolean simpleColor = false;

        vertexBuffer.position(0);
        colorBuffer.position(0);

        vertexBuffer.put(-halfWidth).put(height / 2).put(0);
        vertexBuffer.put(-halfWidth).put(-height / 2).put(0);

        float lowRight = -halfGap;
        float lowColor = 1;

        if (level < 0.5) {
            lowRight = -0.5f + level * 2 * (0.5f - halfGap);
            lowColor = level * 2;
        }

        vertexBuffer.put(lowRight).put(height / 2).put(0);
        vertexBuffer.put(lowRight).put(-height / 2).put(0);

        vertexBuffer.put(halfGap).put(height / 2).put(0);
        vertexBuffer.put(halfGap).put(-height / 2).put(0);

        float highRight = halfGap;
        float highColor = 1;
        if (level > 0.5) {
            highRight = halfGap + (level - 0.5f) * 2 * (halfWidth - halfGap);
            highColor = 1 - (level - 0.5f);
        }

        vertexBuffer.put(highRight).put(height / 2).put(0);
        vertexBuffer.put(highRight).put(-height / 2).put(0);

        if (simpleColor) {
            //TODO: Implement simple colors
            colorBuffer.put(1).put(0).put(0).put(1);
            colorBuffer.put(1).put(0).put(0).put(1);
            colorBuffer.put(1).put(lowColor).put(0).put(1);
            colorBuffer.put(1).put(lowColor).put(0).put(1);
            colorBuffer.put(1).put(1).put(0).put(1);
            colorBuffer.put(1).put(1).put(0).put(1);
            colorBuffer.put(highColor).put(1).put(0).put(1);
            colorBuffer.put(highColor).put(1).put(0).put(1);
        } else {
            colorBuffer.put(1).put(0).put(0).put(1);
            colorBuffer.put(1).put(0).put(0).put(1);
            colorBuffer.put(1).put(lowColor).put(0).put(1);
            colorBuffer.put(1).put(lowColor).put(0).put(1);
            colorBuffer.put(1).put(1).put(0).put(1);
            colorBuffer.put(1).put(1).put(0).put(1);
            colorBuffer.put(highColor).put(1).put(0).put(1);
            colorBuffer.put(highColor).put(1).put(0).put(1);
        }

        colorBuffer.put(0).put(0).put(0).put(blackAlpha);
        colorBuffer.put(0).put(0).put(0).put(blackAlpha);
        colorBuffer.put(0).put(0).put(0).put(blackAlpha);
        colorBuffer.put(0).put(0).put(0).put(blackAlpha);
        colorBuffer.put(0).put(0).put(0).put(blackAlpha);
        colorBuffer.put(0).put(0).put(0).put(blackAlpha);
        colorBuffer.put(0).put(0).put(0).put(blackAlpha);
        colorBuffer.put(0).put(0).put(0).put(blackAlpha);

        float border = 0.03f;
        vertexBuffer.put(-halfWidth - border).put(height / 2 + border).put(-0.01f);
        vertexBuffer.put(-halfWidth - border).put(-height / 2 - border).put(-0.01f);
        /*vertexBuffer.put(-halfGap + border).put(height / 2 + border).put(-0.01f);
        vertexBuffer.put(-halfGap + border).put(-height / 2 - border).put(-0.01f);
        vertexBuffer.put(halfGap - border).put(height / 2 + border).put(-0.01f);
        vertexBuffer.put(halfGap - border).put(-height / 2 - border).put(-0.01f);*/
        vertexBuffer.put(+halfWidth + border).put(height / 2 + border).put(-0.01f);
        vertexBuffer.put(+halfWidth + border).put(-height / 2 - border).put(-0.01f);

        mesh.getBuffer(Type.Position).updateData(vertexBuffer);
        mesh.getBuffer(Type.Color).updateData(colorBuffer);

    }

}