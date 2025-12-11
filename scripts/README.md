# Firebase Data Seeder

Python script to populate Firestore with demo data for development and testing.

## Requirements

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

## Usage

```bash
python seed_firebase_data.py
```

### Step-by-step:

1. **Get your Firebase Auth UID**
   - Option 1: Firebase Console > Authentication > Users tab > Copy UID
   - Option 2: Check Android Logcat when you login to the app

2. **Run the script**
   ```bash
   python seed_firebase_data.py
   ```

3. **Enter your UID when prompted**
   ```
   Enter your Firebase Auth UID (or press Enter to skip):
   > kO2Oy3j2kNVzyj06eiFdeFEBlwA2
   ```

4. **Choose whether to clear existing data**
   ```
   Clear existing data before seeding? (yes/no)
   > yes
   ```

## What the Script Does

### If you provide your real UID:
- Replaces `user001` with your real UID in all conversations
- Creates conversations where YOU are one of the members
- Creates messages where YOU are the sender
- You will see these conversations immediately in the app

### If you skip UID:
- Uses placeholder `user001` for all conversations
- You won't see conversations unless `user001` exists in your app

## Data Created

### Users (4)
- user002: Trần Thị Bích (tranthibich@example.com)
- user003: Lê Hoàng Châu (lehoangchau@example.com)
- user004: Phạm Minh Đức (phamminhduc@example.com)
- user005: Hoàng Thu Hà (hoangthuha@example.com)

Note: user001 is skipped when using real UID

### Conversations (4)
Each conversation between you (real UID) and another user:
- Your UID + user002
- Your UID + user003
- Your UID + user004
- Your UID + user005

Each conversation includes:
- memberIds array with your UID
- lastMessage
- timestamp (sorted newest first)

### Messages (15 total)
3-4 messages per conversation with:
- Messages from you (your UID as senderId)
- Messages from the other user
- Proper timestamps

## Example

```bash
$ python seed_firebase_data.py

Enter your Firebase Auth UID (or press Enter to skip):
> kO2Oy3j2kNVzyj06eiFdeFEBlwA2

Using UID: kO2Oy3j2kNVzyj06eiFdeFEBlwA2

Clear existing data before seeding? (yes/no)
> yes

Initializing Firebase Admin SDK...
Initialized.

Clearing collection: users
Deleted 5 documents

Clearing collection: conversations
Deleted 4 documents

Seeding users...
  Skipped: Nguyễn Văn An (using real Firebase Auth user)
  Created: Trần Thị Bích
  Created: Lê Hoàng Châu
  Created: Phạm Minh Đức
  Created: Hoàng Thu Hà
Done

Seeding conversations...
  Created: Trần Thị Bích (members: ['kO2Oy3j2kNVzyj06eiFdeFEBlwA2', 'user002'])
  Created: Lê Hoàng Châu (members: ['kO2Oy3j2kNVzyj06eiFdeFEBlwA2', 'user003'])
  Created: Phạm Minh Đức (members: ['kO2Oy3j2kNVzyj06eiFdeFEBlwA2', 'user004'])
  Created: Hoàng Thu Hà (members: ['kO2Oy3j2kNVzyj06eiFdeFEBlwA2', 'user005'])
Done

Seeding messages...
Done: 15 messages

Seeding completed successfully
You can now open the app and see conversations!
```

## Customization

Edit these variables in `seed_firebase_data.py`:
- `DEMO_USERS` - List of demo users
- `DEMO_CONVERSATIONS` - List of conversations
- `DEMO_MESSAGES` - Messages for each conversation

## Firestore Structure

```
/users/{userId}
  - id, name, email, avatarUrl, devices

/conversations/{conversationId}
  - id, name, lastMessage, timestamp, memberIds
  
  /messages/{messageId}
    - id, senderId, content, type, timestamp
```

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

**No conversations showing in app**
- Make sure you entered the correct UID
- Check that UID matches the user logged into the app
- Verify memberIds in Firestore contains your UID
- Check Android Logcat for any errors
