const express = require("express");
const router = express.Router();
const { db } = require("../config/firebase");
const { verifyToken } = require("../middleware/authMiddleware");
const { success, failure } = require("../utils/response");
const { scheduleNextReview } = require("../utils/spacedRepetition");

router.use(verifyToken);

// Helper: confirm the deck exists and belongs to the requesting user
async function getOwnedDeck(deckId, uid) {
  const deckRef = db.collection("decks").doc(deckId);
  const deckDoc = await deckRef.get();
  if (!deckDoc.exists) return null;
  if (deckDoc.data().ownerId !== uid) return null;
  return deckRef;
}

// GET /decks/:deckId/cards - list all cards in a deck
router.get("/:deckId/cards", async (req, res) => {
  try {
    const deckRef = await getOwnedDeck(req.params.deckId, req.user.uid);
    if (!deckRef) return failure(res, "Deck not found or not authorized", 404);

    const snapshot = await deckRef.collection("flashcards").get();
    const cards = snapshot.docs.map((doc) => ({ cardId: doc.id, ...doc.data() }));
    return success(res, cards);
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to fetch cards", 500);
  }
});

// GET /decks/:deckId/cards/due - list only cards due for review
router.get("/:deckId/cards/due", async (req, res) => {
  try {
    const deckRef = await getOwnedDeck(req.params.deckId, req.user.uid);
    if (!deckRef) return failure(res, "Deck not found or not authorized", 404);

    const now = Date.now();
    const snapshot = await deckRef
      .collection("flashcards")
      .where("dueDate", "<=", now)
      .get();

    const cards = snapshot.docs.map((doc) => ({ cardId: doc.id, ...doc.data() }));
    return success(res, cards);
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to fetch due cards", 500);
  }
});

// POST /decks/:deckId/cards - create a new card
router.post("/:deckId/cards", async (req, res) => {
  try {
    const deckRef = await getOwnedDeck(req.params.deckId, req.user.uid);
    if (!deckRef) return failure(res, "Deck not found or not authorized", 404);

    const { frontText, backText, imageUrl = null } = req.body;
    if (!frontText || !backText) {
      return failure(res, "frontText and backText are required", 400);
    }

    const newCard = {
      frontText,
      backText,
      imageUrl,
      easeFactor: 2.5,
      intervalDays: 0,
      repetitions: 0,
      dueDate: Date.now(), // due immediately, first review
    };

    const cardDoc = await deckRef.collection("flashcards").add(newCard);
    await deckRef.update({ cardCount: (await deckRef.collection("flashcards").get()).size });

    return success(res, { cardId: cardDoc.id, ...newCard }, 201);
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to create card", 500);
  }
});

// PUT /decks/:deckId/cards/:cardId/review - submit a review rating
router.put("/:deckId/cards/:cardId/review", async (req, res) => {
  try {
    const deckRef = await getOwnedDeck(req.params.deckId, req.user.uid);
    if (!deckRef) return failure(res, "Deck not found or not authorized", 404);

    const { rating } = req.body; // "again" | "hard" | "good" | "easy"
    if (!["again", "hard", "good", "easy"].includes(rating)) {
      return failure(res, "rating must be one of: again, hard, good, easy", 400);
    }

    const cardRef = deckRef.collection("flashcards").doc(req.params.cardId);
    const cardDoc = await cardRef.get();
    if (!cardDoc.exists) return failure(res, "Card not found", 404);

    const updated = scheduleNextReview(cardDoc.data(), rating);
    await cardRef.update(updated);

    return success(res, { cardId: req.params.cardId, ...cardDoc.data(), ...updated });
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to submit review", 500);
  }
});

// DELETE /decks/:deckId/cards/:cardId
router.delete("/:deckId/cards/:cardId", async (req, res) => {
  try {
    const deckRef = await getOwnedDeck(req.params.deckId, req.user.uid);
    if (!deckRef) return failure(res, "Deck not found or not authorized", 404);

    await deckRef.collection("flashcards").doc(req.params.cardId).delete();
    return success(res, { cardId: req.params.cardId, deleted: true });
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to delete card", 500);
  }
});

module.exports = router;
