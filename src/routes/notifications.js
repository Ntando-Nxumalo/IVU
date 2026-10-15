const express = require("express");
const router = express.Router();
const { admin, db } = require("../config/firebase");
const { verifyToken } = require("../middleware/authMiddleware");
const { success, failure } = require("../utils/response");

/**
 * 1. POST /notifications/register-token
 * Body: { token, reminderEnabled (default true), reminderHour (default 18) }
 * Upsert into "deviceTokens" collection with doc ID = req.user.uid
 */
router.post("/register-token", verifyToken, async (req, res) => {
  try {
    const { token, reminderEnabled, reminderHour } = req.body;

    if (!token) {
      return failure(res, "Token is required", 400);
    }

    const isEnabled = reminderEnabled !== undefined ? Boolean(reminderEnabled) : true;
    const hour = reminderHour !== undefined ? Number(reminderHour) : 18;

    const tokenData = {
      userId: req.user.uid,
      token,
      reminderEnabled: isEnabled,
      reminderHour: hour,
      updatedAt: Date.now(),
    };

    await db.collection("deviceTokens").doc(req.user.uid).set(tokenData, { merge: true });

    return success(res, tokenData);
  } catch (err) {
    console.error("Error registering token:", err);
    return failure(res, "Failed to register device token", 500);
  }
});

/**
 * 2. DELETE /notifications/token
 * Delete the deviceTokens document for req.user.uid
 */
router.delete("/token", verifyToken, async (req, res) => {
  try {
    await db.collection("deviceTokens").doc(req.user.uid).delete();
    return success(res, { deleted: true });
  } catch (err) {
    console.error("Error deleting token:", err);
    return failure(res, "Failed to delete device token", 500);
  }
});

/**
 * 3. POST /notifications/test
 * Body: { delaySeconds } (clamp to 0-30)
 * Look up caller's device token (404 if none). Responds immediately with { scheduled: true }.
 * After delay, sends "IVU" notification.
 */
router.post("/test", verifyToken, async (req, res) => {
  try {
    const rawDelay = req.body.delaySeconds;
    let delaySeconds = parseInt(rawDelay, 10);
    if (isNaN(delaySeconds)) {
      delaySeconds = 0;
    }
    delaySeconds = Math.max(0, Math.min(30, delaySeconds));

    const docRef = db.collection("deviceTokens").doc(req.user.uid);
    const docSnap = await docRef.get();

    if (!docSnap.exists || !docSnap.data()?.token) {
      return failure(res, "Device token not found", 404);
    }

    const token = docSnap.data().token;

    const message = {
      token,
      notification: {
        title: "IVU",
        body: "Time for a quick review!",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "ivu_reminders",
        },
      },
    };

    setTimeout(async () => {
      try {
        await admin.messaging().send(message);
      } catch (err) {
        if (
          err.code === "messaging/registration-token-not-registered" ||
          err.errorInfo?.code === "messaging/registration-token-not-registered"
        ) {
          await docRef.delete();
        } else {
          console.error("FCM test notification error:", err);
        }
      }
    }, delaySeconds * 1000);

    return success(res, { scheduled: true });
  } catch (err) {
    console.error("Error scheduling test notification:", err);
    return failure(res, "Failed to schedule test notification", 500);
  }
});

/**
 * 4. POST /notifications/send-reminders
 * Protected by header x-cron-secret matching process.env.CRON_SECRET (403 if unset/mismatch).
 * Queries deviceTokens for reminderEnabled == true and reminderHour == current SAST hour.
 * Sends "Keep your streak alive" reminders. Responds with { hour, sent }.
 */
router.post("/send-reminders", async (req, res) => {
  try {
    const cronSecret = process.env.CRON_SECRET;
    const headerSecret = req.headers["x-cron-secret"];

    if (!cronSecret || headerSecret !== cronSecret) {
      return failure(res, "Unauthorized", 403);
    }

    const formatter = new Intl.DateTimeFormat("en-US", {
      timeZone: "Africa/Johannesburg",
      hour: "numeric",
      hourCycle: "h23",
    });
    const currentHour = parseInt(formatter.format(new Date()), 10);

    const snapshot = await db
      .collection("deviceTokens")
      .where("reminderEnabled", "==", true)
      .where("reminderHour", "==", currentHour)
      .get();

    let sentCount = 0;
    const sendPromises = snapshot.docs.map(async (docSnap) => {
      const data = docSnap.data();
      if (!data.token) return;

      const message = {
        token: data.token,
        notification: {
          title: "IVU",
          body: "Keep your streak alive",
        },
        android: {
          priority: "high",
          notification: {
            channelId: "ivu_reminders",
          },
        },
      };

      try {
        await admin.messaging().send(message);
        sentCount++;
      } catch (err) {
        if (
          err.code === "messaging/registration-token-not-registered" ||
          err.errorInfo?.code === "messaging/registration-token-not-registered"
        ) {
          await docSnap.ref.delete();
        } else {
          console.error(`FCM reminder error for user ${docSnap.id}:`, err);
        }
      }
    });

    await Promise.all(sendPromises);

    return success(res, { hour: currentHour, sent: sentCount });
  } catch (err) {
    console.error("Error sending reminders:", err);
    return failure(res, "Failed to send reminders", 500);
  }
});

module.exports = router;
