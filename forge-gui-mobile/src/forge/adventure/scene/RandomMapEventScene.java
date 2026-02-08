package forge.adventure.scene;

import forge.screens.FScreen;
import forge.screens.RandomMapEventScreen;

/**
 * Custom Scene for processing random overworld map events
 */

public class RandomMapEventScene extends ForgeScene
{
    private static RandomMapEventScene instance;
    private RandomMapEventScreen screen;

    public static RandomMapEventScene instance()
    {
        if (instance == null)
        {
            instance = new RandomMapEventScene();
        }
        return instance;
    }

    private RandomMapEventScene()
    {
        super();
    }

    @Override
    public FScreen getScreen()
    {
        if (screen == null)
        {
            screen = new RandomMapEventScreen();
        }
        return screen;
    }

    public void showEvent(String title, String text)
    {
        if (screen == null)
        {
            screen = new RandomMapEventScreen();
        }
        screen.setEvent(title, text);
    }
}
