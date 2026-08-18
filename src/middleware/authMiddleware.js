const { auth } = require("../config/firebase");

/**
 * Verifies the Firebase ID token sent in the Authorization header
 * as "Bearer <token>". Attaches the decoded user (uid, email, etc.)
 * to req.user if valid.
 */
async function verifyToken(req, res, next) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({
      success: false,
      data: null,
      error: "Missing or malformed Authorization header",
    });
  }

  const idToken = authHeader.split("Bearer ")[1];

  try {
    const decodedToken = await auth.verifyIdToken(idToken);
    req.user = decodedToken; // contains uid, email, etc.
    next();
  } catch (err) {
    console.error("Token verification failed:", err.message);
    return res.status(401).json({
      success: false,
      data: null,
      error: "Invalid or expired token",
    });
  }
}

module.exports = { verifyToken };
