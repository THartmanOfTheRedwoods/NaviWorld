package info.cotr.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
// import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private Stage stage;
    private Skin skin;
    // Re-Usable lock that multiple threads can use to wait on multiple lock conditions.
    private final ReentrantLock lock = new ReentrantLock();
    private Condition condition1; // This is a condition from a ReentrantLock used to trigger some game thread.
    private final GameThread[] gameThreads = new GameThread[2];

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);  // Make input go to stage before game app.

        /*
        // Debug statements that helped me determine if uiskin atlas and json files were correctly defined.
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("uiskin.atlas"));
        for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
            System.out.println("Atlas contains region: " + region.name);
        }
        System.out.println(Gdx.files.internal("uiskin.atlas").file().getAbsolutePath());
        */

        // Load the default UI skin
        skin = new Skin();
        skin.addRegions(new TextureAtlas(Gdx.files.internal("uiskin.atlas")));
        // Generate a BitmapFont from the TTF file
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("UnifrakturMaguntia-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;  // Set font size
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();  // Dispose to avoid memory leaks
        // Add the generated font to the skin
        skin.add("default-font", font);

        skin.load(Gdx.files.internal("uiskin.json"));

        // Create a window (i.e. more like a draggable component inside the game window)
        Window window = new Window("Super Duper", skin);
        window.setSize(300, 200);
        window.setPosition(Gdx.graphics.getWidth() / 2f - 150, Gdx.graphics.getHeight() / 2f - 100); // Center it

        // Let's set up the Game threads and thread control mechanisms.
        condition1 = lock.newCondition();
        for(int i=0; i < this.gameThreads.length; i++) {
            gameThreads[i] = new GameThread(lock, condition1, "gt" + i);
            gameThreads[i].start();
        }

        // Create a button
        TextButton button = new TextButton("Click Me!", skin);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Button Clicked");
                signalGameThreads(condition1);  // Signal all threads waiting on condition1
            }
        });

        // Add the button to the window
        window.add(button).pad(20);
        window.row(); // Move to next row in row layout

        // Make the window draggable
        //window.setMovable(true);
        // Make the ENTIRE window draggable, not just the title bar
        window.addListener(new InputListener() {
            private float startX, startY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                startX = x;
                startY = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                window.moveBy(x - startX, y - startY);
            }
        });

        // Add window to the stage
        stage.addActor(window);

    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    private void signalGameThreads(Condition condition) {
        lock.lock();  // Acquire the ReentrantLock so we can signal waiting threads. Prevents illegal monitor ex
        try {
            condition.signalAll(); // Since each condition is a Q, this signals all waiting threads in the Q
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void dispose() {
        // Notify all game threads that the game is over
        for (GameThread gameThread : this.gameThreads) { gameThread.gameOver(); }
        signalGameThreads(condition1);  // Signal all threads waiting on condition1 to check if the game is over.
        for (Thread gameThread : this.gameThreads) {  // Wait for game threads to re-join main thread.
            try { gameThread.join(); }
            catch (InterruptedException ignored) { }
        }
        batch.dispose();
        image.dispose();
        stage.dispose();
        skin.dispose();
    }
}
