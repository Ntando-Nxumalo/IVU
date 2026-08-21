const express = require("express");
const router = express.Router();
const { verifyToken } = require("../middleware/authMiddleware");
const { success, failure } = require("../utils/response");

// AI routes require a valid logged-in user
router.use(verifyToken);

/**
 * POST /ai/ask
 * Body: { prompt: string }
 */
router.post("/ask", async (req, res) => {
  try {
    const { prompt } = req.body;

    if (!prompt) {
      return failure(res, "Prompt is required", 400);
    }

    console.log(`AI Prompt from user ${req.user.uid}: ${prompt}`);

    // TODO: Integrate with OpenAI, Gemini, or another AI provider here.
    // For now, we return a mock response to verify the connection.
    const mockReply = `Hello! I received your message: "${prompt}". This is a mock response from the IVU AI backend. Once you add your AI API key, I'll be able to help you study more effectively!`;

    return success(res, { reply: mockReply });
  } catch (err) {
    console.error("AI Error:", err);
    return failure(res, "Failed to process AI request", 500);
  }
});

module.exports = router;
