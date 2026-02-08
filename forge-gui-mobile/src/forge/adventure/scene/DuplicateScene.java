package forge.adventure.scene;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Controls;
import forge.adventure.util.Reward;
import forge.adventure.util.RewardActor;
import forge.item.PaperCard;

public class DuplicateScene extends UIScene
{
    private static DuplicateScene instance;
    private Array<PaperCard> duplicateOptions;
    private Array<Actor> cardActors = new Array<>();
    private Array<Array<PaperCard>> cardRows = new Array<>();

    private static final float BASE_CARD_WIDTH   = 120f;
    private static final float CARD_ASPECT       = 680f / 488f;
    private static final float MAX_ROW_WIDTH     = 350f;
    private static final int   MAX_CARDS_PER_ROW = 10;
    private static final float CARD_GAP          = 6f;

    private TextraButton doneButton;
    private Array<TextraButton> cardButtons = new Array<>();

    private Group cardContentGroup;
    private ScrollPane cardScrollPane;
    private static final float CARD_AREA_X      = 20f;
    private static final float CARD_AREA_Y      = 10f;
    private static final float CARD_AREA_WIDTH  = 350f;
    private static final float CARD_AREA_HEIGHT = 230f;
    private static final float CARD_AREA_HEIGHT_TRADE = 215f;
    private Image borderTop, borderBottom, borderLeft, borderRight;
    private static Texture borderTex;

    private PaperCard duplicateCard;

    public static DuplicateScene getInstance()
    {
        if(instance == null)
        {
            instance = new DuplicateScene();
        }
        return instance;
    }

    private DuplicateScene()
    {
        super(Forge.isLandscapeMode() ? "ui/items.json" : "ui/items_portrait.json");
        duplicateCard = null;
        showBackground();
        initGui();
    }

    private void initGui()
    {
        Actor restockButton = ui.findActor("restock");
        Actor detailButton  = ui.findActor("detail");
        if (restockButton != null) {
            restockButton.setVisible(false);
        }
        if (detailButton != null) {
            detailButton.setVisible(false);
        }
        doneButton = ui.findActor("done");
        if (doneButton != null)
        {
            doneButton.setText("Duplicate");
            doneButton.setDisabled(true);
        }
        Actor goldIcon   = ui.findActor("gold");
        Actor goldLabel  = ui.findActor("goldLabel");
        Actor shardsIcon = ui.findActor("shards");
        Actor shardsLabel = ui.findActor("shardsLabel");
        if (goldIcon != null)   goldIcon.setVisible(false);
        if (goldLabel != null)  goldLabel.setVisible(false);
        if (shardsIcon != null) shardsIcon.setVisible(false);
        if (shardsLabel != null) shardsLabel.setVisible(false);
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null) {
            shopName.setVisible(false);
        }
        ui.onButtonPress("done", () -> {
            if(duplicateCard != null)
            {
                instance = null;
                Actor duplicateBg = ui.findActor("duplicate_background");
                duplicateBg.setVisible(false);
                finishDuplicate();
            }
        });

        ensureBorderTexture();

        cardContentGroup = new Group();
        cardScrollPane = new ScrollPane(cardContentGroup);
        cardScrollPane.setScrollingDisabled(true, false);
        cardScrollPane.setFadeScrollBars(false);
        cardScrollPane.setSmoothScrolling(true);
        cardScrollPane.setOverscroll(false, false);

        cardScrollPane.setBounds(
                CARD_AREA_X,
                CARD_AREA_Y,
                CARD_AREA_WIDTH,
                CARD_AREA_HEIGHT
        );
        stage.addActor(cardScrollPane);
    }

    private void finishDuplicate()
    {
        AdventurePlayer.current().addCard(duplicateCard, 1);
        Forge.switchToLast();
    }

    public void setDuplicateOptions(Array<PaperCard> options)
    {
        duplicateOptions = options;
        showCards();
    }

    private void showBackground()
    {
        if (ui == null)
        {
            return;
        }

        Actor duplicateBg = ui.findActor("duplicate_background");
        if (duplicateBg != null)
        {
            duplicateBg.setVisible(true);
        }
    }

    private void ensureBorderTexture()
    {
        if (borderTex != null) return;

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0.8f, 0.8f, 0.8f, 0.5f);
        pm.fill();
        borderTex = new Texture(pm);
        pm.dispose();
    }

    private void showCards()
    {
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null)
        {
            shopName.setText("Choose a card to duplicate");
            shopName.setVisible(true);
        }

        cardContentGroup.clearChildren();
        cardActors.clear();
        cardScrollPane.setVisible(true);

        calculateCardRows();

        if (cardRows.size == 0)
        {
            return;
        }
        int maxCardsInAnyRow = 0;
        for (Array<PaperCard> row : cardRows)
        {
            int rowSize = row.size;
            if (rowSize > maxCardsInAnyRow)
            {
                maxCardsInAnyRow = rowSize;
            }
        }
        if (maxCardsInAnyRow <= 0)
        {
            return;
        }
        float buttonHeight = doneButton != null ? doneButton.getHeight() : 20f;

        float cardWidth;
        if (maxCardsInAnyRow <= 2)
        {
            cardWidth = BASE_CARD_WIDTH;
        }
        else
        {
            float totalGap = CARD_GAP * (maxCardsInAnyRow - 1);
            cardWidth = (MAX_ROW_WIDTH - totalGap) / maxCardsInAnyRow;
            if (cardWidth > BASE_CARD_WIDTH)
            {
                cardWidth = BASE_CARD_WIDTH;
            }
        }
        float cardHeight = cardWidth * CARD_ASPECT;

        float rowHeight = cardHeight + buttonHeight + CARD_GAP;
        int numRows = cardRows.size;
        float totalContentHeight = numRows * rowHeight + CARD_GAP;

        cardContentGroup.setSize(MAX_ROW_WIDTH, totalContentHeight);

        drawScrollPaneBorder(CARD_AREA_X, CARD_AREA_Y, CARD_AREA_WIDTH, CARD_AREA_HEIGHT);

        for (int rowIndex = 0; rowIndex < cardRows.size; rowIndex++)
        {
            Array<PaperCard> row = cardRows.get(rowIndex);
            int n = row.size;
            if (n <= 0)
            {
                continue;
            }

            float rowWidth  = n * cardWidth + (n - 1) * CARD_GAP;
            float rowStartX = (MAX_ROW_WIDTH - rowWidth) / 2f;

            float rowTopY = totalContentHeight - CARD_GAP - rowIndex * rowHeight;

            int x_count = 0;
            for (PaperCard card : row)
            {
                if (card == null)
                {
                    continue;
                }

                Reward r = new Reward(card, false);
                RewardActor actor = new RewardActor(r, false, RewardScene.Type.Shop, false);

                float x      = rowStartX + x_count * (cardWidth + CARD_GAP);
                float cardY  = rowTopY - cardHeight;
                actor.setBounds(x, cardY, cardWidth, cardHeight);
                cardContentGroup.addActor(actor);
                cardActors.add(actor);

                TextraButton cardButton =
                        new TextraButton("", doneButton.getStyle(), Controls.getTextraFont());

                float buttonY = rowTopY - cardHeight - buttonHeight;

                cardButton.setBounds(x, buttonY, cardWidth, buttonHeight);

                cardButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float xx, float yy) {
                        toggleCardButton(cardButton);
                    }
                });

                cardButton.setText("");

                DuplicateScene.CardButtonData data = new DuplicateScene.CardButtonData();
                data.card     = card;
                data.actor    = actor;
                cardButton.setUserObject(data);

                cardContentGroup.addActor(cardButton);
                cardButtons.add(cardButton);

                x_count++;
            }
        }
        cardScrollPane.layout();
        stage.setScrollFocus(cardScrollPane);
    }

    private void toggleCardButton(TextraButton cardButton)
    {
        Object obj = cardButton.getUserObject();
        if (!(obj instanceof DuplicateScene.CardButtonData))
        {
            return;
        }

        DuplicateScene.CardButtonData data = (DuplicateScene.CardButtonData) obj;

        //deselect all other buttons
        for(TextraButton btn : cardButtons)
        {
            if(btn != cardButton)
            {
                ((CardButtonData) btn.getUserObject()).selected = false;
                btn.setText("");
            }
        }

        if(data.selected)
        {
            data.selected = false;
            cardButton.setText("");
            duplicateCard = null;
            doneButton.setDisabled(true);
        }
        else
        {
            data.selected = true;
            cardButton.setText("X");
            duplicateCard = data.card;
            doneButton.setDisabled(false);
        }
    }

    private void calculateCardRows()
    {
        Array<PaperCard> tempRow;
        int rowCounter;
        cardRows.clear();
        if (duplicateOptions == null || duplicateOptions.isEmpty())
        {
            return;
        }
        tempRow = new Array<>();
        rowCounter = 0;
        for (PaperCard card : duplicateOptions)
        {
            if (rowCounter == MAX_CARDS_PER_ROW)
            {
                cardRows.add(tempRow);
                rowCounter = 0;
                tempRow = new Array<>();
            }
            if (card == null)
            {
                continue;
            }
            tempRow.add(card);
            rowCounter++;
        }
        if (!tempRow.isEmpty())
        {
            cardRows.add(tempRow);
        }
    }

    private void drawScrollPaneBorder(float xLeft, float yBottom, float width, float height)
    {
        if (borderTex == null)
        {
            ensureBorderTexture();
        }
        float thickness = 2f;
        if(borderTop == null)
        {
            Drawable borderDrawable = new TextureRegionDrawable(new TextureRegion(borderTex));

            borderTop = new Image(borderDrawable);
            borderBottom = new Image(borderDrawable);
            borderLeft = new Image(borderDrawable);
            borderRight = new Image(borderDrawable);

            stage.addActor(borderTop);
            stage.addActor(borderBottom);
            stage.addActor(borderLeft);
            stage.addActor(borderRight);
        }

        // Top horizontal line: directly above the card area
        borderTop.setBounds(
                xLeft,
                yBottom + height,
                width,
                thickness
        );

        // Bottom horizontal line: slightly below the card area
        borderBottom.setBounds(
                xLeft,
                yBottom - thickness,
                width,
                thickness
        );

        // Left vertical line
        borderLeft.setBounds(
                xLeft - thickness,
                yBottom - thickness,
                thickness,
                height + 2 * thickness
        );

        // Right vertical line
        borderRight.setBounds(
                xLeft + width,
                yBottom - thickness,
                thickness,
                height + 2 * thickness
        );
    }

    public static class CardButtonData
    {
        public PaperCard   card;
        public RewardActor actor;
        public boolean     selected = false;
    }
}
