#!/usr/bin/env python3
"""
Firebase Auth to Firestore Sync
Syncs all users from Firebase Authentication to Firestore users collection
"""

import firebase_admin
from firebase_admin import credentials, auth, firestore
import sys

SERVICE_ACCOUNT_KEY = "serviceAccountKey.json"


class AuthToFirestoreSync:
    def __init__(self, service_account_path):
        print("Initializing Firebase Admin SDK...")
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred)
        self.db = firestore.client()
        print("Initialized.\n")

    def clear_firestore_users(self):
        """Delete all users from Firestore users collection"""
        print("Clearing all users from Firestore...")
        users_ref = self.db.collection("users")
        docs = users_ref.stream()
        
        deleted_count = 0
        for doc in docs:
            doc.reference.delete()
            deleted_count += 1
        
        print(f"Deleted {deleted_count} users from Firestore\n")
        return deleted_count

    def get_all_auth_users(self):
        """Get all users from Firebase Authentication"""
        print("Fetching users from Firebase Authentication...")
        users = []
        page = auth.list_users()
        
        while page:
            for user in page.users:
                users.append(user)
            # Get next page
            page = page.get_next_page()
        
        print(f"Found {len(users)} users in Authentication\n")
        return users

    def sync_user_to_firestore(self, auth_user):
        """Sync a single user to Firestore"""
        user_id = auth_user.uid
        email = auth_user.email if auth_user.email else ""
        display_name = auth_user.display_name if auth_user.display_name else email.split('@')[0]
        
        # Normalize email to lowercase
        normalized_email = email.lower() if email else ""
        
        # Check if user already exists in Firestore
        user_ref = self.db.collection("users").document(user_id)
        doc = user_ref.get()
        
        user_data = {
            "userId": user_id,
            "id": user_id,  # For compatibility with User model
            "name": display_name,
            "email": normalized_email,
            "avatarUrl": "",
            "devices": {}
        }
        
        if doc.exists:
            # Update existing user (preserve devices)
            existing_data = doc.to_dict()
            if "devices" in existing_data:
                user_data["devices"] = existing_data["devices"]
            
            user_ref.update({
                "name": display_name,
                "email": normalized_email,
                "userId": user_id,
                "id": user_id
            })
            return "updated"
        else:
            # Create new user
            user_data["createdAt"] = firestore.SERVER_TIMESTAMP
            user_ref.set(user_data)
            return "created"

    def run(self, clear_existing=False):
        print("=" * 60)
        print("Firebase Auth to Firestore Sync")
        print("=" * 60)
        print()
        
        # Clear existing users if requested
        if clear_existing:
            self.clear_firestore_users()
        
        # Get all Authentication users
        auth_users = self.get_all_auth_users()
        
        if not auth_users:
            print("No users found in Authentication.")
            return
        
        print("Syncing users to Firestore...")
        print("-" * 60)
        
        created_count = 0
        updated_count = 0
        error_count = 0
        
        for auth_user in auth_users:
            try:
                email = auth_user.email if auth_user.email else "No email"
                name = auth_user.display_name if auth_user.display_name else "No name"
                
                result = self.sync_user_to_firestore(auth_user)
                
                if result == "created":
                    created_count += 1
                    print(f"✓ Created: {name} ({email})")
                elif result == "updated":
                    updated_count += 1
                    print(f"✓ Updated: {name} ({email})")
                    
            except Exception as e:
                error_count += 1
                print(f"✗ Error syncing {auth_user.uid}: {str(e)}")
        
        print("-" * 60)
        print(f"\nSync Summary:")
        print(f"  Created: {created_count} users")
        print(f"  Updated: {updated_count} users")
        print(f"  Errors:  {error_count} users")
        print(f"  Total:   {len(auth_users)} users")
        print()
        print("=" * 60)
        print("Sync completed!")
        print("=" * 60)


def main():
    import os
    
    if not os.path.exists(SERVICE_ACCOUNT_KEY):
        print("ERROR: serviceAccountKey.json not found")
        print("\nGet it from:")
        print("Firebase Console > Project Settings > Service Accounts")
        print("> Generate new private key\n")
        sys.exit(1)
    
    print("=" * 60)
    print("Firebase Auth to Firestore Sync")
    print("=" * 60)
    print()
    print("This script will sync all users from Authentication to Firestore.")
    print("Existing users in Firestore will be updated with latest info.")
    print()
    
    # Ask about clearing existing users
    clear_choice = input("Clear all existing users in Firestore before sync? (yes/no): ").strip().lower()
    clear_existing = clear_choice == 'yes'
    
    if clear_existing:
        print("\n⚠️  WARNING: This will DELETE all users in Firestore!")
        print("   (Authentication users will NOT be affected)")
        confirm = input("Are you sure? Type 'DELETE' to confirm: ").strip()
        if confirm != 'DELETE':
            print("Sync canceled.")
            sys.exit(0)
    
    print()
    syncer = AuthToFirestoreSync(SERVICE_ACCOUNT_KEY)
    syncer.run(clear_existing=clear_existing)


if __name__ == "__main__":
    main()
