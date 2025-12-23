/**
 * Set Admin Custom Claim cho user
 * Chạy: node scripts/set_admin_claim.js phanchikien@gmail.com
 */

const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const email = process.argv[2];

if (!email) {
  console.error('Usage: node set_admin_claim.js <email>');
  process.exit(1);
}

admin.auth().getUserByEmail(email)
  .then(user => {
    return admin.auth().setCustomUserClaims(user.uid, { admin: true });
  })
  .then(() => {
    console.log(`✅ Admin claim set for: ${email}`);
    console.log('User needs to sign out and sign in again for changes to take effect.');
    process.exit(0);
  })
  .catch(error => {
    console.error('❌ Error:', error.message);
    process.exit(1);
  });
