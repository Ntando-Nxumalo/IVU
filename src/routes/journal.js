const express = require("express");
const router = express.Router();
const { db } = require("../config/firebase");
const { verifyToken } = require("../middleware/authMiddleware");
const { success, failure } = require("../utils/response");

router.use(verifyToken);

// GET /journal - list the logged-in user's journal entries
router.get("/", async (req, res) => {
  try {
    const snapshot = await db
      .collection("journalEntries")
      .where("userId", "==", req.user.uid)
      .orderBy("date", "desc")
      .get();

    const entries = snapshot.docs.map((doc) => ({ entryId: doc.id, ...doc.data() }));
    return success(res, entries);
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to fetch journal entries", 500);
  }
});

// POST /journal - create a new journal entry
router.post("/", async (req, res) => {
  try {
    const { date, mood, text, linkedDeckId = null } = req.body;

    if (!date || !mood || !text) {
      return failure(res, "date, mood, and text are required", 400);
    }
    if (!["great", "okay", "tough"].includes(mood)) {
      return failure(res, "mood must be one of: great, okay, tough", 400);
    }

    const newEntry = {
      userId: req.user.uid,
      date,
      mood,
      text,
      linkedDeckId,
      createdAt: Date.now(),
    };

    const docRef = await db.collection("journalEntries").add(newEntry);
    return success(res, { entryId: docRef.id, ...newEntry }, 201);
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to create journal entry", 500);
  }
});

module.exports = router;
