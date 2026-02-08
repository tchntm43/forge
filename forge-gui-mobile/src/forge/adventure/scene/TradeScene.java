package forge.adventure.scene;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Controls;
import forge.adventure.util.TradeController;
import forge.adventure.util.Reward;
import forge.adventure.util.RewardActor;
import forge.deck.CardPool;
import forge.item.PaperCard;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;

import java.util.Map;


public class TradeScene extends UIScene
{
    private static TradeScene instance;
    private mode currentMode;
    private Array<Actor> playerCardActors = new Array<>();
    private Array<Actor> cardActors = new Array<>();

    private enum mode
    {
        SelectPlayerTradeCards,
        SelectEnemyCardsWanted,
        ViewEnemyChoices,
        CreateTrade,
        EnemyOffer,
        TradeComplete,
        NoTradePossible
    }

    private static final float BASE_CARD_WIDTH   = 120f;
    private static final float CARD_ASPECT       = 680f / 488f;
    private static final float MAX_ROW_WIDTH     = 350f;
    private static final int   MAX_CARDS_PER_ROW = 10;
    private static final float CARD_GAP          = 6f;
    private Array<CardPool> playerCardRows = new Array<>();
    private Array<Array<PaperCard>> cardRows = new Array<>();
    private Array<Array<PaperCard>> enemyCardRows = new Array<>();

    private TextraButton doneButton;
    private TextraButton nextButton;
    private TextraButton declineButton;
    private static final float NEXT_BUTTON_GAP = 10f;
    private Array<TextraButton> playerCardButtons = new Array<>();
    private Array<TextraButton> enemyCardButtons = new Array<>();
    private TypingLabel enemyCardsLabel;
    private TypingLabel playerCardsLabel;
    private boolean playerAcceptedCounterOffer;
    private boolean nextDisabled = false;

    private Group playerCardContentGroup;
    private ScrollPane playerCardScrollPane;
    private Group cardContentGroup;
    private ScrollPane cardScrollPane;

    private static final float CARD_AREA_X      = 20f;
    private static final float CARD_AREA_Y      = 10f;
    private static final float CARD_AREA_WIDTH  = 350f;
    private static final float CARD_AREA_HEIGHT = 230f;
    private static final float CARD_AREA_HEIGHT_TRADE = 215f;
    private Image borderTop, borderBottom, borderLeft, borderRight;
    private static Texture borderTex;


    public static TradeScene getInstance()
    {
        if(instance == null)
        {
            instance = new TradeScene();
        }
        return instance;
    }

    private TradeScene()
    {
        super(Forge.isLandscapeMode() ? "ui/items.json" : "ui/items_portrait.json");
        showTradeBackground();
        initTradeGui();
        redrawMode();
    }

    public static class CardButtonData
    {
        public PaperCard   card;
        public int         count;
        public RewardActor actor;
        public boolean     excluded = false;
        Image       dimOverlay;
    }

    private void redrawMode() {
        clearCardActors();
        clearCardButtons();

        switch (currentMode) {
            case SelectPlayerTradeCards:
                showSelectPlayerTradeCards();
                break;
            case SelectEnemyCardsWanted:
                showSelectEnemyCardsWanted();
                break;
            case ViewEnemyChoices:
                showViewEnemyChoices();
                break;
            case CreateTrade:
                showCreateTrade();
                break;
            case EnemyOffer:
                showEnemyOffer();
                break;
            case TradeComplete:
                showTradeComplete();
                break;
            case NoTradePossible:
                showNoTradePossible();
                break;
        }
    }

    private void showNoTradePossible()
    {
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null)
        {
            shopName.setText("");
            shopName.setVisible(false);
        }
        if(enemyCardsLabel != null)
        {
            enemyCardsLabel.setVisible(false);
        }
        if(playerCardsLabel != null)
        {
            playerCardsLabel.setVisible(false);
        }

        TypingLabel tradeCompleteLabel = new TypingLabel();
        tradeCompleteLabel.setText("No Trade Possible");
        tradeCompleteLabel.setBounds(140f, 135f, 120f, 20f);
        tradeCompleteLabel.setVisible(true);
        stage.addActor(tradeCompleteLabel);

        nextButton.setVisible(false);
        cardScrollPane.setVisible(false);
        borderTop.setVisible(false);
        borderBottom.setVisible(false);
        borderLeft.setVisible(false);
        borderRight.setVisible(false);

        clearCardActors();
        clearCardButtons();
    }

    private void showViewEnemyChoices()
    {
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null)
        {
            shopName.setText(TradeController.getInstance().getCurrentMob().getName() + " is interested in these cards");
            shopName.setVisible(true);
        }

        cardContentGroup.clearChildren();
        cardActors.clear();

        playerCardScrollPane.setVisible(false);
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

        float rowHeight = cardHeight + CARD_GAP;
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

                x_count++;
            }
        }
        cardScrollPane.layout();
        stage.setScrollFocus(cardScrollPane);
    }

    private void showTradeComplete()
    {
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null)
        {
            shopName.setText("");
            shopName.setVisible(false);
        }
        if(enemyCardsLabel != null)
        {
            enemyCardsLabel.setVisible(false);
        }
        if(playerCardsLabel != null)
        {
            playerCardsLabel.setVisible(false);
        }

        TypingLabel tradeCompleteLabel = new TypingLabel();
        tradeCompleteLabel.setText("TRADE COMPLETE!");
        tradeCompleteLabel.setBounds(150f, 135f, 100f, 20f);
        tradeCompleteLabel.setVisible(true);
        stage.addActor(tradeCompleteLabel);

        nextButton.setVisible(false);
        cardScrollPane.setVisible(false);
        borderTop.setVisible(false);
        borderBottom.setVisible(false);
        borderLeft.setVisible(false);
        borderRight.setVisible(false);

        clearCardActors();
        clearCardButtons();
    }

    private void showEnemyOffer()
    {
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null)
        {
            shopName.setText("Declined, here is a counter-offer");
            shopName.setVisible(true);
        }
        cardContentGroup.clearChildren();
        cardActors.clear();
        enemyCardButtons.clear();
        playerCardButtons.clear();

        declineButton.setVisible(true);
        nextButton.setText("Accept");

        playerCardScrollPane.setVisible(false);
        cardScrollPane.setVisible(true);
        cardScrollPane.setHeight(CARD_AREA_HEIGHT_TRADE);

        calculateCardRows();
        if (cardRows.size == 0 || enemyCardRows.size == 0)
        {
            return;
        }

        float cardWidth;
        float totalGap = CARD_GAP * 7;
        cardWidth = (MAX_ROW_WIDTH - totalGap) / 8;
        if (cardWidth > BASE_CARD_WIDTH)
        {
            cardWidth = BASE_CARD_WIDTH;
        }
        float cardHeight = cardWidth * CARD_ASPECT;
        float rowHeight = cardHeight + CARD_GAP;

        int numRows = Math.max(cardRows.size, enemyCardRows.size);
        float totalContentHeight = numRows * rowHeight + CARD_GAP;
        cardContentGroup.setSize(MAX_ROW_WIDTH, totalContentHeight);

        drawScrollPaneBorder(CARD_AREA_X, CARD_AREA_Y, CARD_AREA_WIDTH, CARD_AREA_HEIGHT_TRADE);

        for (int rowIndex = 0; rowIndex < cardRows.size; rowIndex++)
        {
            Array<PaperCard> row = cardRows.get(rowIndex);
            int n = row.size;
            if (n <= 0)
            {
                continue;
            }

            // Left-align
            float rowStartX = 0;

            // Row top from the top of the content region
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

                x_count++;
            }
        }

        for (int rowIndex = 0; rowIndex < enemyCardRows.size; rowIndex++)
        {
            Array<PaperCard> row = enemyCardRows.get(rowIndex);
            int n = row.size;
            if (n <= 0)
            {
                continue;
            }

            // Left-align
            float rowStartX = (cardWidth * 5) + (CARD_GAP * 5);

            // Row top from the top of the content region
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

                x_count++;
            }
        }
        cardScrollPane.layout();
        stage.setScrollFocus(cardScrollPane);
    }

    private void showCreateTrade()
    {
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null)
        {
            shopName.setText("Make a trade offer");
            shopName.setVisible(true);
        }
        enemyCardsLabel = new TypingLabel();
        enemyCardsLabel.setText("Their cards");
        enemyCardsLabel.setBounds(20f, shopName.getY() - enemyCardsLabel.getHeight() - 10f, 50f, enemyCardsLabel.getHeight());
        enemyCardsLabel.setVisible(true);;
        stage.addActor(enemyCardsLabel);
        playerCardsLabel = new TypingLabel();
        playerCardsLabel.setText("Your cards");
        playerCardsLabel.setBounds(250f, shopName.getY() - playerCardsLabel.getHeight() - 10f, 50f, playerCardsLabel.getHeight());
        playerCardsLabel.setVisible(true);;
        stage.addActor(playerCardsLabel);

        cardContentGroup.clearChildren();
        cardActors.clear();
        enemyCardButtons.clear();
        playerCardButtons.clear();

        declineButton.setVisible(false);
        nextButton.setText("Next");
        checkNextButton(playerCardButtons, enemyCardButtons);

        playerCardScrollPane.setVisible(false);
        cardScrollPane.setVisible(true);
        cardScrollPane.setHeight(CARD_AREA_HEIGHT_TRADE);

        calculateCardRows();
        if (cardRows.size == 0 || enemyCardRows.size == 0)
        {
            return;
        }

        TextraButton doneButton = ui.findActor("done");
        float buttonHeight = doneButton != null ? doneButton.getHeight() : 20f;

        float cardWidth;
        float totalGap = CARD_GAP * 7;
        cardWidth = (MAX_ROW_WIDTH - totalGap) / 8;
        if (cardWidth > BASE_CARD_WIDTH)
        {
            cardWidth = BASE_CARD_WIDTH;
        }
        float cardHeight = cardWidth * CARD_ASPECT;
        float rowHeight = cardHeight + buttonHeight + CARD_GAP;

        int numRows = Math.max(cardRows.size, enemyCardRows.size);
        float totalContentHeight = numRows * rowHeight + CARD_GAP;
        cardContentGroup.setSize(MAX_ROW_WIDTH, totalContentHeight);

        drawScrollPaneBorder(CARD_AREA_X, CARD_AREA_Y, CARD_AREA_WIDTH, CARD_AREA_HEIGHT_TRADE);

        for (int rowIndex = 0; rowIndex < cardRows.size; rowIndex++)
        {
            Array<PaperCard> row = cardRows.get(rowIndex);
            int n = row.size;
            if (n <= 0)
            {
                continue;
            }

            // Left-align
            float rowStartX = 0;

            // Row top from the top of the content region
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
                    public void clicked(InputEvent event, float xx, float yy)
                    {
                        toggleLeftCardButton(cardButton);
                        checkNextButton(playerCardButtons, enemyCardButtons);
                    }
                });

                cardButton.setText("");

                CardButtonData data = new CardButtonData();
                data.card     = card;
                data.count    = 1;
                data.actor    = actor;
                data.excluded = true;
                data.dimOverlay = null;
                cardButton.setUserObject(data);

                cardContentGroup.addActor(cardButton);
                playerCardButtons.add(cardButton);

                x_count++;
            }
        }

        for (int rowIndex = 0; rowIndex < enemyCardRows.size; rowIndex++)
        {
            Array<PaperCard> row = enemyCardRows.get(rowIndex);
            int n = row.size;
            if (n <= 0)
            {
                continue;
            }

            // Left-align
            float rowStartX = (cardWidth * 5) + (CARD_GAP * 5);

            // Row top from the top of the content region
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
                    public void clicked(InputEvent event, float xx, float yy)
                    {
                        toggleRightCardButton(cardButton);
                        checkNextButton(enemyCardButtons, enemyCardButtons);
                    }
                });

                cardButton.setText("");

                CardButtonData data = new CardButtonData();
                data.card     = card;
                data.count    = 1;
                data.actor    = actor;
                data.excluded = true;
                data.dimOverlay = null;
                cardButton.setUserObject(data);

                cardContentGroup.addActor(cardButton);
                enemyCardButtons.add(cardButton);

                x_count++;
            }
        }
        cardScrollPane.layout();
        stage.setScrollFocus(cardScrollPane);
    }

    private void checkNextButton(Array<TextraButton> playerCardButtons, Array<TextraButton> enemyCardButtons)
    {
        if(checkIfButtonsEmpty(playerCardButtons) || checkIfButtonsEmpty(enemyCardButtons))
        {
            nextButton.setDisabled(true);
            nextDisabled = true;
        }
        else
        {
            nextButton.setDisabled(false);
            nextDisabled = false;
        }
    }

    private void showSelectEnemyCardsWanted()
    {
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null)
        {
            shopName.setText("Choose which cards you want to trade for");
            shopName.setVisible(true);
        }
        cardContentGroup.clearChildren();
        cardActors.clear();
        enemyCardButtons.clear();

        playerCardScrollPane.setVisible(false);
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
        TextraButton doneButton = ui.findActor("done");
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

                Drawable overlayDrawable = doneButton.getStyle().up;
                Image dimOverlay = new Image(overlayDrawable);
                dimOverlay.setBounds(x, cardY, cardWidth, cardHeight);
                dimOverlay.setColor(0f, 0f, 0f, 0.4f);
                dimOverlay.setVisible(false);
                cardContentGroup.addActor(dimOverlay);
                dimOverlay.toFront();

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

                CardButtonData data = new CardButtonData();
                data.card     = card;
                data.count    = 1;
                data.actor    = actor;
                data.excluded = true;
                data.dimOverlay = dimOverlay;
                cardButton.setUserObject(data);

                cardContentGroup.addActor(cardButton);
                enemyCardButtons.add(cardButton);

                x_count++;
            }
        }
        cardScrollPane.layout();
        stage.setScrollFocus(cardScrollPane);
    }

    private void showSelectPlayerTradeCards()
    {
        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null)
        {
            shopName.setText("Choose cards you do not want to trade");
            shopName.setVisible(true);
        }

        playerCardContentGroup.clearChildren();
        playerCardActors.clear();
        playerCardButtons.clear();

        playerCardScrollPane.setVisible(true);
        cardScrollPane.setVisible(false);

        // 3) Rebuild the logical rows from the tradable pool
        calculatePlayerCardRows();
        if (playerCardRows.size == 0)
        {
            return;
        }

        // 4) Figure out cardWidth based on the *widest* row (max cards in any row)
        int maxCardsInAnyRow = 0;
        for (CardPool row : playerCardRows)
        {
            int rowSize = row.size();
            if (rowSize > maxCardsInAnyRow)
            {
                maxCardsInAnyRow = rowSize;
            }
        }
        if (maxCardsInAnyRow <= 0)
        {
            return;
        }

        // Base button height from the existing "done" button
        TextraButton doneButton = ui.findActor("done");
        float buttonHeight = doneButton != null ? doneButton.getHeight() : 20f;

        // Card width: same width for all rows, scaled so the widest row fits
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

        // Each row: [card][button] + vertical gap
        float rowHeight = cardHeight + buttonHeight + CARD_GAP;
        int numRows = playerCardRows.size;
        float totalContentHeight = numRows * rowHeight + CARD_GAP;

        // Let the scrollable content group know how tall it is
        playerCardContentGroup.setSize(MAX_ROW_WIDTH, totalContentHeight);

        drawScrollPaneBorder(CARD_AREA_X, CARD_AREA_Y, CARD_AREA_WIDTH, CARD_AREA_HEIGHT);

        // 5) Lay out each row and card
        for (int rowIndex = 0; rowIndex < playerCardRows.size; rowIndex++)
        {
            CardPool row = playerCardRows.get(rowIndex);
            int n = row.size();
            if (n <= 0)
            {
                continue;
            }

            // Center this row horizontally
            float rowWidth  = n * cardWidth + (n - 1) * CARD_GAP;
            float rowStartX = (MAX_ROW_WIDTH - rowWidth) / 2f;

            // Row top from the top of the content region
            float rowTopY = totalContentHeight - CARD_GAP - rowIndex * rowHeight;

            int x_count = 0;
            for (Map.Entry<PaperCard, Integer> entry : row)
            {
                PaperCard pc   = entry.getKey();
                Integer count  = entry.getValue();
                if (pc == null || count == null || count <= 0)
                {
                    continue;
                }

                Reward r = new Reward(pc, false);
                RewardActor actor = new RewardActor(r, false, RewardScene.Type.Shop, false);

                float x      = rowStartX + x_count * (cardWidth + CARD_GAP);
                float cardY  = rowTopY - cardHeight;
                actor.setBounds(x, cardY, cardWidth, cardHeight);
                playerCardContentGroup.addActor(actor);
                playerCardActors.add(actor);

                Drawable overlayDrawable = doneButton.getStyle().up;
                Image dimOverlay = new Image(overlayDrawable);
                dimOverlay.setBounds(x, cardY, cardWidth, cardHeight);
                dimOverlay.setColor(0f, 0f, 0f, 0.4f);
                dimOverlay.setVisible(false);
                playerCardContentGroup.addActor(dimOverlay);
                dimOverlay.toFront();

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

                cardButton.setText("" + count);

                CardButtonData data = new CardButtonData();
                data.card     = pc;
                data.count    = count;
                data.actor    = actor;
                data.excluded = false;
                data.dimOverlay = dimOverlay;
                cardButton.setUserObject(data);

                playerCardContentGroup.addActor(cardButton);
                playerCardButtons.add(cardButton);

                x_count++;
            }
        }

        playerCardScrollPane.layout();
        stage.setScrollFocus(playerCardScrollPane);
    }

    private void toggleCardButton(TextraButton button)
    {
        Object obj = button.getUserObject();
        if (!(obj instanceof CardButtonData))
        {
            return;
        }

        CardButtonData data = (CardButtonData) obj;

        data.excluded = !data.excluded;

        switch (currentMode)
        {
            case SelectPlayerTradeCards:
                if (data.excluded)
                {
                    button.setText("X");

                    if (data.dimOverlay != null)
                    {
                        data.dimOverlay.setVisible(true);
                    }
                }
                else
                {
                    button.setText(Integer.toString(data.count));

                    if (data.dimOverlay != null)
                    {
                        data.dimOverlay.setVisible(false);
                    }
                }
                break;
            case SelectEnemyCardsWanted:
                if (data.excluded)
                {
                    button.setText("");

                    if (data.dimOverlay != null)
                    {
                        data.dimOverlay.setVisible(true);
                    }
                }
                else
                {
                    button.setText("X");

                    if (data.dimOverlay != null)
                    {
                        data.dimOverlay.setVisible(false);
                    }
                }
                break;
        }
    }

    private void toggleLeftCardButton(TextraButton button)
    {
        Object obj = button.getUserObject();
        if (!(obj instanceof CardButtonData))
        {
            return;
        }

        CardButtonData data = (CardButtonData) obj;

        data.excluded = !data.excluded;

        if (data.excluded)
        {
            button.setText("");

            if (data.dimOverlay != null)
            {
                data.dimOverlay.setVisible(true);
            }
        }
        else
        {
            button.setText("\u2192");

            if (data.dimOverlay != null)
            {
                data.dimOverlay.setVisible(false);
            }
        }
    }

    private void toggleRightCardButton(TextraButton button)
    {
        Object obj = button.getUserObject();
        if (!(obj instanceof CardButtonData))
        {
            return;
        }

        CardButtonData data = (CardButtonData) obj;

        data.excluded = !data.excluded;

        if (data.excluded)
        {
            button.setText("");

            if (data.dimOverlay != null)
            {
                data.dimOverlay.setVisible(true);
            }
        }
        else
        {
            button.setText("\u2190");

            if (data.dimOverlay != null)
            {
                data.dimOverlay.setVisible(false);
            }
        }
    }

    private void clearCardActors() {
        for (Actor a : playerCardActors) {
            if (a != null) {
                a.remove();
            }
        }
        playerCardActors.clear();
    }

    private void clearCardButtons()
    {
        for (TextraButton btn : playerCardButtons) {
            btn.remove();
        }
        playerCardButtons.clear();
    }

    private void initTradeGui()
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
        if (doneButton != null) {
            doneButton.setText("Quit");
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
            instance = null;
            Actor tradeBg = ui.findActor("trade_background");
            tradeBg.setVisible(false);
            TradeController.getInstance().finishTrade(true);
        });
        createNextButton();
        createDeclineButton();

        ensureBorderTexture();

        playerCardContentGroup = new Group();
        playerCardScrollPane = new ScrollPane(playerCardContentGroup);
        playerCardScrollPane.setScrollingDisabled(true, false);
        playerCardScrollPane.setFadeScrollBars(false);
        playerCardScrollPane.setSmoothScrolling(true);
        playerCardScrollPane.setOverscroll(false, false);

        playerCardScrollPane.setBounds(
                CARD_AREA_X,
                CARD_AREA_Y,
                CARD_AREA_WIDTH,
                CARD_AREA_HEIGHT
        );
        stage.addActor(playerCardScrollPane);

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

        if(AdventurePlayer.current().getTradeBinder().countAll() > 0)
        {
            TradeController.getInstance().setApprovedPlayerTradeCards();
            TradeController.getInstance().setEnemyTradeCards();
            setMode(mode.SelectEnemyCardsWanted);
        }
        else
        {
            setMode(mode.SelectPlayerTradeCards);
        }
    }

    private void setMode(mode newMode)
    {
        System.out.println("Advancing from " + currentMode + " to " +newMode);
        currentMode = newMode;
        redrawMode();
    }

    private void showTradeBackground() {
        if (ui == null) {
            return;
        }

        Actor tradeBg = ui.findActor("trade_background");
        if (tradeBg != null) {
            tradeBg.setVisible(true);
        }
    }

    private void calculateCardRows()
    {
        Array<PaperCard> pool;
        Array<PaperCard> tempRow;
        int rowCounter;
        switch(currentMode)
        {
            case SelectEnemyCardsWanted:
                cardRows.clear();

                pool = TradeController.getInstance().getEnemyTradeCards();
                if (pool == null || pool.isEmpty())
                {
                    return;
                }
                tempRow = new Array<>();
                rowCounter = 0;
                for (PaperCard card : pool)
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
                    cardRows.add(tempRow); //last row wouldn't have been added in the loop
                }
                break;
            case ViewEnemyChoices:
                cardRows.clear();

                pool = TradeController.getInstance().getPlayerCardsWanted();
                if (pool == null || pool.isEmpty())
                {
                    return;
                }
                tempRow = new Array<>();
                rowCounter = 0;
                for (PaperCard card : pool)
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
                    cardRows.add(tempRow); //last row wouldn't have been added in the loop
                }
                break;
            case CreateTrade:
                cardRows.clear();
                enemyCardRows.clear();

                pool = TradeController.getInstance().getEnemyCardsWanted();
                if (pool == null || pool.isEmpty())
                {
                    return;
                }
                tempRow = new Array<>();
                rowCounter = 0;
                for (PaperCard card : pool)
                {
                    if (rowCounter == 3)
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
                    cardRows.add(tempRow); //last row wouldn't have been added in the loop
                }

                pool = TradeController.getInstance().getPlayerCardsWanted();
                if (pool == null || pool.isEmpty())
                {
                    return;
                }
                tempRow = new Array<>();
                rowCounter = 0;
                for (PaperCard card : pool)
                {
                    if (rowCounter == 3)
                    {
                        enemyCardRows.add(tempRow);
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
                    enemyCardRows.add(tempRow); //last row wouldn't have been added in the loop
                }
                break;
            case EnemyOffer:
                cardRows.clear();
                enemyCardRows.clear();

                pool = TradeController.getInstance().getCounterOffer_enemyCards();
                if (pool == null || pool.isEmpty())
                {
                    return;
                }
                tempRow = new Array<>();
                rowCounter = 0;
                for (PaperCard card : pool)
                {
                    if (rowCounter == 3)
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
                    cardRows.add(tempRow); //last row wouldn't have been added in the loop
                }

                pool = TradeController.getInstance().getCounterOffer_playerCards();
                if (pool == null || pool.isEmpty())
                {
                    return;
                }
                tempRow = new Array<>();
                rowCounter = 0;
                for (PaperCard card : pool)
                {
                    if (rowCounter == 3)
                    {
                        enemyCardRows.add(tempRow);
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
                    enemyCardRows.add(tempRow); //last row wouldn't have been added in the loop
                }
                break;
        }
    }

    private void calculatePlayerCardRows()
    {
        playerCardRows.clear();

        CardPool pool = TradeController.getInstance().getTradablePlayerCards();
        if (pool == null || pool.isEmpty())
        {
            return;
        }

        CardPool tempRow = new CardPool();
        int rowCounter = 0;
        for (Map.Entry<PaperCard, Integer> entry : pool)
        {
            if (rowCounter == MAX_CARDS_PER_ROW)
            {
                playerCardRows.add(tempRow);
                rowCounter = 0;
                tempRow = new CardPool();
            }
            PaperCard pc = entry.getKey();
            Integer count = entry.getValue();
            if (pc == null || count == null || count <= 0)
            {
                continue;
            }
            tempRow.add(pc, count);
            rowCounter++;
        }
        if (!tempRow.isEmpty())
        {
            playerCardRows.add(tempRow); //last row wouldn't have been added in the loop
        }
    }

    private void createNextButton()
    {
        if (doneButton == null)
        {
            return;
        }

        nextButton = new TextraButton("Next", doneButton.getStyle(), Controls.getTextraFont());

        nextButton.setSize(doneButton.getWidth(), doneButton.getHeight());

        float x = doneButton.getX();
        float y = doneButton.getY() - doneButton.getHeight() - NEXT_BUTTON_GAP;
        nextButton.setPosition(x, y);

        nextButton.addListener(new ClickListener()
        {
            @Override
            public void clicked(InputEvent event, float x, float y)
            {
                if(!nextDisabled)
                {
                    if (currentMode == mode.EnemyOffer)
                    {
                        playerAcceptedCounterOffer = true;
                    }
                    advanceMode();
                }
            }
        });

        ui.addActor(nextButton);
    }

    private void createDeclineButton()
    {
        if (doneButton == null)
        {
            return;
        }

        declineButton = new TextraButton("Decline", doneButton.getStyle(), Controls.getTextraFont());

        declineButton.setSize(doneButton.getWidth(), doneButton.getHeight());

        float x = doneButton.getX();
        float y = nextButton.getY() - nextButton.getHeight() - NEXT_BUTTON_GAP;
        declineButton.setPosition(x, y);

        declineButton.addListener(new ClickListener()
        {
            @Override
            public void clicked(InputEvent event, float x, float y)
            {
                playerAcceptedCounterOffer = false;
                advanceMode();
            }
        });
        declineButton.setVisible(false);

        ui.addActor(declineButton);
    }

    private void advanceMode()
    {
        switch(currentMode)
        {
            case SelectPlayerTradeCards:
                if(checkIfButtonsEmpty(playerCardButtons))
                {
                    setMode(mode.NoTradePossible);
                }
                else
                {
                    TradeController.getInstance().setApprovedPlayerTradeCards(playerCardButtons);
                    TradeController.getInstance().setEnemyTradeCards();
                    setMode(mode.SelectEnemyCardsWanted);
                }
                break;
            case SelectEnemyCardsWanted:
                if(checkIfButtonsEmpty(enemyCardButtons))
                {
                    setMode(mode.NoTradePossible);
                }
                else
                {
                    TradeController.getInstance().setEnemyCardsWanted(enemyCardButtons);
                    TradeController.getInstance().setPlayerCardsWanted();
                    setMode(mode.ViewEnemyChoices);
                }
                break;
            case ViewEnemyChoices:
                TradeController.getInstance().setEnemyCardPrices();
                setMode(mode.CreateTrade);
                break;
            case CreateTrade:
                if(TradeController.getInstance().playerOfferAccepted(playerCardButtons, enemyCardButtons))
                {
                    TradeController.getInstance().exchangeCardsFromButtons(playerCardButtons, enemyCardButtons);
                    setMode(mode.TradeComplete);
                }
                else
                {
                    TradeController.getInstance().createCounterOffer(playerCardButtons, enemyCardButtons);
                    if(TradeController.getInstance().tradeFound)
                    {
                        setMode(mode.EnemyOffer);
                    }
                    else
                    {
                        setMode(mode.NoTradePossible);
                    }
                }
                break;
            case EnemyOffer:
                if(playerAcceptedCounterOffer)
                {
                    TradeController.getInstance().exchangeCards(TradeController.getInstance().getCounterOffer_enemyCards(), TradeController.getInstance().getCounterOffer_playerCards());
                    setMode(mode.TradeComplete);
                }
                else
                {
                    setMode(mode.CreateTrade);
                }
                break;
            case TradeComplete:
                //next button should not be visible in this mode, so getting here indicates an error
                break;
            case NoTradePossible:
                //next button should not be visible in this mode, so getting here indicates an error
        }
    }

    private boolean checkIfButtonsEmpty(Array<TextraButton> cardButtons)
    {
        for(TextraButton btn : cardButtons)
        {
            if(btn == null)
            {
                continue;
            }
            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData))
            {
                continue;
            }
            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
            if (data.excluded)
            {
                continue;
            }
            return false;
        }
        return true;
    }

    private void ensureBorderTexture() {
        if (borderTex != null) return;

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0.8f, 0.8f, 0.8f, 0.5f);
        pm.fill();
        borderTex = new Texture(pm);
        pm.dispose();
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
}
