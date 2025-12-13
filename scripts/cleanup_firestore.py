#!/usr/bin/env python3
"""
Firestore Cleanup Tool
Deletes all friend requests and conversations from Firebase Firestore
USE WITH CAUTION - This will permanently delete data!
"""

import firebase_admin
from firebase_admin import credentials, firestore
import sys
import os

def init_firebase():
    """Initialize Firebase Admin SDK"""
    # Path to your service account key JSON file
    # Download from Firebase Console > Project Settings > Service Accounts
    cred_path = os.path.join(os.path.dirname(__file__), 'serviceAccountKey.json')
    
    if not os.path.exists(cred_path):
        print("❌ ERROR: serviceAccountKey.json not found!")
        print(f"   Please download it from Firebase Console and place it at:")
        print(f"   {cred_path}")
        sys.exit(1)
    
    try:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        print("✅ Firebase initialized successfully")
        return firestore.client()
    except Exception as e:
        print(f"❌ Failed to initialize Firebase: {e}")
        sys.exit(1)

def delete_collection(db, collection_name, batch_size=100):
    """Delete all documents in a collection"""
    print(f"\n🗑️  Deleting collection: {collection_name}")
    
    coll_ref = db.collection(collection_name)
    deleted_count = 0
    
    while True:
        # Get a batch of documents
        docs = coll_ref.limit(batch_size).stream()
        deleted_in_batch = 0
        
        for doc in docs:
            # If it's a conversation, delete messages subcollection first
            if collection_name == 'conversations':
                delete_messages(db, doc.id)
            
            doc.reference.delete()
            deleted_in_batch += 1
            deleted_count += 1
        
        if deleted_in_batch == 0:
            break
        
        print(f"   Deleted {deleted_in_batch} documents...")
    
    print(f"✅ Total deleted from {collection_name}: {deleted_count}")
    return deleted_count

def delete_messages(db, conversation_id):
    """Delete all messages in a conversation"""
    messages_ref = db.collection('conversations').document(conversation_id).collection('messages')
    deleted = 0
    
    for message_doc in messages_ref.stream():
        message_doc.reference.delete()
        deleted += 1
    
    if deleted > 0:
        print(f"   └─ Deleted {deleted} messages from conversation {conversation_id[:8]}...")

def delete_friend_requests(db):
    """Delete all friend requests"""
    return delete_collection(db, 'friendRequests')

def delete_conversations(db):
    """Delete all conversations and their messages"""
    return delete_collection(db, 'conversations')

def confirm_action():
    """Ask user for confirmation"""
    print("\n" + "="*60)
    print("⚠️  WARNING: DESTRUCTIVE OPERATION")
    print("="*60)
    print("This will permanently delete:")
    print("  • All friend requests")
    print("  • All conversations")
    print("  • All messages")
    print("\nThis action CANNOT be undone!")
    print("="*60)
    
    response = input("\nType 'DELETE' to confirm: ").strip()
    return response == 'DELETE'

def main():
    """Main function"""
    print("╔══════════════════════════════════════════════════════════╗")
    print("║       Firestore Cleanup Tool - Zalo Clone               ║")
    print("╚══════════════════════════════════════════════════════════╝")
    
    # Confirm action
    if not confirm_action():
        print("\n❌ Operation cancelled by user")
        sys.exit(0)
    
    # Initialize Firebase
    db = init_firebase()
    
    # Delete data
    print("\n🚀 Starting cleanup...")
    
    try:
        # Delete friend requests
        fr_count = delete_friend_requests(db)
        
        # Delete conversations
        conv_count = delete_conversations(db)
        
        # Summary
        print("\n" + "="*60)
        print("✅ CLEANUP COMPLETED SUCCESSFULLY")
        print("="*60)
        print(f"   Friend requests deleted: {fr_count}")
        print(f"   Conversations deleted:   {conv_count}")
        print("="*60)
        
    except Exception as e:
        print(f"\n❌ Error during cleanup: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
