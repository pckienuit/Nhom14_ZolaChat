# Firebase Scripts

Collection of Python scripts for Firebase management.

## Prerequisites

- Python 3.7+
- firebase-admin package

## Installation

```bash
pip install -r requirements.txt
```

## Setup

1. Go to Firebase Console > Project Settings > Service Accounts
2. Click "Generate new private key"
3. Save the JSON file as `serviceAccountKey.json` in this directory
4. Never commit this file to Git (already in .gitignore)

---

## Scripts

### 1. sync_auth_to_firestore.py

Syncs all users from Firebase Authentication to Firestore users collection.

**Use case**: When you have users in Authentication but not in Firestore.

**Usage**:
```bash
python sync_auth_to_firestore.py
```

**What it does**:
- Fetches all users from Firebase Authentication
- Creates/updates corresponding documents in Firestore `users` collection
- Normalizes emails to lowercase for consistent searching
- Preserves existing device tokens

**Output**:
```
✓ Created: John Doe (john@example.com)
✓ Updated: Jane Smith (jane@example.com)

Sync Summary:
  Created: 5 users
  Updated: 2 users
  Total:   7 users
```

---

### 2. seed_firebase_data.py

Seeds demo data to Firestore for development and testing.

**Use case**: Testing with demo conversations and messages.

**Usage**:
```bash
python seed_firebase_data.py
```

When prompted:
- Enter your Firebase Auth UID (or skip for demo user001)
- Choose yes/no to clear existing data

**Data created**:
- 4 demo users (user002-005)
- 4 conversations with you as member
- 15 messages across conversations

---

### 3. cleanup_firestore.py

⚠️ **DESTRUCTIVE TOOL** - Deletes all friend requests and conversations.

**Use case**: Clean up test data, reset database state.

**Usage**:
```bash
python cleanup_firestore.py
```

**Confirmation required**:
```
⚠️  WARNING: DESTRUCTIVE OPERATION
This will permanently delete:
  • All friend requests
  • All conversations
  • All messages

Type 'DELETE' to confirm: DELETE
```

**What it deletes**:
- All documents in `friendRequests` collection
- All documents in `conversations` collection
- All messages subcollections within conversations

**Output**:
```
✅ Firebase initialized successfully

🗑️  Deleting collection: friendRequests
   Deleted 15 documents...
✅ Total deleted from friendRequests: 15

🗑️  Deleting collection: conversations
   └─ Deleted 23 messages from conversation abc123...
   Deleted 5 documents...
✅ Total deleted from conversations: 5

═══════════════════════════════════════════════
✅ CLEANUP COMPLETED SUCCESSFULLY
   Friend requests deleted: 15
   Conversations deleted:   5
═══════════════════════════════════════════════
```

---

## Common Workflows

### New Developer Setup
```bash
# 1. Install dependencies
pip install -r requirements.txt

# 2. Sync real users from Authentication
python sync_auth_to_firestore.py

# 3. Add demo data for testing
python seed_firebase_data.py
```

### Production Deployment
```bash
# Only sync real users, no demo data
python sync_auth_to_firestore.py
```

### Testing Environment
```bash
# Clear and reseed with demo data
python seed_firebase_data.py
# Choose 'yes' when asked to clear data
```

### Database Reset
```bash
# Delete ALL friend requests and conversations
python cleanup_firestore.py
# Type 'DELETE' to confirm
```

---

## Troubleshooting

**Module not found**
```bash
pip install firebase-admin
```

**Permission denied**
- Check Firestore Rules allow writes
- Verify service account key is correct

**File not found**
- Ensure serviceAccountKey.json is in scripts/ directory

**No users found in Authentication**
- Users must register through the app first
- Or create them in Firebase Console > Authentication
