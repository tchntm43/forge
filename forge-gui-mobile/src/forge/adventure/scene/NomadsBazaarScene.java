package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.item.PaperCard;
import forge.screens.FScreen;
import forge.screens.NomadsBazaarScreen;
import com.github.tommyettinger.textra.TextraLabel;
import forge.adventure.util.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.github.tommyettinger.textra.TextraButton;
import forge.model.FModel;
import forge.card.ICardFace;
import forge.adventure.util.Config; // same import you used in RandomMapEventController
import java.util.List;
import java.util.stream.Collectors;
import forge.adventure.util.Reward;
import forge.adventure.util.RewardActor;

/**
 * Custom Scene for processing Nomad's Bazaar random map event
 */

public class NomadsBazaarScene extends UIScene
{
    private static NomadsBazaarScene instance;
    private NomadsBazaarScreen screen;
    private TextraLabel playerGold;
    private TextraLabel playerShards;
    private TypingLabel instructionLabel;
    private TextField searchField;
    private TextraButton submitButton;
    private List<ICardFace> bazaarFaces;
    private ICardFace selectedFace;
    private PaperCard selectedPaperCard;
    private TypingLabel cardLabel;
    private TextraButton buyButton;
    private RewardActor cardActor;
    private final float nomadsbazaarPriceModifier = 1.33f;
    private Reward reward;
    private int price;

    public static NomadsBazaarScene instance()
    {
        if (instance == null)
        {
            instance = new NomadsBazaarScene();
        }
        return instance;
    }

    private NomadsBazaarScene()
    {
        super(Forge.isLandscapeMode() ? "ui/items.json" : "ui/items_portrait.json");

        playerGold = Controls.newAccountingLabel(ui.findActor("playerGold"), false);
        playerShards = Controls.newAccountingLabel(ui.findActor("playerShards"), true);
        showBazaarBackground();
        configureBazaarUi();
        createInstructionLabel();
        createSearchField();
        createSearchSubmitButton();
        createCardDisplayArea();
        buildBazaarCardPool();
    }

    public FScreen getScreen()
    {
        //obsolete, not used anywhere
        System.out.println("getScreen still being used");
        if (screen == null)
        {
            screen = new NomadsBazaarScreen();
        }
        return screen;
    }

    public void showEvent(String title, String text)
    {
        //obsolete, not used
        if (screen == null)
        {
            screen = new NomadsBazaarScreen();
        }
        screen.setEvent(title, text);
    }

    private void showBazaarBackground() {
        if (ui == null) {
            return; // ForgeScene should normally have initialized this
        }

        // Turn on the Nomad's Bazaar background.
        Actor bazaarBg = ui.findActor("nomadsbazaar_background");
        if (bazaarBg != null) {
            bazaarBg.setVisible(true);
        }
    }

    private void configureBazaarUi() {
        // Hide shop-only buttons
        Actor restockButton = ui.findActor("restock");
        Actor detailButton  = ui.findActor("detail");

        if (restockButton != null) {
            restockButton.setVisible(false);
        }
        if (detailButton != null) {
            detailButton.setVisible(false);
        }

        TypingLabel shopName = ui.findActor("shopName");
        if (shopName != null) {
            shopName.setVisible(true);
            shopName.setText("Nomad's Bazaar");
        }

        ui.onButtonPress("done", () ->
        {
            instance = null;
            Actor bazaarBg = ui.findActor("nomadsbazaar_background");
            bazaarBg.setVisible(false);
            Forge.switchToLast();
        });
    }

    public void reset()
    {
        selectedFace = null;
        buyButton.setVisible(false);
        searchField.setMessageText("Search card name...");
        updateCardDisplay();
    }

    private void createInstructionLabel() {
        // Create a label with the same style as adventure dialogs
        instructionLabel = Controls.newTypingLabel("Type any card to buy!");
        instructionLabel.skipToTheEnd();          // no typing animation every time
        instructionLabel.setWrap(true);
        instructionLabel.setAlignment(Align.center);

        // Reasonable width so wrapping works nicely
        float labelWidth = 320f;
        instructionLabel.setWidth(labelWidth);

        // Position: centered horizontally, just under the scene header area
        // UIScene's `ui` is the root actor for this layout; use its size.
        float uiWidth = ui.getWidth();
        float uiHeight = ui.getHeight();

        if (uiWidth == 0f || uiHeight == 0f) {
            // Fallback if sizes aren’t initialized yet; hardcode something that
            // matches the items.json coordinate space (same as RewardScene).
            uiWidth = 480f;
            uiHeight = 270f;
        }

        float x = (uiWidth - labelWidth) / 2f;
        // Slightly below top — tweak this number if it feels too high/low
        float y = uiHeight - 35f;

        instructionLabel.setPosition(x, y);

        ui.addActor(instructionLabel);
    }

    private void createSearchField() {
        Skin skin = Controls.getSkin();

        searchField = new TextField("", skin);
        searchField.setMessageText("Search card name...");

        float fieldWidth = 320f;
        searchField.setWidth(fieldWidth);

        // Position: centered horizontally, just below the instruction label
        float uiWidth = ui.getWidth();
        float uiHeight = ui.getHeight();
        if (uiWidth == 0f || uiHeight == 0f) {
            // Same fallback coord space as before
            uiWidth = 480f;
            uiHeight = 270f;
        }

        float x = (uiWidth - fieldWidth) / 2f;
        float y = instructionLabel.getY() - 40f;   // just under "Type any card to buy!"

        searchField.setPosition(x, y);

        ui.addActor(searchField);
    }

    private void createSearchSubmitButton() {
        // Create a text button using the same skin/theme as everything else
        submitButton = Controls.newTextButton("Submit", new Runnable() {
            @Override
            public void run() {
                onSearchSubmitted();
            }
        });

        float padding = 10f;
        float btnWidth = 60f;

        // Try to match the search field height if it has one
        float btnHeight = searchField.getHeight() > 0 ? searchField.getHeight() : 24f;

        float x = searchField.getX() + searchField.getWidth() + padding;
        float y = searchField.getY();

        submitButton.setSize(btnWidth, btnHeight);
        submitButton.setPosition(x, y);

        ui.addActor(submitButton);
    }

    private void onSearchSubmitted() {
        String query = searchField.getText();
        if (query == null) {
            query = "";
        }
        query = query.trim();

        if (query.isEmpty()) {
            System.out.println("Nomad's Bazaar search: empty query");
            selectedFace = null;
            updateCardDisplay();
            return;
        }

        if (bazaarFaces == null || bazaarFaces.isEmpty()) {
            System.out.println("Nomad's Bazaar search: no faces in pool");
            selectedFace = null;
            updateCardDisplay();
            return;
        }

        String qLower = query.toLowerCase();

        List<ICardFace> matches = bazaarFaces.stream()
                .filter(face -> face.getName().toLowerCase().startsWith(qLower))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            System.out.println("Nomad's Bazaar: no card found for '" + query + "'");
            selectedFace = null;
            selectedPaperCard = null;
        } else {
            selectedFace = matches.get(0);
            System.out.println("Nomad's Bazaar: found '" + selectedFace.getName() + "' for query '" + query + "'");
            selectedPaperCard = FModel.getMagicDb()
                    .getCommonCards()
                    .getCard(selectedFace.getName());
        }

        updateCardDisplay();
    }

    private void buildBazaarCardPool() {
        bazaarFaces = FModel.getMagicDb().getCommonCards().streamAllFaces()
                .filter(face -> !isRestricted(face.getName()))
                .collect(Collectors.toList());
    }

    private boolean isRestricted(String cardName) {
        var configData = Config.instance().getConfigData();
        String[] restricted = configData.restrictedCards;
        if (restricted == null) {
            return false;
        }
        for (String r : restricted) {
            if (cardName.equalsIgnoreCase(r)) {
                return true;
            }
        }
        return false;
    }

    private void createCardDisplayArea() {
        float uiWidth = ui.getWidth();
        float uiHeight = ui.getHeight();
        if (uiWidth == 0f || uiHeight == 0f) {
            uiWidth = 480f;
            uiHeight = 270f;
        }

        // Label that shows which card was found.
        cardLabel = Controls.newTypingLabel("");
        cardLabel.setWrap(true);
        cardLabel.setText("");

        float labelWidth = 320f;
        float x = (uiWidth - labelWidth) / 2f;
        float y = searchField.getY() - 60f; // a bit below the search field

        cardLabel.setWidth(labelWidth);
        cardLabel.setPosition(x, y);
        ui.addActor(cardLabel);

        // "Buy" button under the label
        buyButton = Controls.newTextButton("Buy", new Runnable() {
            @Override
            public void run() {
                onBuyClicked();
            }
        });
        buyButton.setVisible(false);

        float btnWidth = 50f;
        float btnHeight = 39f;
        float btnX = (uiWidth - btnWidth) / 2f;
        float btnY = y - 40f;

        buyButton.setSize(btnWidth, btnHeight);
        buyButton.setPosition(btnX, btnY);
        ui.addActor(buyButton);
    }

    private void updateCardDisplay()
    {
        // --- Card layout ---
        float cardWidth = 120f; // a bit larger so the art looks nicer
        float aspect = 680f / 488f; // same ratio RewardActor uses
        float cardHeight = cardWidth * aspect;

        if (cardLabel == null || buyButton == null) {
            return;
        }

        // --- Label layout (below the searchField) ---
        cardLabel.setText("");
        cardLabel.setVisible(true);
        cardLabel.skipToTheEnd();

        float labelWidth = cardWidth + 40f;
        cardLabel.setWidth(labelWidth);

        float labelX = searchField.getX() + 20f;              //approx. centered with searchField
        float labelY = searchField.getY() - 15f;               // just below searchField
        cardLabel.setPosition(labelX, labelY);

        // If no card is selected, clear card image and show "not found" text.
        if (selectedPaperCard == null) {
            // Remove any previous card actor from the stage
            if (cardActor != null) {
                cardActor.remove();
                cardActor = null;
            }

            cardLabel.setText("Card not found, or not available here.");
            cardLabel.setVisible(true);
            cardLabel.skipToTheEnd();

            buyButton.setVisible(false);
            return;
        }

        // We have a valid card – recreate the RewardActor each time
        if (cardActor != null) {
            cardActor.remove();
            cardActor = null;
        }

        reward = new Reward(selectedPaperCard, false);
        cardActor = new RewardActor(reward, false, RewardScene.Type.Shop, false);
        stage.addActor(cardActor);

        // Card Positioning
        float uiWidth  = ui.getWidth()  == 0f ? 480f : ui.getWidth();
        float uiHeight = ui.getHeight() == 0f ? 270f : ui.getHeight();

        float cardX = (uiWidth - cardWidth) / 2f;

        // Place the card somewhere under the search field
        float cardY = searchField.getY() - (15f + cardHeight);

        cardActor.setBounds(cardX, cardY, cardWidth, cardHeight);

        // --- Buy button layout (below the card) ---
        price = (int) (CardUtil.getRewardPrice(cardActor.getReward()) * nomadsbazaarPriceModifier);
        buyButton.setDisabled(false);
        if (AdventurePlayer.current().getGold() < price) {
            buyButton.setDisabled(true);
        }
        int ownedCount = AdventurePlayer.current()
                .getCollectionCards(true)
                .count(selectedPaperCard);
        String ownedLabel = Forge.getLocalizer().getMessage("lblOwned");
        String buttonText = "[%75][+GoldCoin] " + price
                + "\n" + ownedLabel + ": " + ownedCount;
        buyButton.setText(buttonText);
        buyButton.setVisible(true);

        // Ensure the button has a reasonable size even before first layout
        float btnWidth  = buyButton.getWidth();
        float btnHeight = buyButton.getHeight();
        if (btnWidth <= 0f) {
            btnWidth = 100f;
            buyButton.setWidth(btnWidth);
        }
        if (btnHeight <= 0f) {
            btnHeight = 26f;
            buyButton.setHeight(btnHeight);
        }

        float btnX = cardX + cardWidth + 4f;
        float btnY = searchField.getY() - (15f + buyButton.getHeight());

        buyButton.setPosition(btnX, btnY);
        buyButton.toFront(); // make sure it draws above the card
    }

    private void onBuyClicked() {
        if (selectedFace == null) {
            System.out.println("Nomad's Bazaar: Buy clicked with no selected card.");
            return;
        }

        AdventurePlayer player = AdventurePlayer.current();

        if (player.getGold() < price) {
            buyButton.setDisabled(true);
        }

        player.takeGold(price);
        player.addReward(reward);

        System.out.println("Nomad's Bazaar: bought '" + selectedPaperCard.getName()
                + "' for " + price + " gold.");

        updateCardDisplay();
    }
}
