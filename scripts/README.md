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

When prompted:
- Press Enter to keep existing data
- Type "yes" to clear all existing data before seeding

## Data Created

### Users (5)
- user001: Nguyễn Văn An (nguyenvanan@example.com)
- user002: Trần Thị Bích (tranthibich@example.com)
- user003: Lê Hoàng Châu (lehoangchau@example.com)
- user004: Phạm Minh Đức (phamminhduc@example.com)
- user005: Hoàng Thu Hà (hoangthuha@example.com)

### Conversations (4)
Each conversation between user001 and another user, includes:
- memberIds array
- lastMessage
- timestamp

### Messages (15 total)
3-4 messages per conversation with proper timestamps.

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
