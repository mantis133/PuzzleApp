package com.github.mantis133.puzzleapp.ui.screens.solitaire

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mantis133.puzzleapp.puzzle.solitaire.Card
import com.github.mantis133.puzzleapp.puzzle.solitaire.CardLocation
import com.github.mantis133.puzzleapp.puzzle.solitaire.Suit
import com.github.mantis133.puzzleapp.puzzle.solitaire.TableauColumn

private val CARD_SHAPE = RoundedCornerShape(6.dp)
private val EMPTY_SLOT_COLOR  = Color(0x33FFFFFF)  // subtle white tint on the green felt
private val FACE_DOWN_COLOR   = Color(0xFF1565C0)   // dark blue card back
private val CARD_FACE_COLOR   = Color(0xFFFFFDE7)   // warm white face
private val CARD_RED_COLOR    = Color(0xFFD32F2F)
private val CARD_BLACK_COLOR  = Color(0xFF212121)

// Height of a face-down card peek strip
private val FACE_DOWN_STRIP = 16.dp
// Height of a face-up card peek strip (all but the bottom-most face-up card)
private val FACE_UP_STRIP   = 26.dp

@Composable
fun SolitaireBoardView(
    state: SolitaireUiState,
    onStockTap: () -> Unit,
    onLocationTap: (CardLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val board = state.board ?: return
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        SolitaireBoardLandscape(board, state, onStockTap, onLocationTap, modifier)
    } else {
        SolitaireBoardPortrait(board, state, onStockTap, onLocationTap, modifier)
    }
}

// ── Portrait layout ────────────────────────────────────────────────────────────

@Composable
private fun SolitaireBoardPortrait(
    board: com.github.mantis133.puzzleapp.puzzle.solitaire.SolitaireBoard,
    state: SolitaireUiState,
    onStockTap: () -> Unit,
    onLocationTap: (CardLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val totalWidth = maxWidth
        val gap        = 2.dp
        val sidePad    = 4.dp
        val cardWidth  = (totalWidth - sidePad * 2 - gap * 6) / 7
        val cardHeight = cardWidth * 1.45f

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // ── Top row: Stock | Waste | gap | 4 Foundations ───────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = sidePad),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                StockSlot(board, cardWidth, cardHeight, onStockTap)
                WasteSlot(board, state, cardWidth, cardHeight, onLocationTap)
                Spacer(Modifier.weight(1f))
                FoundationSlots(board, state, cardWidth, cardHeight, onLocationTap)
            }

            // ── Tableau ─────────────────────────────────────────────────────
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = sidePad),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment     = Alignment.Top
                ) {
                    (0 until 7).forEach { colIndex ->
                        TableauColumnView(
                            column           = board.tableau[colIndex],
                            colIndex         = colIndex,
                            cardWidth        = cardWidth,
                            cardHeight       = cardHeight,
                            faceDownStrip    = FACE_DOWN_STRIP,
                            faceUpStrip      = FACE_UP_STRIP,
                            selectedLocation = state.selectedLocation,
                            selectedCards    = state.selectedCards,
                            onCardTap        = { idx -> onLocationTap(CardLocation.Tableau(colIndex, idx)) },
                            onEmptyTap       = { onLocationTap(CardLocation.Tableau(colIndex, 0)) },
                            modifier         = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ── Landscape layout ───────────────────────────────────────────────────────────

@Composable
private fun SolitaireBoardLandscape(
    board: com.github.mantis133.puzzleapp.puzzle.solitaire.SolitaireBoard,
    state: SolitaireUiState,
    onStockTap: () -> Unit,
    onLocationTap: (CardLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val totalWidth  = maxWidth
        val totalHeight = maxHeight
        val gap         = 2.dp

        // Left sidebar: stock + waste + 4 foundations stacked vertically
        // Card width in sidebar = totalHeight / 7 items in column (rough guide), capped
        val sidebarCardWidth = minOf((totalHeight - gap * 7) / 7, 80.dp)
        val sidebarCardH     = sidebarCardWidth * 1.45f
        val sidebarWidth     = sidebarCardWidth + 8.dp

        // Tableau fills remaining width: 7 columns
        val tableauWidth = totalWidth - sidebarWidth - gap
        val tableauCardW = (tableauWidth - gap * 6) / 7
        val tableauCardH = tableauCardW * 1.45f
        // Tighter strips in landscape to save vertical space
        val faceDownStrip = 10.dp
        val faceUpStrip   = 18.dp

        Row(
            modifier              = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            // ── Left sidebar ─────────────────────────────────────────────
            Column(
                modifier              = Modifier.width(sidebarWidth).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement   = Arrangement.spacedBy(gap),
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                StockSlot(board, sidebarCardWidth, sidebarCardH, onStockTap)
                WasteSlot(board, state, sidebarCardWidth, sidebarCardH, onLocationTap)

                Spacer(Modifier.height(4.dp))

                // 4 Foundations stacked vertically
                Suit.entries.forEach { suit ->
                    val foundation = board.foundations[suit.ordinal]
                    val topCard    = foundation.lastOrNull()
                    val isSelected = state.selectedLocation is CardLocation.Foundation &&
                            (state.selectedLocation as CardLocation.Foundation).suit == suit
                    PileSlot(
                        modifier  = Modifier.size(sidebarCardWidth, sidebarCardH),
                        onClick   = { onLocationTap(CardLocation.Foundation(suit)) },
                        highlight = isSelected
                    ) {
                        if (topCard != null) {
                            CardFace(topCard, isSelected, Modifier.fillMaxSize())
                        } else {
                            EmptySlotLabel(suit.symbol, sidebarCardH,
                                color = if (suit.isRed) CARD_RED_COLOR else CARD_BLACK_COLOR)
                        }
                    }
                }
            }

            // ── Tableau ──────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment     = Alignment.Top
                ) {
                    (0 until 7).forEach { colIndex ->
                        TableauColumnView(
                            column           = board.tableau[colIndex],
                            colIndex         = colIndex,
                            cardWidth        = tableauCardW,
                            cardHeight       = tableauCardH,
                            faceDownStrip    = faceDownStrip,
                            faceUpStrip      = faceUpStrip,
                            selectedLocation = state.selectedLocation,
                            selectedCards    = state.selectedCards,
                            onCardTap        = { idx -> onLocationTap(CardLocation.Tableau(colIndex, idx)) },
                            onEmptyTap       = { onLocationTap(CardLocation.Tableau(colIndex, 0)) },
                            modifier         = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ── Shared slot composables ────────────────────────────────────────────────────

@Composable
private fun StockSlot(
    board: com.github.mantis133.puzzleapp.puzzle.solitaire.SolitaireBoard,
    cardWidth: Dp,
    cardHeight: Dp,
    onStockTap: () -> Unit
) {
    PileSlot(modifier = Modifier.size(cardWidth, cardHeight), onClick = onStockTap) {
        if (board.stock.isNotEmpty()) CardBack(Modifier.fillMaxSize())
        else EmptySlotLabel("↺", cardHeight)
    }
}

@Composable
private fun WasteSlot(
    board: com.github.mantis133.puzzleapp.puzzle.solitaire.SolitaireBoard,
    state: SolitaireUiState,
    cardWidth: Dp,
    cardHeight: Dp,
    onLocationTap: (CardLocation) -> Unit
) {
    PileSlot(
        modifier  = Modifier.size(cardWidth, cardHeight),
        onClick   = { if (board.waste.isNotEmpty()) onLocationTap(CardLocation.Waste) },
        highlight = state.selectedLocation is CardLocation.Waste
    ) {
        val top = board.waste.lastOrNull()
        if (top != null) CardFace(top, state.selectedLocation is CardLocation.Waste, Modifier.fillMaxSize())
        else EmptySlotLabel("", cardHeight)
    }
}

@Composable
private fun FoundationSlots(
    board: com.github.mantis133.puzzleapp.puzzle.solitaire.SolitaireBoard,
    state: SolitaireUiState,
    cardWidth: Dp,
    cardHeight: Dp,
    onLocationTap: (CardLocation) -> Unit
) {
    Suit.entries.forEach { suit ->
        val foundation = board.foundations[suit.ordinal]
        val topCard    = foundation.lastOrNull()
        val isSelected = state.selectedLocation is CardLocation.Foundation &&
                (state.selectedLocation as CardLocation.Foundation).suit == suit
        PileSlot(
            modifier  = Modifier.size(cardWidth, cardHeight),
            onClick   = { onLocationTap(CardLocation.Foundation(suit)) },
            highlight = isSelected
        ) {
            if (topCard != null) CardFace(topCard, isSelected, Modifier.fillMaxSize())
            else EmptySlotLabel(suit.symbol, cardHeight,
                color = if (suit.isRed) CARD_RED_COLOR else CARD_BLACK_COLOR)
        }
    }
}

@Composable
private fun TableauColumnView(
    column: TableauColumn,
    colIndex: Int,
    cardWidth: Dp,
    cardHeight: Dp,
    faceDownStrip: Dp,
    faceUpStrip: Dp,
    selectedLocation: CardLocation?,
    selectedCards: List<Card>,
    onCardTap: (Int) -> Unit,
    onEmptyTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val faceDownCount = column.faceDownCount
    val faceUpCount   = column.faceUpCount
    val totalCards    = column.cards.size

    Box(modifier = modifier) {
        if (totalCards == 0) {
            // Empty column slot — drop target
            PileSlot(
                modifier  = Modifier.size(cardWidth, cardHeight),
                onClick   = onEmptyTap,
                highlight = false
            ) {
                EmptySlotLabel("K", cardHeight)
            }
            return@Box
        }

        // Calculate total height needed
        // face-down cards each take faceDownStrip, except they are stacked
        // face-up cards each take faceUpStrip, except the last which is full cardHeight
        val stackHeight = run {
            var h = 0.dp
            h += faceDownStrip * faceDownCount
            if (faceUpCount > 1) h += faceUpStrip * (faceUpCount - 1)
            h += cardHeight // last face-up card is fully visible
            h
        }

        Column(modifier = Modifier.height(stackHeight)) {
            // Face-down cards
            for (i in 0 until faceDownCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (i < faceDownCount - 1 || faceUpCount > 0) faceDownStrip else cardHeight)
                        .clip(CARD_SHAPE)
                        .background(FACE_DOWN_COLOR)
                )
            }

            // Face-up cards
            for (localIdx in 0 until faceUpCount) {
                val cardIdx  = faceDownCount + localIdx
                val card     = column.cards[cardIdx]
                val isLast   = localIdx == faceUpCount - 1
                val cardH    = if (isLast) cardHeight else faceUpStrip

                val isSelected = selectedLocation is CardLocation.Tableau &&
                        selectedLocation.column == colIndex &&
                        selectedLocation.cardIndex == cardIdx

                // Also highlight if this card is part of a selected stack
                val isInSelectedStack = run {
                    if (selectedLocation is CardLocation.Tableau && selectedLocation.column == colIndex) {
                        cardIdx >= selectedLocation.cardIndex
                    } else false
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardH)
                        .clip(CARD_SHAPE)
                        .background(CARD_FACE_COLOR)
                        .then(
                            if (isSelected || isInSelectedStack)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CARD_SHAPE)
                            else Modifier
                        )
                        .clickable { onCardTap(cardIdx) }
                        .padding(3.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    val textColor = if (card.suit.isRed) CARD_RED_COLOR else CARD_BLACK_COLOR
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = card.rank.display,
                            color      = textColor,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp
                        )
                        Text(
                            text     = card.suit.symbol,
                            color    = textColor,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                    if (isLast) {
                        // Show large suit symbol centered on the full-height card
                        Text(
                            text      = card.suit.symbol,
                            color     = textColor.copy(alpha = 0.25f),
                            fontSize  = 28.sp,
                            modifier  = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardFace(
    card: Card,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (card.suit.isRed) CARD_RED_COLOR else CARD_BLACK_COLOR
    Box(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(CARD_FACE_COLOR)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CARD_SHAPE)
                else Modifier
            )
            .padding(4.dp)
    ) {
        // Top-left rank + suit
        Row(
            modifier          = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = card.rank.display,
                color      = textColor,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = card.suit.symbol,
                color = textColor,
                fontSize = 11.sp
            )
        }
        // Large centered suit
        Text(
            text      = card.suit.symbol,
            color     = textColor,
            fontSize  = 28.sp,
            modifier  = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun CardBack(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(FACE_DOWN_COLOR)
            .border(1.dp, Color.White.copy(alpha = 0.3f), CARD_SHAPE),
        contentAlignment = Alignment.Center
    ) {
        Text("🂠", fontSize = 22.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun EmptySlotLabel(
    label: String,
    cardHeight: Dp,
    color: Color = Color.White.copy(alpha = 0.5f)
) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text      = label,
            fontSize  = 22.sp,
            color     = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PileSlot(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    highlight: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(EMPTY_SLOT_COLOR)
            .then(
                if (highlight) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CARD_SHAPE)
                else Modifier
            )
            .clickable(onClick = onClick),
        content = content
    )
}
