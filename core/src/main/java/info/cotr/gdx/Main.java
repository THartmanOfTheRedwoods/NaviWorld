package info.cotr.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private static final int WORLD_WIDTH = 100; // Width of the world in tiles
    private static final int WORLD_HEIGHT = 100; // Height of the world in tiles
    private static final int TILE_SIZE = 124; // Size of each tile in pixels
    private static final int CAMERA_VIEW_THRESHOLD = 2; // Number of tiles outside the camera view to render

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Texture tileTexture;
    private Texture playerTexture;

    private Vector2 playerPosition;
    private float playerSpeed = 200f; // Speed in pixels per second

    @Override
    public void create() {
        // Initialize the camera
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera = new OrthographicCamera(w, h);
        camera.setToOrtho(false, w, h);

        // Initialize the sprite batch
        batch = new SpriteBatch();

        // Load textures
        //tileTexture = new Texture("brick.png");
        tileTexture = new Texture("honeycomb.png");
        playerTexture = new Texture("player.png");

        // Initialize player position
        playerPosition = new Vector2(WORLD_WIDTH * TILE_SIZE / 2f, WORLD_HEIGHT * TILE_SIZE / 2f);
    }

    @Override
    public void render() {
        // Clear the screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update player movement
        handleInput();

        // Update camera position to follow the player
        camera.position.set(playerPosition.x, playerPosition.y, 0);
        camera.update();

        // Set the sprite batch's projection matrix to the camera's combined matrix
        batch.setProjectionMatrix(camera.combined);

        // Begin drawing
        batch.begin();

        // Render the visible tiles
        int startX = (int) ((camera.position.x - camera.viewportWidth / 2) / TILE_SIZE) - CAMERA_VIEW_THRESHOLD;
        int startY = (int) ((camera.position.y - camera.viewportHeight / 2) / TILE_SIZE) - CAMERA_VIEW_THRESHOLD;
        int endX = (int) ((camera.position.x + camera.viewportWidth / 2) / TILE_SIZE) + CAMERA_VIEW_THRESHOLD;
        int endY = (int) ((camera.position.y + camera.viewportHeight / 2) / TILE_SIZE) + CAMERA_VIEW_THRESHOLD;

        for (int x = Math.max(0, startX); x < Math.min(WORLD_WIDTH, endX); x++) {
            for (int y = Math.max(0, startY); y < Math.min(WORLD_HEIGHT, endY); y++) {
                batch.draw(tileTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Render the player
        batch.draw(playerTexture, playerPosition.x - TILE_SIZE / 2f, playerPosition.y - TILE_SIZE / 2f, TILE_SIZE, TILE_SIZE);

        // End drawing
        batch.end();
    }

    private void handleInput() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        Vector2 movement = new Vector2();

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            movement.y += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            movement.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            movement.x -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            movement.x += 1;
        }

        // Normalize diagonal movement
        if (movement.len2() > 0) {
            movement.nor().scl(playerSpeed * deltaTime);
            playerPosition.add(movement);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        tileTexture.dispose();
        playerTexture.dispose();
    }
}
