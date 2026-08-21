const express = require("express");
const router = express.Router();
const { verifyToken } = require("../middleware/authMiddleware");
const { success, failure } = require("../utils/response");

// AI routes require a valid logged-in user
router.use(verifyToken);

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${GEMINI_API_KEY}`;

router.post("/ask", async (req, res) => {
  try {
    const { prompt } = req.body;

    if (!prompt || typeof prompt !== "string") {
      return failure(res, "prompt is required", 400);
    }

    if (!GEMINI_API_KEY) {
      return failure(res, "AI service not configured", 500);
    }

    const systemInstruction =
      "You are IVU AI Assist, a friendly study helper inside a flashcard app. " +
      "Help the user understand flashcard content, give extra example sentences, " +
      "or quiz them conversationally. Keep answers short, warm, and encouraging.";

    const response = await fetch(GEMINI_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        systemInstruction: { parts: [{ text: systemInstruction }] },
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      console.error("Gemini API error:", data);
      return failure(res, "AI service request failed", 502);
    }

    const replyText =
      data.candidates?.[0]?.content?.parts?.[0]?.text ??
      "Sorry, I couldn't come up with a response for that.";

    return success(res, { reply: replyText });
  } catch (err) {
    console.error(err);
    return failure(res, "Failed to process AI request", 500);
  }
});

module.exports = router;
