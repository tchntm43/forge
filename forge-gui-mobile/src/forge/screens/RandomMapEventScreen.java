package forge.screens;

import forge.adventure.data.DialogData;
import forge.menu.FPopupMenu;
import forge.toolbox.FOptionPane;

public class RandomMapEventScreen extends FScreen
{
    private String eventTitle = "Random Event";
    private String eventText = "Something happens on the road...";
    private DialogData currentDialog;

    public RandomMapEventScreen()
    {
        super("Random Event");
    }

    protected RandomMapEventScreen(String headerCaption)
    {
        super(headerCaption);
    }

    protected RandomMapEventScreen(String headerCaption, FPopupMenu menu)
    {
        super(headerCaption, menu);
    }

    protected RandomMapEventScreen(Header header0)
    {
        super(header0);
    }

    public void setEvent(String title, String text)
    {
        this.eventTitle = title;
        this.eventText = text;
    }

    public void setDialog(DialogData root) {
        this.currentDialog = root;
        rebuildUI();
    }

    private void rebuildUI()
    {
        //fill in later
    }

    @Override
    protected void doLayout(float startY, float width, float height)
    {
        //fill in later
    }
}
