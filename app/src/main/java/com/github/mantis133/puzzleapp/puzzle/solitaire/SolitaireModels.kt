package com.github.mantis133.puzzleapp.puzzle.solitaire

enum class Suit(val symbol: String, val isRed: Boolean) {
    CLUBS("♣", false),
    DIAMONDS("♦", true),
    HEARTS("♥", true),
    SPADES("♠", false)
}

enum class Rank(val display: String, val value: Int) {
    ACE("A", 1),
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13)
}

data class Card(val suit: Suit, val rank: Rank)

/**
 * Represents one of the 7 tableau columns.
 * [cards] is ordered bottom-to-top (index 0 is the bottom of the column visually).
 * [faceUpCount] is the number of cards from the top of the list that are face-up.
 */
data class TableauColumn(
    val cards: List<Card>,
    val faceUpCount: Int
) {
    val faceDownCount: Int get() = cards.size - faceUpCount

    /** Returns only the face-up cards (top portion of the column). */
    val faceUpCards: List<Card>
        get() = if (faceUpCount == 0) emptyList() else cards.takeLast(faceUpCount)

    /** Returns only the face-down cards (bottom portion). */
    val faceDownCards: List<Card>
        get() = cards.dropLast(faceUpCount)
}

/**
 * The full game board state for Klondike Solitaire.
 *
 * @param tableau   7 columns (indices 0–6).
 * @param foundations 4 foundation piles indexed by [Suit.ordinal]:
 *                    0 = CLUBS, 1 = DIAMONDS, 2 = HEARTS, 3 = SPADES.
 *                    Each list is ordered Ace → King.
 * @param stock     Cards remaining in the draw pile (top of stack = last element).
 * @param waste     Cards in the discard/waste pile (top card = last element).
 */
data class SolitaireBoard(
    val tableau: List<TableauColumn>,
    val foundations: List<List<Card>>,
    val stock: List<Card>,
    val waste: List<Card>
)

/** Identifies a location from which (or to which) a card can be moved. */
sealed class CardLocation {
    data object Waste : CardLocation()
    data class Tableau(val column: Int, val cardIndex: Int) : CardLocation()
    data class Foundation(val suit: Suit) : CardLocation()
}
