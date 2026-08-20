import admin from 'firebase-admin';

/**
 * Firebase Admin is initialized lazily (on first use) on purpose.
 *
 * During `next build`, Next.js imports route modules to collect page data, and
 * several API routes import this module transitively. Initializing Firebase at
 * module scope would crash the build with "Service account object must contain
 * a string project_id property" whenever the FIREBASE_* env vars are missing or
 * not yet loaded. Deferring initialization keeps the build green and produces a
 * clear, actionable error only when a notification is actually sent.
 */
const REQUIRED_ENV_KEYS = [
  'FIREBASE_TYPE',
  'FIREBASE_PROJECT_ID',
  'FIREBASE_PRIVATE_KEY_ID',
  'FIREBASE_PRIVATE_KEY',
  'FIREBASE_CLIENT_EMAIL',
  'FIREBASE_CLIENT_ID',
] as const;

function hasFirebaseCredentials(): boolean {
  return REQUIRED_ENV_KEYS.every(
    (key) => typeof process.env[key] === 'string' && (process.env[key] as string).length > 0
  );
}

function getServiceAccount() {
  return {
    type: process.env.FIREBASE_TYPE,
    project_id: process.env.FIREBASE_PROJECT_ID,
    private_key_id: process.env.FIREBASE_PRIVATE_KEY_ID,
    private_key: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    client_email: process.env.FIREBASE_CLIENT_EMAIL,
    client_id: process.env.FIREBASE_CLIENT_ID,
    auth_uri: process.env.FIREBASE_AUTH_URI,
    token_uri: process.env.FIREBASE_TOKEN_URI,
    auth_provider_x509_cert_url: process.env.FIREBASE_AUTH_PROVIDER_CERT_URL,
    client_x509_cert_url: process.env.FIREBASE_CLIENT_CERT_URL,
  } as admin.ServiceAccount;
}

export function getAdmin(): typeof admin {
  if (!admin.apps.length) {
    if (!hasFirebaseCredentials()) {
      throw new Error(
        'Firebase Admin credentials are missing. Add FIREBASE_TYPE, FIREBASE_PROJECT_ID, ' +
          'FIREBASE_PRIVATE_KEY_ID, FIREBASE_PRIVATE_KEY, FIREBASE_CLIENT_EMAIL and ' +
          'FIREBASE_CLIENT_ID to the server .env file.'
      );
    }
    admin.initializeApp({
      credential: admin.credential.cert(getServiceAccount()),
    });
  }
  return admin;
}

// Exported for compatibility; prefer getAdmin() so initialization is deferred.
export { admin };
