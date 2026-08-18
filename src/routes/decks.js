const express = require("express");
const router = express.Router();
const { db } = require("../config/firebase");
const { verifyToken } = require("../middleware/authMiddleware");
const { success, failure } = require("../utils/response");

// All deck routes require a valid logged-in user
router.use(verifyToken);

// GET /decks - list all decks owned by the logged-in user
router.get("/", async (req, res) => {
  try {
    const snapshot = await db
      .collection("decks")
      .where("ownerId", "==", req.user.uid)
      .get();

    const decks = snapshot.docs.map((doc) => ({ deckId: doc.id, ...doc.data() }));
    return success(res, decks);
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to fetch decks", 500);
  }
});

// POST /decks - create a new deck
router.post("/", async (req, res) => {
  try {
    const { title, language } = req.body;

    if (!title || !language) {
      return failure(res, "title and language are required", 400);
    }

    const newDeck = {
      ownerId: req.user.uid,
      title,
      language, // "en" | "zu" | "af"
      cardCount: 0,
      createdAt: Date.now(),
    };

    const docRef = await db.collection("decks").add(newDeck);
    return success(res, { deckId: docRef.id, ...newDeck }, 201);
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to create deck", 500);
  }
});

// DELETE /decks/:id - delete a deck (only if owned by the user)
router.delete("/:id", async (req, res) => {
  try {
    const deckRef = db.collection("decks").doc(req.params.id);
    const deckDoc = await deckRef.get();

    if (!deckDoc.exists) {
      return failure(res, "Deck not found", 404);
    }
    if (deckDoc.data().ownerId !== req.user.uid) {
      return failure(res, "Not authorized to delete this deck", 403);
    }

    await deckRef.delete();
    return success(res, { deckId: req.params.id, deleted: true });
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to delete deck", 500);
  }
});

module.exports = router;
