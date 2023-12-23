package dev.slangware.ultralight;

import dev.slangware.ultralight.listener.UILoadListener;
import dev.slangware.ultralight.util.UltralightKeyMapper;
import net.janrupf.ujr.api.*;
import net.janrupf.ujr.api.event.*;
import net.janrupf.ujr.api.filesystem.UltralightFilesystem;
import net.janrupf.ujr.api.javascript.JavaScriptException;
import net.janrupf.ujr.api.math.IntRect;
import net.janrupf.ujr.api.surface.UltralightSurface;
import net.janrupf.ujr.api.util.UltralightBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;


public class ViewController {

    private final UltralightPlatform platform;
    private final UltralightRenderer renderer;
    private final UltralightView view;
    private int glTexture;
    private double scale;


    public ViewController(UltralightRenderer renderer, UltralightView view) {
        this.platform = UltralightPlatform.instance();

        this.renderer = renderer;

        this.view = view;

        this.view.setLoadListener(new UILoadListener());

        this.glTexture = -1;

        Keyboard.enableRepeatEvents(true);
    }

    /**
     * Retrieves the UltralightView associated with this object.
     *
     * @return the UltralightView associated with this object
     */
    public UltralightView getView() {
        return view;
    }

    /**
     * Load a URL, the View will navigate to it as a new page.
     * <p>
     * You can use File URLs (eg, file:///page.html) but you must define your own FileSystem
     * implementation. {@link UltralightPlatform#setFilesystem(UltralightFilesystem)}
     *
     * @param url the URL to load
     */
    public void loadURL(String url) {
        this.view.loadURL(url);
    }


    /**
     * Updates the function.
     */
    public void update() {
        this.renderer.update();
        if (!view.needsPaint()) return;
        this.renderer.render();
    }

    /**
     * Resizes the view to the specified width and height.
     *
     * @param width  the new width of the view
     * @param height the new height of the view
     */
    public void resize(int width, int height) {
        this.view.resize(width, height);
    }


    /**
     * Renders the texture on the screen. some codes from <a href="https://github.com/DaveH355/ultralight-forge-1.8.9">ultralight-forge-1.8.9</a>
     */
    public void render() {
        if (glTexture == -1) createGLTexture();

        UltralightSurface surface = view.surface();

        int width = (int) view.width();
        int height = (int) view.height();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GlStateManager.bindTexture(glTexture);

        IntRect dirtyBounds = surface.dirtyBounds();

        if (dirtyBounds.isValid()) {
            try (UltralightBuffer buffer = surface.lockPixels()) {
                ByteBuffer byteBuffer = buffer.asByteBuffer();

                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, (int) surface.rowBytes() / 4);

                if (dirtyBounds.width() == width && dirtyBounds.height() == height) {
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D,
                            0,
                            GL11.GL_RGBA8,
                            width,
                            height,
                            0,
                            GL12.GL_BGRA,
                            GL_UNSIGNED_INT_8_8_8_8_REV,
                            byteBuffer);
                    GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
                } else {
                    int x = dirtyBounds.x();
                    int y = dirtyBounds.y();
                    int dirtyWidth = dirtyBounds.width();
                    int dirtyHeight = dirtyBounds.height();
                    int startOffset = (int) ((y * surface.rowBytes()) + x * 4);

                    GL11.glTexSubImage2D(
                            GL11.GL_TEXTURE_2D,
                            0,
                            x, y, dirtyWidth, dirtyHeight,
                            GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
                            (ByteBuffer) byteBuffer.position(startOffset));

                }

                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            }

            surface.clearDirtyBounds();
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TRANSFORM_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4f(1, 1, 1, 1f);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer bufferBuilder = tessellator.getWorldRenderer();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bufferBuilder.pos(0, 0, 0).tex(0, 0).endVertex();
        bufferBuilder.pos(0, height, 0).tex(0, 1).endVertex();
        bufferBuilder.pos(width, height, 0).tex(1, 1).endVertex();
        bufferBuilder.pos(width, 0, 0).tex(1, 0).endVertex();
        tessellator.draw();

        GlStateManager.bindTexture(0);

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPopAttrib();

        int error = GL11.glGetError();

        if (error != GL11.GL_NO_ERROR) {
            try {
                view.evaluateScript("alert(\"" + error + "\")");
            } catch (JavaScriptException ignored) {

            }
            UltraManager.getLogger().error(error);
        }

    }

    /**
     * Creates a GL texture.
     */
    private void createGLTexture() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        this.glTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTexture);

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /**
     * Handles the mouse click event.
     *
     * @param x           the x-coordinate of the mouse click
     * @param y           the y-coordinate of the mouse click
     * @param mouseButton the button that was clicked (0 for left, 1 for right, 2 for middle)
     * @param buttonDown  indicates whether the button was down or up
     */
    public void onMouseClick(int x, int y, int mouseButton, boolean buttonDown) {
        UltralightMouseEventBuilder builder;
        UlMouseButton button;

        switch (mouseButton) {
            case 0:
                button = UlMouseButton.LEFT;
                break;
            case 1:
                button = UlMouseButton.RIGHT;
                break;
            case 2:
                button = UlMouseButton.MIDDLE;
                break;
            default:
                button = UlMouseButton.NONE;
                break;
        }


        if (buttonDown) {
            builder = UltralightMouseEventBuilder.down(button);
        } else {
            builder = UltralightMouseEventBuilder.up(button);
        }

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int scaleFactor = scaledResolution.getScaleFactor();

        view.fireMouseEvent(builder
                .x(x * scaleFactor)
                .y(y * scaleFactor)
                .build());
    }

    /**
     * Handles the mouse move event.
     *
     * @param x the x coordinate of the mouse pointer
     * @param y the y coordinate of the mouse pointer
     */
    public void onMouseMove(int x, int y) {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int scaleFactor = scaledResolution.getScaleFactor();

        view.fireMouseEvent(UltralightMouseEventBuilder.moved()
                .x(x * scaleFactor)
                .y(y * scaleFactor)
                .button(UlMouseButton.NONE)
                .build());
    }

    /**
     * Handles the scrolling of the mouse.
     *
     * @param w the amount of scrolling to be done
     */
    public void onScrollMouse(int w) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            double newDeviceScale = Math.min(Math.max(scale + (double) w / 715, 1), 10);
            this.setDeviceScale(newDeviceScale);
            return;
        }

        UlScrollEvent scrollEvent = new UltralightScrollEventBuilder(UlScrollEventType.BY_PIXEL)
                .deltaY(w)
                .deltaY(w)
                .build();

        this.view.fireScrollEvent(scrollEvent);
    }


    /**
     * Handles the key down event.
     *
     * @param c   the character representing the key pressed
     * @param key the virtual key code of the key pressed
     */
    public void onKeyDown(char c, int key) {
        UltralightKeyEventBuilder builder;

        if (Character.isLetterOrDigit(c) || c == '-' || c == '*' || c == '`' || c == '/' || c == '+' ||
                c == ' ' || c == '!' || c == '@' || c == '#' || c == '$' || c == '%' || c == '^' ||
                c == '&' || c == ')' || c == '(' || c == '_' || c == '=' || c == '{' || c == '}' ||
                c == '[' || c == ']' || c == ':' || c == ';' || c == '\'' || c == '"' || c == '\\' ||
                c == '|' || c == '<' || c == '>' || c == '?' || c == '؟' || (c >= 'À' && c <= 'ÿ') ||
                c == '»' || c == '«' || c == 'ـ' || c == '~' || c == 'ً' || c == 'ٌ' || c == 'ٍ' ||
                c == '،' || c == '؛' || c == ',' || c == 'ّ' || c == 'ۀ' || c == 'آ' || c == 'َ' ||
                c == 'ُ' || c == 'ِ' || c == 'ة' || c == 'ي' || c == 'ژ' || c == 'إ' || c == 'أ' ||
                c == 'ء' || c == 'ؤ') {
            String text = new String(Character.toChars(c));
            builder = UltralightKeyEventBuilder.character()
                    .unmodifiedText(text)
                    .text(text);
        } else if (Keyboard.getEventKeyState()) {
            builder = UltralightKeyEventBuilder.down();
        } else {
            builder = UltralightKeyEventBuilder.up();
        }

        builder.nativeKeyCode(c)
                .autoRepeat(Keyboard.isRepeatEvent())
                .virtualKeyCode(UltralightKeyMapper.lwjglKeyToUltralight(key))
                .keyIdentifier(UlKeyEvent.keyIdentifierFromVirtualKeyCode(builder.virtualKeyCode))
                .modifiers(UltralightKeyMapper.lwjglModifiersToUltralight());

        this.view.fireKeyEvent(builder.build());

        // Manually synthesize enter and tab
        if (builder.type == UlKeyEventType.DOWN && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_TAB)) {
            this.view.fireKeyEvent(UltralightKeyEventBuilder.character()
                    .unmodifiedText(key == Keyboard.KEY_RETURN ? "\n" : "\t")
                    .text(key == Keyboard.KEY_RETURN ? "\n" : "\t"));
        }

        // Manually synthesize reload
        if (builder.type == UlKeyEventType.DOWN && (key == Keyboard.KEY_R && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) && !Keyboard.isRepeatEvent()) {
            UltraManager.getLogger().info("Reloading page");
            view.reload();
        }
    }

    /**
     * Sets the load listener for the UI.
     *
     * @param loadListener the load listener to be set
     */
    public void setLoadListener(UILoadListener loadListener) {
        this.view.setLoadListener(loadListener);
    }

    /**
     * Reloads the view.
     */
    public void reload() {
        this.view.reload();
    }

    /**
     * Focuses on the view.
     */
    public void focus() {
        this.view.focus();
    }

    /**
     * Determines if the function has focus.
     *
     * @return true if the function has focus, false otherwise
     */
    public boolean hasFocus() {
        return this.view.hasFocus();
    }

    /**
     * Destroys the object by stopping the view and unfocusing it.
     */

    public void destroy() {
        this.getView().stop();
        this.getView().unfocus();
    }

    /**
     * Evaluates a JavaScript script.
     *
     * @param s the JavaScript script to evaluate
     * @return the result of the evaluation
     * @throws JavaScriptException if an error occurs during script evaluation
     */
    public String evaluateScript(String s) throws JavaScriptException {
        return this.view.evaluateScript(s);
    }

    /**
     * Loads the specified HTML string into the view.
     *
     * @param s the HTML string to be loaded
     */
    public void loadHTML(String s) {
        this.view.loadHTML(s);
    }

    /**
     * Sets the device scale for the view.
     *
     * @param scale the scale value to set
     */
    public void setDeviceScale(double scale) {
        this.scale = scale;
//        this.view.setDeviceScale(scale);
        try {
            String transformOrigin = "left top";
            String transform = "scale(" + this.scale + ")";

            this.evaluateScript("document.body.style.transformOrigin = \"" + transformOrigin + "\"");
            this.evaluateScript("document.body.style.transform = \"" + transform + "\"");
        } catch (JavaScriptException e) {
            UltraManager.getLogger().throwing(e);
        }
    }

    /**
     * Unfocuses the view.
     */
    public void unfocus() {
        this.view.unfocus();
    }

    /**
     * Retrieves the Ultralight platform.
     *
     * @return the Ultralight platform
     */
    public UltralightPlatform getPlatform() {
        return platform;
    }
}