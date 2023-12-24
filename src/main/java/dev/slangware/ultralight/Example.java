package dev.slangware.ultralight;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(
        modid = "ulexample",
        name = "MCUltraLight-1.8.9-example",
        acceptedMinecraftVersions = "[1.8.9]",
        version = "1.0.0-SNAPSHOT",
        clientSideOnly = true
)
public class Example  {
    /**
     * Initializes the mod.
     *
     * @param event The initialization event.
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Register this mod to the MinecraftForge event bus
        MinecraftForge.EVENT_BUS.register(this);
        // Initialize the UltraManager instance
        UltraManager.getInstance().init();
    }

    /**
     * This method is called when the F10 key is pressed.
     * It opens an example screen in Minecraft.
     */
    @SubscribeEvent
    public void onTick(InputEvent.KeyInputEvent event) {
        // Check if the F10 key is not pressed
        if (!Keyboard.isKeyDown(Keyboard.KEY_F10)) {
            return;
        }

        // Get the Minecraft instance and display the example screen
        Minecraft.getMinecraft().displayGuiScreen(ExampleScreen.getInstance());
    }
}
