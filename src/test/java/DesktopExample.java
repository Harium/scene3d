import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.harium.gdx.scenes.scene3d.Stage3d;

public class DesktopExample extends Game {

    public Stage3d stage3d;

    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public CameraInputController camController;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(940, 640);
        config.setTitle("Desktop Example");
        config.useVsync(false);
        new Lwjgl3Application(new DesktopExample(), config);
    }

    @Override
    public void create() {
        modelBatch = new ModelBatch();
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(1f, 1f, 1f);
        cam.lookAt(0,0,0);
        cam.near = 0.01f;
        cam.far = 300f;
        cam.update();

        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);

        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGB888, 512, 512, false);

        stage3d = new Stage3d(fbo, new Vector3(), 1, 1);
        stage3d.background.set(Color.WHITE);

        Skin skin = new Skin(Gdx.files.internal("flat/skin.json"));
        Stage stage = stage3d.stage;

        float buttonWidth = 200;
        float buttonHeight = 80;

        TextButton button = new TextButton("Button", skin);
        button.setPosition(256 - buttonWidth / 2, 256 - buttonHeight / 2);
        button.setSize(buttonWidth, buttonHeight);
        stage.addActor(button);

        stage3d.act();
    }

    @Override
    public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(cam);
        modelBatch.render(stage3d.instance);
        modelBatch.end();

        handleInputs();
    }

    private void handleInputs() {
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();

        Ray ray = cam.getPickRay(x, y);
        stage3d.onMove(0, ray);

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            stage3d.onPressed(Input.Buttons.LEFT);
        } else {
            stage3d.onRelease(Input.Buttons.LEFT);
        }

        camController.update();
    }

    @Override
    public void dispose() {
        stage3d.dispose();
    }
}
