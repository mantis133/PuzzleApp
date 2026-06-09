package com.github.mantis133.puzzleapp.puzzle.solitaire

object SolitaireEngine {

    private val fullDeck: List<Card> = buildList {
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                add(Card(suit, rank))
            }
        }
    }

    // ── Deal ─────────────────────────────────────────────────────────────────

    /**
     * Deals a shuffled Klondike board.
     * Column i contains (i+1) cards; only the top card is face-up.
     * Remaining 24 cards go into the stock.
     */
    fun deal(): SolitaireBoard {
        val deck = fullDeck.shuffled().toMutableList()

        val tableau = (0 until 7).map { colIndex ->
            val count = colIndex + 1
            val cards = deck.removeRange(0, count)
            TableauColumn(cards = cards, faceUpCount = 1)
        }

        return SolitaireBoard(
            tableau     = tableau,
            foundations = List(4) { emptyList() },
            stock       = deck, // remaining 24 cards
            waste       = emptyList()
        )
    }

    // ── Stock / Waste operations ──────────────────────────────────────────────

    /** Moves the top card of stock to the top of waste. */
    fun flipStock(board: SolitaireBoard): SolitaireBoard {
        if (board.stock.isEmpty()) return board
        val flipped = board.stock.last()
        return board.copy(
            stock = board.stock.dropLast(1),
            waste = board.waste + flipped
        )
    }

    /**
     * Recycles the waste pile back into the stock (reversed, face-down).
     * Called when stock is empty and waste is non-empty.
     */
    fun recycleWaste(board: SolitaireBoard): SolitaireBoard {
        if (board.stock.isNotEmpty() || board.waste.isEmpty()) return board
        return board.copy(
            stock = board.waste.reversed(),
            waste = emptyList()
        )
    }

    // ── Move validation ───────────────────────────────────────────────────────

    /**
     * Returns the card(s) that would move given a source location.
     * - Waste → top waste card (1 card).
     * - Foundation → top foundation card (1 card).
     * - Tableau → all face-up cards from [cardIndex] to end of column.
     */
    fun getMovableCards(board: SolitaireBoard, from: CardLocation): List<Card> {
        return when (from) {
            is CardLocation.Waste -> {
                val top = board.waste.lastOrNull() ?: return emptyList()
                listOf(top)
            }
            is CardLocation.Foundation -> {
                val top = board.foundations[from.suit.ordinal].lastOrNull() ?: return emptyList()
                listOf(top)
            }
            is CardLocation.Tableau -> {
                val col = board.tableau[from.column]
                val faceUpStart = col.faceDownCount // index into col.cards where face-up begins
                if (from.cardIndex < faceUpStart) return emptyList() // face-down card
                col.cards.subList(from.cardIndex, col.cards.size)
            }
        }
    }

    /**
     * Returns true if [cards] (a stack of 1+ cards) can be placed onto [target].
     * Rules:
     * - The target's top card must be the opposite color and one rank higher than [cards.first()].
     * - An empty column accepts a King as the first card.
     */
    fun canMoveToTableau(cards: List<Card>, target: TableauColumn): Boolean {
        if (cards.isEmpty()) return false
        val moving = cards.first()
        return if (target.cards.isEmpty()) {
            moving.rank == Rank.KING
        } else {
            val top = target.cards.last()
            top.suit.isRed != moving.suit.isRed && top.rank.value == moving.rank.value + 1
        }
    }

    /**
     * Returns true if [card] can be moved to [foundation].
     * - Empty foundation accepts an Ace only.
     * - Otherwise the card must be the same suit and exactly one rank higher than the current top.
     */
    fun canMoveToFoundation(card: Card, foundation: List<Card>): Boolean {
        return if (foundation.isEmpty()) {
            card.rank == Rank.ACE
        } else {
            val top = foundation.last()
            top.suit == card.suit && top.rank.value + 1 == card.rank.value
        }
    }

    // ── Apply move ────────────────────────────────────────────────────────────

    /**
     * Applies a move from [from] to [to] and returns the updated board.
     * The caller should have verified the move is legal.
     * Newly exposed tableau cards are automatically flipped face-up.
     */
    fun applyMove(board: SolitaireBoard, from: CardLocation, to: CardLocation): SolitaireBoard {
        val movingCards = getMovableCards(board, from)
        if (movingCards.isEmpty()) return board

        // Remove cards from source
        val boardAfterRemove = removeCards(board, from, movingCards.size)

        // Add cards to destination
        return addCards(boardAfterRemove, to, movingCards)
    }

    private fun removeCards(board: SolitaireBoard, from: CardLocation, count: Int): SolitaireBoard {
        return when (from) {
            is CardLocation.Waste -> board.copy(waste = board.waste.dropLast(count))

            is CardLocation.Foundation -> {
                val newFoundations = board.foundations.toMutableList()
                newFoundations[from.suit.ordinal] = newFoundations[from.suit.ordinal].dropLast(count)
                board.copy(foundations = newFoundations)
            }

            is CardLocation.Tableau -> {
                val newTableau = board.tableau.toMutableList()
                val col = newTableau[from.column]
                val newCards = col.cards.dropLast(count)
                val newFaceUp = when {
                    newCards.isEmpty() -> 0
                    col.faceUpCount - count <= 0 -> 1 // flip the newly exposed card
                    else -> col.faceUpCount - count
                }
                newTableau[from.column] = col.copy(cards = newCards, faceUpCount = newFaceUp)
                board.copy(tableau = newTableau)
            }
        }
    }

    private fun addCards(board: SolitaireBoard, to: CardLocation, cards: List<Card>): SolitaireBoard {
        return when (to) {
            is CardLocation.Foundation -> {
                val suitIndex = cards.first().suit.ordinal
                val newFoundations = board.foundations.toMutableList()
                newFoundations[suitIndex] = newFoundations[suitIndex] + cards
                board.copy(foundations = newFoundations)
            }

            is CardLocation.Tableau -> {
                val newTableau = board.tableau.toMutableList()
                val col = newTableau[to.column]
                newTableau[to.column] = col.copy(
                    cards       = col.cards + cards,
                    faceUpCount = col.faceUpCount + cards.size
                )
                board.copy(tableau = newTableau)
            }

            // You can't move cards to Waste
            is CardLocation.Waste -> board
        }
    }

    // ── Win detection ─────────────────────────────────────────────────────────

    /** Returns true when all 4 foundations are complete (Ace through King = 13 cards each). */
    fun isSolved(board: SolitaireBoard): Boolean {
        return board.foundations.all { it.size == 13 }
    }

    // ── Auto-complete ─────────────────────────────────────────────────────────

    /**
     * Returns true when the game is guaranteed winnable without any more decisions:
     * the stock is empty and every tableau card is face-up.
     * At that point every remaining move can be automated.
     */
    fun isAutoCompletable(board: SolitaireBoard): Boolean {
        if (board.stock.isNotEmpty()) return false
        return board.tableau.all { it.faceDownCount == 0 }
    }

    /**
     * Finds the next card that can be moved to a foundation.
     * Checks the waste top card first, then each tableau column's top card.
     * Returns a [from → to] pair, or null if no foundation move is available.
     */
    fun findAutoCompleteMove(board: SolitaireBoard): Pair<CardLocation, CardLocation>? {
        // Check waste
        val wasteTop = board.waste.lastOrNull()
        if (wasteTop != null) {
            val foundation = board.foundations[wasteTop.suit.ordinal]
            if (canMoveToFoundation(wasteTop, foundation)) {
                return CardLocation.Waste to CardLocation.Foundation(wasteTop.suit)
            }
        }
        // Check each tableau column
        for (colIndex in 0 until 7) {
            val col = board.tableau[colIndex]
            val top = col.cards.lastOrNull() ?: continue
            val foundation = board.foundations[top.suit.ordinal]
            if (canMoveToFoundation(top, foundation)) {
                val cardIdx = col.cards.lastIndex
                return CardLocation.Tableau(colIndex, cardIdx) to CardLocation.Foundation(top.suit)
            }
        }
        return null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Extension to remove a range from a MutableList and return it as a new list. */
    private fun MutableList<Card>.removeRange(fromIndex: Int, toIndex: Int): List<Card> {
        val removed = subList(fromIndex, toIndex).toList()
        subList(fromIndex, toIndex).clear()
        return removed
    }
}
