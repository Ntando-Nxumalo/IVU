require("dotenv").config();
const express = require("express");
const cors = require("cors");

const deckRoutes = require("./routes/decks");
const flashcardRoutes = require("./routes/flashcards");
const journalRoutes = require("./routes/journal");
const aiRoutes = require("./routes/ai");

const app = express();

app.use(cors());
app.use(express.json());

// Health check - useful to confirm the API is live once hosted
app.get("/", (req, res) => {
  res.json({ success: true, data: "IVU API is running", error: null });
});

app.use("/decks", deckRoutes);
app.use("/decks", flashcardRoutes); // nested under /decks/:deckId/cards
app.use("/journal", journalRoutes);
app.use("/ai", aiRoutes);

// Catch-all 404
app.use((req, res) => {
  res.status(404).json({ success: false, data: null, error: "Route not found" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`IVU API listening on port ${PORT}`);
});

module.exports = app;
