package forge.screens;

import forge.adventure.data.DialogData;
import forge.menu.FPopupMenu;
import forge.toolbox.FOptionPane;

public class NomadsBazaarScreen extends FScreen
{
    private String eventTitle = "Nomad's Bazaar";
    private String eventText = "";
    private DialogData currentDialog;

    public NomadsBazaarScreen()
    {
        super("Nomad's Bazaar");
    }

    protected NomadsBazaarScreen(String headerCaption)
    {
        super(headerCaption);
    }

    protected NomadsBazaarScreen(String headerCaption, FPopupMenu menu)
    {
        super(headerCaption, menu);
    }

    protected NomadsBazaarScreen(Header header0)
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

