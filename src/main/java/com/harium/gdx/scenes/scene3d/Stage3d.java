package com.harium.gdx.scenes.scene3d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;

public class Stage3d {

    private static final ObjectMap<Integer, Pointer> pointers = new ObjectMap<>();

    private final FrameBuffer fbo;
    public final Stage stage;

    public float width;
    public float height;

    public ModelInstance instance;

    private final Matrix4 matrix = new Matrix4();
    public final Color background = new Color(0, 0, 0, 0);

    private static final Vector3 tmp1 = new Vector3();
    private static final Vector3 tmp2 = new Vector3();
    private static final Vector3 tmp3 = new Vector3();
    private static final Vector3 tmp4 = new Vector3();
    private static final Vector3 tmp5 = new Vector3();

    private static final Vector3 v0 = new Vector3();
    private static final Vector3 v1 = new Vector3();
    private static final Vector3 v2 = new Vector3();

    public Stage3d(FrameBuffer fbo, Vector3 center, float width, float height) {
        this.fbo = fbo;
        this.width = width;
        this.height = height;

        stage = new CustomStage(fbo.getWidth(), fbo.getHeight());

        Texture generated = fbo.getColorBufferTexture();

        Model model = build(width, height, generated);
        instance = new ModelInstance(model);
        instance.transform.setToTranslation(center);
    }

    protected void render() {
        Gdx.gl.glClearColor(background.r, background.g, background.b, background.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        matrix.setToOrtho2D(0, 0, fbo.getWidth(), fbo.getHeight());
        Batch batch = stage.getBatch();
        batch.setProjectionMatrix(matrix);
        batch.begin();
        stage.getRoot().draw(batch, 1);
        batch.end();
    }

    public int getTextureWidth() {
        return fbo.getWidth();
    }

    public int getTextureHeight() {
        return fbo.getHeight();
    }

    private void onMove(int x, int y) {
        stage.mouseMoved(x, y);
        act();
    }

    private void touchDown(int x, int y, int id) {
        stage.touchDown(x, y, id, Input.Buttons.LEFT);
        act();
    }

    private void touchUp(int x, int y, int id) {
        stage.touchUp(x, y, id, Input.Buttons.LEFT);
        act();
    }

    public void act() {
        stage.act();
        refresh();
    }

    private void refresh() {
        fbo.begin();
        render();
        fbo.end();
    }

    public Model build(float width, float height, Texture texture) {
        ModelBuilder modelBuilder = new ModelBuilder();

        Material material = new Material();
        material.set(TextureAttribute.createDiffuse(texture), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));

        modelBuilder.begin();

        MeshPartBuilder mpb = modelBuilder
                .part("stage", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position /*| VertexAttributes.Usage.Normal*/ | VertexAttributes.Usage.TextureCoordinates, material);

        mpb.setUVRange(1, 0, 0, 1);

        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;

        Vector3 a = tmp1.set(-halfWidth, halfHeight, 0);
        Vector3 b = tmp2.set(halfWidth, halfHeight, 0);
        Vector3 c = tmp3.set(halfWidth, -halfHeight, 0);
        Vector3 d = tmp4.set(-halfWidth, -halfHeight, 0);
        Vector3 normal = tmp5.set(Vector3.Z);
        mpb.rect(b, a, d, c, normal);

        return modelBuilder.end();
    }

    public void lookAt(Camera camera) {
        lookAt(instance, camera.position, camera.up);
    }

    public void lookAt(Vector3 position, Vector3 up) {
        lookAt(instance, position, up);
    }

    private void lookAt(ModelInstance instance, Vector3 position, Vector3 up) {
        // Save position
        Vector3 p = instance.transform.getTranslation(tmp4);
        tmp3.set(position).sub(p).nor();

        // Calculate Rotation
        tmp1.set(up).crs(tmp3).nor();
        tmp2.set(tmp3).crs(tmp1).nor();
        Quaternion q = new Quaternion().setFromAxes(tmp1.x, tmp2.x, tmp3.x, tmp1.y, tmp2.y, tmp3.y, tmp1.z, tmp2.z, tmp3.z);

        // Apply transform
        instance.transform.setToTranslation(p);
        instance.transform.rotate(q);
    }

    public void onMove(int pointer, Ray ray) {
        Pointer p = pointers.get(pointer);
        if (p == null) {
            p = new Pointer();
            p.id = pointer;
            pointers.put(pointer, p);
        }

        float px = -1;
        float py = -1;

        Matrix4 transform = instance.transform;

        Vector3 a = tmp1.set(-width / 2,  -height / 2,0).mul(transform);
        Vector3 b = tmp2.set(width / 2, -height / 2,0).mul(transform);
        Vector3 c = tmp3.set(width / 2,  height / 2,0).mul(transform);
        Vector3 d = tmp4.set(-width / 2,  height / 2,0).mul(transform);

        Vector3 intersection = tmp5;
        if (Intersector.intersectRayTriangle(ray, a, b, c, intersection)) {
            // First half
            // From intersection we calculate the barycenter
            // From the barycenter we can calculate the local space
            Vector3 weight = barycentric(a, b, c, intersection, tmp4);
            // A = (0, 0)
            // B = (1, 0)
            // C = (1, 1)
            float tx = /*0 * weight.x*/ + 1 * weight.y + 1 * weight.z;
            float ty = /*0 * weight.x*/ /*+ 0 * weight.y*/ + 1 * weight.z;

            px = getTextureWidth() * tx;
            py = getTextureHeight() * ty;

        } else if (Intersector.intersectRayTriangle(ray, a, c, d, intersection)) {
            Vector3 weight = barycentric(a, c, d, intersection, tmp2);
            // A = (0, 0)
            // C = (1, 1)
            // D = (0, 1)
            float tx = /*0 * weight.x*/ + 1 * weight.y /*+ 0 * weight.z*/;
            float ty = /*0 * weight.x*/ + 1 * weight.y + 1 * weight.z;

            px = getTextureWidth() * tx;
            py = getTextureHeight() * ty;
        } else {
            p.x = (int) px;
            p.y = (int) py;
            p.lastStage = null;

            onMove(p.x, p.y);
            return;
        }

        if (p.x != (int) px || p.y != (int) py) {
            p.x = (int) px;
            p.y = (int) py;
            p.lastStage = this;

            onMove(p.x, p.y);
        }
    }

    public void onPressed(int pointer) {
        Pointer p = pointers.get(pointer);
        if (p == null || p.pressed) {
            return;
        }
        p.pressed = true;

        if (p.lastStage != null) {
            p.lastStage.touchDown(p.x, p.y, p.id);
        }
    }

    public void onRelease(int pointer) {
        Pointer p = pointers.get(pointer);
        if (p == null || !p.pressed) {
            return;
        }
        p.pressed = false;

        if (p.lastStage != null) {
            p.lastStage.touchUp(p.x, p.y, p.id);
        }
    }

    /**
     * https://github.com/mgsx-dev/dl13/blob/master/core/src/net/mgsx/dl13/navmesh/NavMesh.java#L136
     */
    public static Vector3 barycentric(Vector3 a, Vector3 b, Vector3 c, Vector3 p, Vector3 out) {
        v0.set(b).sub(a);
        v1.set(c).sub(a);
        v2.set(p).sub(a);

        float d00 = v0.dot(v0);
        float d01 = v0.dot(v1);
        float d11 = v1.dot(v1);
        float d20 = v2.dot(v0);
        float d21 = v2.dot(v1);
        float denom = d00 * d11 - d01 * d01;
        if (denom == 0) {
            denom = 1;
        }
        float v = (d11 * d20 - d01 * d21) / denom;
        float w = (d00 * d21 - d01 * d20) / denom;
        float u = 1.0f - v - w;

        return out.set(u, v, w);
    }

    public void dispose() {
        instance.model.dispose();
    }

    static class Pointer {
        boolean pressed;
        int x, y, id;
        Stage3d lastStage;
    }

    private class CustomStage extends Stage {

        public CustomStage(int textureWidth, int textureHeight) {
            super(new ScalingViewport(Scaling.none, textureWidth, textureHeight, new OrthographicCamera(textureWidth, textureHeight)));
        }

        @Override
        protected boolean isInsideViewport(int screenX, int screenY) {
            return screenX >= 0 && screenY >= 0 && screenX < getCamera().viewportWidth && screenY < getCamera().viewportHeight;
        }

        public Vector2 screenToStageCoordinates(Vector2 screenCoords) {
            return screenCoords;
        }

        public Vector2 stageToScreenCoordinates(Vector2 stageCoords) {
            return stageCoords;
        }
    }

}
