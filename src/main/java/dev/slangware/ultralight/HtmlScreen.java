package dev.slangware.ultralight;


import dev.slangware.ultralight.listener.UILoadListener;
import net.janrupf.ujr.api.UltralightRenderer;
import net.janrupf.ujr.api.UltralightView;
import net.janrupf.ujr.api.UltralightViewConfigBuilder;
import net.janrupf.ujr.api.config.UlViewConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class HtmlScreen extends GuiScreen {
    private static final ViewController viewController;

    static {
        UlViewConfig config = new UltralightViewConfigBuilder()
                .transparent(true)
                .enableImages(true)
                .enableJavascript(true)
                .build();

        UltralightRenderer renderer = UltralightRenderer.getOrCreate();

        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());

        UltralightView view = renderer.createView(resolution.getScaledWidth(), resolution.getScaledHeight(), config);
        viewController = new ViewController(renderer, view);
    }

    private String url = "";
    private boolean ready;
    private boolean initialized;
    private GuiScreen parentScreen;

    public HtmlScreen() {

    }

    public HtmlScreen(String url) {
        this.url = url;
    }

    public HtmlScreen(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    public HtmlScreen(GuiScreen parentScreen, String url) {
        this.parentScreen = parentScreen;
        this.url = url;
    }

    /**
     * Generates a ModelBuilder object.
     *
     * @return a ModelBuilder object
     */
    public static ModelBuilder modelBuilder() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return ModelBuilder.builder()
                .append("USERNAME", minecraft.thePlayer != null ? minecraft.thePlayer.getName() : minecraft.getSession().getUsername())
                .append("USER_UUID", minecraft.thePlayer != null ? minecraft.thePlayer.getName() : minecraft.getSession().getPlayerID());
    }


    /**
     * Sets the ready status to true and performs necessary actions when the document is ready.
     */
    public void onDocReady() {
        this.ready = true;

        viewController.resize(this.getWidth(), this.getHeight());
        UltraManager.getLogger().info(this.getClass().getSimpleName() + " Loaded");

        viewController.focus();
    }

    /**
     * Initializes the graphical user interface.
     */
    @Override
    public void initGui() {
        if (initialized) return;

        initialized = true;

        if (viewController.hasFocus()) {
            viewController.unfocus();
        }

        viewController.setLoadListener(new UILoadListener() {
            @Override
            public void onFinishLoading(UltralightView view, long frameId, boolean isMainFrame, String url) {
                if (isMainFrame) onDocReady();
            }
        });

        if (url.startsWith("http")) {
            viewController.loadURL(url);
        } else {
            viewController.loadURL("http://127.0.0.1:" + UltraManager.SERVER_PORT + "/" + url);
        }

        UltraManager.addScreen(this);
    }


    /**
     * Draws the screen at the specified coordinates.
     *
     * @param x the x-coordinate of the screen
     * @param y the y-coordinate of the screen
     * @param p the float value
     */
    @Override
    public void drawScreen(int x, int y, float p) {
        // Check if the screen is initialized
        if (!initialized) {
            return;
        }

        // Update the view controller
        viewController.update();

        // Check if the screen is ready
        if (!ready) {
            // Draw the default background
            this.drawDefaultBackground();
            return;
        }

        // Render the view controller
        viewController.render();

        // Check if a parent screen exists
        if (parentScreen != null) {
            // Draw the parent screen
            parentScreen.drawScreen(x, y, p);
        }
    }


    /**
     * Called when the Minecraft window is resized.
     * Resizes the view controller if the new width or height is different from the current width or height.
     *
     * @param mc The Minecraft instance.
     * @param w  The new width of the Minecraft window.
     * @param h  The new height of the Minecraft window.
     */
    @Override
    public void onResize(Minecraft mc, int w, int h) {
        int currentWidth = (int) viewController.width();
        int currentHeight = (int) viewController.height();

        int newWidth = this.getWidth();
        int newHeight = this.getHeight();

        if (newWidth != currentWidth || newHeight != currentHeight) {
            viewController.resize(newWidth, newHeight);
        }

        super.onResize(mc, w, h);
    }

    /**
     * Gets the width of the display.
     *
     * @return the width of the display
     */
    private int getWidth() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc.displayWidth <= 0 ? Display.getWidth() : mc.displayWidth;
    }

    /**
     * Retrieves the height of the display.
     *
     * @return the height of the display
     */
    private int getHeight() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc.displayHeight <= 0 ? Display.getHeight() : mc.displayHeight;
    }

    /**
     * This method called when user clicked at window
     *
     * @param x           the x coordinate of the mouse pointer
     * @param y           the y coordinate of the mouse pointer
     * @param mouseButton the button that was clicked (0 for left, 1 for right, 2 for middle)
     */
    @Override
    public void mouseClicked(int x, int y, int mouseButton) {
        viewController.onMouseClick(x, y, mouseButton, true);
    }

    /**
     * normal mouse handler for Minecraft
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int scrollAmount = Mouse.getDWheel();

        if (scrollAmount != 0) {
            viewController.onScrollMouse(scrollAmount);
        }

        int deltaX = Mouse.getDX();
        int deltaY = Mouse.getDY();
        if (deltaX != 0 || deltaY != 0) {
            viewController.onMouseMove(mouseX, mouseY);
        }

        super.handleMouseInput();
    }


    /**
     * This method called when user release click at window
     *
     * @param x           the x coordinate of the mouse pointer
     * @param y           the y coordinate of the mouse pointer
     * @param mouseButton the button that was clicked (0 for left, 1 for right, 2 for middle)
     */
    @Override
    public void mouseReleased(int x, int y, int mouseButton) {
        viewController.onMouseClick(x, y, mouseButton, false);
    }


    /**
     * Handles the keyboard input by retrieving the event key and character from the Keyboard class.
     * Calls the `onKeyDown` method of the `viewController` object with the event character and key as arguments.
     * Invokes the `keyTyped` method of the current object with the event character and key as arguments.
     * Dispatches the key presses to the Minecraft client.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void handleKeyboardInput() throws IOException {
        int eventKey = Keyboard.getEventKey();
        char eventCharacter = Keyboard.getEventCharacter();

        viewController.onKeyDown(eventCharacter, eventKey);
        this.keyTyped(eventCharacter, eventKey);
        this.mc.dispatchKeypresses();
    }

    /**
     * This method is called when the GUI is closed.
     */
    @Override
    public void onGuiClosed() {
        this.ready = false;
        this.initialized = false;

        UltraManager.getLogger().info(this.getClass().getSimpleName() + " Unloaded");

        viewController.destroy();
        UltraManager.removeScreen(this);
    }


    /**
     * Reloads the view of the controller.
     */
    public void reload() {
        viewController.reload();
    }


    public static class ModelBuilder {
        private final Map<String, String> map = new HashMap<>();

        /**
         * Create a new ModelBuilder instance.
         *
         * @return The newly created ModelBuilder instance.
         */
        public static ModelBuilder builder() {
            return new ModelBuilder();
        }

        /**
         * Appends a key-value pair to the model.
         *
         * @param key   the key to append
         * @param value the value to append
         * @return the ModelBuilder instance
         */
        public ModelBuilder append(String key, String value) {
            if (key == null || value == null) return this;
            map.put(key, value);
            return this;
        }

        /**
         * Builds and returns a Map of String keys and String values.
         *
         * @return The built Map of String keys and String values.
         */
        public Map<String, String> build() {
            return map;
        }
    }

}