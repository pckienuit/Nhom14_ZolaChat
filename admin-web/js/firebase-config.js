/**
 * Firebase Configuration for ZaloClone Admin
 * ============================================
 * 
 * This file contains the Firebase SDK initialization.
 * Firebase config is loaded from the project's google-services.json
 */

// Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyCnPXSXyG-jXljq-nNEAAkz0c6Q00e04Hg",
    authDomain: "zalo-clone-dd021.firebaseapp.com",
    projectId: "zalo-clone-dd021",
    storageBucket: "zalo-clone-dd021.firebasestorage.app",
    messagingSenderId: "858555050734",
    appId: "1:858555050734:android:91db84289a0940ece1e1ff"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Initialize services
const auth = firebase.auth();
const db = firebase.firestore();

// Enable Firestore offline persistence (optional)
db.enablePersistence({ synchronizeTabs: true }).catch((err) => {
    if (err.code === 'failed-precondition') {
        console.warn('Firestore persistence requires only one tab open at a time.');
    } else if (err.code === 'unimplemented') {
        console.warn('Firestore persistence is not available in this browser.');
    }
});

console.log('Firebase initialized successfully');
