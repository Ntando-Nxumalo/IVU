/**
 * SM-2 spaced repetition scheduler.
 * rating: "again" | "hard" | "good" | "easy"
 */
function scheduleNextReview(card, rating) {
  let { easeFactor = 2.5, intervalDays = 0, repetitions = 0 } = card;

  if (rating === "again") {
    repetitions = 0;
    intervalDays = 1;
    easeFactor = Math.max(1.3, easeFactor - 0.2);
  } else {
    repetitions += 1;

    if (repetitions === 1) {
      intervalDays = 1;
    } else if (repetitions === 2) {
      intervalDays = 6;
    } else {
      intervalDays = Math.round(intervalDays * easeFactor);
    }

    if (rating === "hard") {
      easeFactor = Math.max(1.3, easeFactor - 0.15);
    } else if (rating === "easy") {
      easeFactor = easeFactor + 0.15;
    }
    // "good" leaves easeFactor unchanged
  }

  const dueDate = Date.now() + intervalDays * 24 * 60 * 60 * 1000;

  return { easeFactor, intervalDays, repetitions, dueDate };
}

module.exports = { scheduleNextReview };
