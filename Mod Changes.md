# Justin's Mod Changes (as of August 6, 2026)

## Adventure Settings

- Added Adventure settings to control which of my features you want to have in your game. Not all features are controllable this way.

## AI Concession

- Added AI concession when combat damage appears overwhelmingly lethal.
- The purpose of this addition is to help avoid the game slowing down when too many creatures are on the battlefield.
- This can be toggled by a setting.

## Adventure World

- Added random map events, which can include opportunities for random cards, gold, buying cards, duplicating cards, and at least one minor negative event. Random events can be toggled by a setting.
- Added enemy trading behavior and trade binder support. This can be toggled by a setting.
- Added enemy fear behavior based on player win history. Enemies that are afraid of the player won't get too close, but can be chased down. This can be toggled by a setting.
- Added rare restricted-card chest rewards. This can be toggled by a setting.
- Added Liliana rewards, including +1 life and Liliana's Chain Veil item.
- Made some adjustments to the card rarities in chests and in shops
- Disabled most digital and other non-paper-legal cards from appearing in shops, rewards, or genetic AI decks. Some still slip through. Can be toggled by a setting.

## Card And Script Fixes

- As Foretold fix: This card now restricts the choice to cards owned by the caster.
- Adjusted Underworld Breach AI usage. AI will not cast Underworld Breach on any empty graveyard anymore.
- AI plays better around your Counterbalance, remembering the top card until the top card changes.
- Removed redundant confirmation prompts for some activated costs.
- Fixed Tibalt boss effect so the 11-15 result only deals random creature damage.
- Improved AI blocking behavior for Hornet Nest-style cards. It will always try to block with Hornet Nest, and prefers to block the creature with the greatest power.

## Item Changes

- Added Liliana's Chain Veil as a Liliana boss item.
- Liliana's Chain Veil item starts battles with The Chain Veil on the battlefield.

## Future Goals
- Randomize biome distribution on the map (likely a large, challenging project)
- Add completely new game mode, the Game Store Nostalgia Mode, where it's 1994 and you buy cards, play games and tournaments, at your local gaming store. Simulates passage of time as new sets are released, formats rotate, decks change, etc. Likely a massive project and I honestly don't know if I'll get to this.