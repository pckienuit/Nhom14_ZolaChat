#!/usr/bin/env python3
"""
Firebase Data Seeder
Seeds demo data to Firestore for development/testing
"""

import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime, timedelta
import sys

SERVICE_ACCOUNT_KEY = "serviceAccountKey.json"

# Demo users (these are placeholders, user001 will be replaced with real UID)
DEMO_USERS = [
    {"id": "user001", "name": "Nguyễn Văn An", "email": "nguyenvanan@example.com", "avatarUrl": "", "devices": {}},
    {"id": "user002", "name": "Trần Thị Bích", "email": "tranthibich@example.com", "avatarUrl": "", "devices": {}},
    {"id": "user003", "name": "Lê Hoàng Châu", "email": "lehoangchau@example.com", "avatarUrl": "", "devices": {}},
    {"id": "user004", "name": "Phạm Minh Đức", "email": "phamminhduc@example.com", "avatarUrl": "", "devices": {}},
    {"id": "user005", "name": "Hoàng Thu Hà", "email": "hoangthuha@example.com", "avatarUrl": "", "devices": {}}
]

DEMO_CONVERSATIONS = [
    {"id": "conv001", "name": "Trần Thị Bích", "lastMessage": "Hẹn gặp lại bạn nhé!", "memberIds": ["user001", "user002"]},
    {"id": "conv002", "name": "Lê Hoàng Châu", "lastMessage": "Cảm ơn bạn nhiều!", "memberIds": ["user001", "user003"]},
    {"id": "conv003", "name": "Phạm Minh Đức", "lastMessage": "Ok, tôi đồng ý", "memberIds": ["user001", "user004"]},
    {"id": "conv004", "name": "Hoàng Thu Hà", "lastMessage": "Chào buổi sáng!", "memberIds": ["user001", "user005"]}
]

DEMO_MESSAGES = {
    "conv001": [
        {"senderId": "user001", "content": "Chào bạn! Bạn có khỏe không?", "type": "text"},
        {"senderId": "user002", "content": "Chào! Mình khỏe, cảm ơn bạn!", "type": "text"},
        {"senderId": "user001", "content": "Tuần này mình có rảnh, hẹn gặp nhé", "type": "text"},
        {"senderId": "user002", "content": "Hẹn gặp lại bạn nhé!", "type": "text"}
    ],
    "conv002": [
        {"senderId": "user001", "content": "Bạn đã hoàn thành báo cáo chưa?", "type": "text"},
        {"senderId": "user003", "content": "Rồi, mình gửi file cho bạn ngay", "type": "text"},
        {"senderId": "user001", "content": "Cảm ơn bạn rất nhiều!", "type": "text"},
        {"senderId": "user003", "content": "Cảm ơn bạn nhiều!", "type": "text"}
    ],
    "conv003": [
        {"senderId": "user004", "content": "Ngày mai họp lúc mấy giờ vậy?", "type": "text"},
        {"senderId": "user001", "content": "9 giờ sáng nhé", "type": "text"},
        {"senderId": "user004", "content": "Ok, tôi đồng ý", "type": "text"}
    ],
    "conv004": [
        {"senderId": "user005", "content": "Chào buổi sáng!", "type": "text"},
        {"senderId": "user001", "content": "Chào bạn! Hôm nay thế nào?", "type": "text"},
        {"senderId": "user005", "content": "Mình ổn, chuẩn bị đi làm đây", "type": "text"}
    ]
}


class FirebaseSeeder:
    def __init__(self, service_account_path, real_user_id=None):
        print("Initializing Firebase Admin SDK...")
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred)
        self.db = firestore.client()
        self.real_user_id = real_user_id
        print("Initialized.\n")

    def clear_collection(self, collection_name):
        print(f"Clearing collection: {collection_name}")
        docs = self.db.collection(collection_name).stream()
        count = 0
        for doc in docs:
            doc.reference.delete()
            count += 1
        print(f"Deleted {count} documents\n")

    def seed_users(self):
        print("Seeding users...")
        for user in DEMO_USERS:
            # Skip user001 if we're using real UID (already exists in Firebase Auth)
            if user["id"] == "user001" and self.real_user_id:
                print(f"  Skipped: {user['name']} (using real Firebase Auth user)")
                continue
            
            self.db.collection("users").document(user["id"]).set(user)
            print(f"  Created: {user['name']}")
        print(f"Done\n")

    def seed_conversations(self):
        print("Seeding conversations...")
        base_time = datetime.now()
        
        for i, conv in enumerate(DEMO_CONVERSATIONS):
            timestamp = int((base_time - timedelta(hours=i)).timestamp() * 1000)
            
            # Replace user001 with real UID in memberIds
            member_ids = conv["memberIds"].copy()
            if self.real_user_id:
                member_ids = [self.real_user_id if uid == "user001" else uid for uid in member_ids]
            
            conv_data = {
                "id": conv["id"],
                "name": conv["name"],
                "lastMessage": conv["lastMessage"],
                "timestamp": timestamp,
                "memberIds": member_ids
            }
            self.db.collection("conversations").document(conv["id"]).set(conv_data)
            print(f"  Created: {conv['name']} (members: {member_ids})")
        
        print(f"Done\n")

    def seed_messages(self):
        print("Seeding messages...")
        base_time = datetime.now()
        
        for conv_id, messages in DEMO_MESSAGES.items():
            for i, msg_data in enumerate(messages):
                timestamp = int((base_time - timedelta(minutes=len(messages)-i)).timestamp() * 1000)
                
                # Replace user001 with real UID in senderId
                sender_id = msg_data["senderId"]
                if self.real_user_id and sender_id == "user001":
                    sender_id = self.real_user_id
                
                message = {
                    "senderId": sender_id,
                    "content": msg_data["content"],
                    "type": msg_data["type"],
                    "timestamp": timestamp
                }
                doc_ref = self.db.collection("conversations").document(conv_id).collection("messages").document()
                message["id"] = doc_ref.id
                doc_ref.set(message)
        
        total = sum(len(msgs) for msgs in DEMO_MESSAGES.values())
        print(f"Done: {total} messages\n")

    def run(self, clear_existing=False):
        print("=" * 60)
        print("Firebase Data Seeder")
        print("=" * 60)
        print()
        
        if self.real_user_id:
            print(f"Using real Firebase Auth UID: {self.real_user_id}")
            print(f"This will replace 'user001' in all conversations\n")
        
        if clear_existing:
            self.clear_collection("users")
            self.clear_collection("conversations")
        
        self.seed_users()
        self.seed_conversations()
        self.seed_messages()
        
        print("=" * 60)
        print("Seeding completed successfully")
        print("=" * 60)
        print("\nYou can now open the app and see conversations!")


def main():
    import os
    
    if not os.path.exists(SERVICE_ACCOUNT_KEY):
        print("ERROR: serviceAccountKey.json not found")
        print("\nGet it from:")
        print("Firebase Console > Project Settings > Service Accounts")
        print("> Generate new private key\n")
        sys.exit(1)
    
    print("=" * 60)
    print("Firebase Data Seeder Setup")
    print("=" * 60)
    print()
    print("Do you want to use your real Firebase Auth UID?")
    print("Get your UID from:")
    print("  - Firebase Console > Authentication > Users")
    print("  - Or from Android Logcat when you login")
    print()
    print("Enter your Firebase Auth UID (or press Enter to skip):")
    real_uid = input("> ").strip()
    
    if not real_uid:
        real_uid = None
        print("\nNo UID provided. Using placeholder 'user001'")
        print("Note: You won't see conversations unless user001 exists in your app\n")
    else:
        print(f"\nUsing UID: {real_uid}\n")
    
    print("Clear existing data before seeding? (yes/no)")
    choice = input("> ").strip().lower()
    clear_existing = choice == 'yes'
    print()
    
    seeder = FirebaseSeeder(SERVICE_ACCOUNT_KEY, real_uid)
    seeder.run(clear_existing=clear_existing)


if __name__ == "__main__":
    main()
