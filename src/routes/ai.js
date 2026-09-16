const express = require("express");
const router = express.Router();
const { verifyToken } = require("../middleware/authMiddleware");
const { success, failure } = require("../utils/response");

// AI routes require a valid logged-in user
router.use(verifyToken);


// GET /ai/ping - Quick test to see if the AI route is alive
router.get("/ping", (req, res) => {
  return success(res, "AI route is active and authorized");
});

const GROQ_API_KEY = process.env.GROQ_API_KEY;
const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

router.post("/ask", async (req, res) => {
  try {
    const { prompt } = req.body;

    console.log("AI request received with prompt:", prompt);

    if (!prompt || typeof prompt !== "string") {
      return failure(res, "prompt is required", 400);
    }

    if (!GROQ_API_KEY) {
      console.error("GROQ_API_KEY is missing in environment variables");
      return failure(res, "AI service not configured on server", 500);
    }

    const systemInstruction =
      "You are IVU AI Assist, a friendly study helper inside a flashcard app. " +
      "Help the user understand flashcard content, give extra example sentences, " +
      "or quiz them conversationally. Keep answers short, warm, and encouraging.";

    console.log("Calling Groq API...");
    const response = await fetch(GROQ_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${GROQ_API_KEY}`,
      },
      body: JSON.stringify({
        model: "openai/gpt-oss-120b", // Use the user-specified Groq model
        messages: [
          { role: "system", content: systemInstruction },
          { role: "user", content: prompt },
        ],
        temperature: 0.7,
        max_tokens: 500,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      console.error("Groq API error response:", data);
      return failure(res, `AI service request failed: ${data.error?.message || response.statusText}`, 502);
    }

    const replyText =
      data.choices?.[0]?.message?.content ??
      "Sorry, I couldn't come up with a response for that.";

    console.log("AI response generated successfully");
    return success(res, { reply: replyText });
  } catch (err) {
    console.error("AI Route Exception:", err);
    return failure(res, "Internal server error processing AI request", 500);
  }
});

module.exports = router;
