const admin = require("firebase-admin");
const path = require("path");
const { getFirestore } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");

// In production (Render), the key is loaded from an environment variable.
// Locally, it's loaded from the serviceAccountKey.json file.
let serviceAccount;

if (process.env.FIREBASE_SERVICE_ACCOUNT) {
  serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
} else {
  serviceAccount = require(path.join(__dirname, "../../serviceAccountKey.json"));
}

admin.initializeApp({
  credential: admin.cert(serviceAccount),
});

const db = getFirestore();
const auth = getAuth();

module.exports = { admin, db, auth };