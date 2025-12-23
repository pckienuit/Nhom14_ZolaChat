#!/usr/bin/env python3
"""
Admin Script - Upload Sticker Pack
===================================

Upload batch stickers to VPS and create pack in Firestore.

Usage:
    python admin_upload_sticker_pack.py \
      --pack-name "Cute Animals" \
      --description "Adorable animal stickers" \
      --type official \
      --stickers-dir ./sticker_images/ \
      --icon ./pack_icon.png

Requirements:
    pip install firebase-admin requests pillow
"""

import argparse
import os
import sys
import time
import uuid
from pathlib import Path
from typing import List, Dict

import requests
from PIL import Image
import firebase_admin
from firebase_admin import credentials, firestore

# VPS Configuration
VPS_UPLOAD_URL = "http://163.61.182.20/api/stickers/upload"
VPS_BASE_URL = "http://163.61.182.20/stickers"

# Firebase Admin SDK
# TODO: Download service account key from Firebase Console
# Place it at: ./service-account-key.json
FIREBASE_CRED_PATH = "service-account-key.json"


def init_firebase():
    """Initialize Firebase Admin SDK"""
    if not os.path.exists(FIREBASE_CRED_PATH):
        print(f"❌ Error: {FIREBASE_CRED_PATH} not found!")
        print("Download service account key from Firebase Console:")
        print("  1. Go to Project Settings > Service Accounts")
        print("  2. Click 'Generate New Private Key'")
        print("  3. Save as 'service-account-key.json' in this directory")
        sys.exit(1)
    
    cred = credentials.Certificate(FIREBASE_CRED_PATH)
    firebase_admin.initialize_app(cred)
    return firestore.client()


def upload_image_to_vps(image_path: str, user_id: str = "admin") -> str:
    """Upload single image to VPS"""
    
    print(f"  Uploading {Path(image_path).name}...", end=" ")
    
    try:
        with open(image_path, 'rb') as f:
            files = {'sticker': f}
            data = {'userId': user_id}
            response = requests.post(VPS_UPLOAD_URL, files=files, data=data, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            if result.get('success'):
                url = result.get('url', f"{VPS_BASE_URL}/{Path(image_path).name}")
                print(f"✓ {url}")
                return url
        
        print(f"✗ Failed (Status {response.status_code})")
        return None
        
    except Exception as e:
        print(f"✗ Error: {e}")
        return None


def create_thumbnail(image_path: str, size: tuple = (128, 128)) -> str:
    """Create thumbnail and save to temp directory"""
    
    img = Image.open(image_path)
    img.thumbnail(size, Image.Resampling.LANCZOS)
    
    # Save to temp directory
    thumb_dir = Path("./temp_thumbnails")
    thumb_dir.mkdir(exist_ok=True)
    
    thumb_path = thumb_dir / f"thumb_{Path(image_path).name}"
    img.save(thumb_path, format=img.format or 'PNG')
    
    return str(thumb_path)


def upload_sticker_pack(
    pack_name: str,
    description: str,
    pack_type: str,
    stickers_dir: str,
    icon_path: str,
    tags: List[str] = None,
    is_free: bool = True
):
    """Upload entire sticker pack"""
    
    print(f"\n{'='*70}")
    print(f"📦 Uploading Sticker Pack: {pack_name}")
    print(f"{'='*70}\n")
    
    # Initialize Firebase
    db = init_firebase()
    
    # Generate pack ID
    pack_id = f"pack_{uuid.uuid4().hex[:8]}"
    print(f"Pack ID: {pack_id}")
    
    # Step 1: Upload icon
    print(f"\n[1/4] Uploading pack icon...")
    icon_url = upload_image_to_vps(icon_path, "admin")
    if not icon_url:
        print("❌ Failed to upload icon!")
        return False
    
    # Step 2: Upload stickers
    print(f"\n[2/4] Uploading stickers...")
    sticker_paths = list(Path(stickers_dir).glob("*"))
    sticker_paths = [p for p in sticker_paths if p.suffix.lower() in ['.png', '.jpg', '.jpeg', '.webp', '.gif']]
    
    print(f"Found {len(sticker_paths)} stickers")
    
    sticker_data = []
    for idx, sticker_path in enumerate(sticker_paths, 1):
        print(f"\n  [{idx}/{len(sticker_paths)}] Processing {sticker_path.name}")
        
        # Upload main image
        sticker_url = upload_image_to_vps(str(sticker_path), "admin")
        if not sticker_url:
            continue
        
        # Create and upload thumbnail
        print(f"  Creating thumbnail...", end=" ")
        try:
            thumb_path = create_thumbnail(str(sticker_path))
            thumb_url = upload_image_to_vps(thumb_path, "admin")
            os.remove(thumb_path)  # Clean up
            print("✓")
        except Exception as e:
            print(f"✗ {e}")
            thumb_url = sticker_url  # Fallback to main image
        
        # Prepare sticker data
        sticker_id = f"sticker_{uuid.uuid4().hex[:8]}"
        sticker_data.append({
            'id': sticker_id,
            'packId': pack_id,
            'imageUrl': sticker_url,
            'thumbnailUrl': thumb_url,
            'isAnimated': sticker_path.suffix.lower() in ['.webp', '.gif'],
            'format': sticker_path.suffix[1:].upper(),
            'creatorId': 'admin',
            'tags': tags or [],
            'createdAt': int(time.time() * 1000)
        })
    
    if not sticker_data:
        print("\n❌ No stickers uploaded successfully!")
        return False
    
    print(f"\n✅ Successfully uploaded {len(sticker_data)} stickers")
    
    # Step 3: Create pack document in Firestore
    print(f"\n[3/4] Creating pack in Firestore...")
    
    pack_doc = {
        'id': pack_id,
        'name': pack_name,
        'description': description,
        'type': pack_type,
        'iconUrl': icon_url,
        'creatorId': 'admin',
        'stickerCount': len(sticker_data),
        'downloadCount': 0,
        'isPublished': True,
        'isFree': is_free,
        'price': 0 if is_free else 50000,  # Default price for paid packs
        'createdAt': int(time.time() * 1000),
        'updatedAt': int(time.time() * 1000)
    }
    
    try:
        db.collection('stickerPacks').document(pack_id).set(pack_doc)
        print(f"  ✓ Pack document created")
    except Exception as e:
        print(f"  ✗ Error: {e}")
        return False
    
    # Step 4: Create sticker documents
    print(f"\n[4/4] Creating sticker documents...")
    
    success_count = 0
    for sticker in sticker_data:
        try:
            db.collection('stickerPacks').document(pack_id).collection('stickers').document(sticker['id']).set(sticker)
            success_count += 1
        except Exception as e:
            print(f"  ✗ Failed to create {sticker['id']}: {e}")
    
    print(f"  ✓ Created {success_count}/{len(sticker_data)} sticker documents")
    
    # Summary
    print(f"\n{'='*70}")
    print(f"✅ UPLOAD COMPLETE!")
    print(f"{'='*70}")
    print(f"Pack ID: {pack_id}")
    print(f"Pack Name: {pack_name}")
    print(f"Stickers: {len(sticker_data)}")
    print(f"Icon URL: {icon_url}")
    print(f"\nUsers can now download this pack from the Sticker Store!")
    print(f"{'='*70}\n")
    
    return True


def main():
    parser = argparse.ArgumentParser(description='Upload sticker pack to VPS and Firestore')
    parser.add_argument('--pack-name', required=True, help='Name of the sticker pack')
    parser.add_argument('--description', required=True, help='Pack description')
    parser.add_argument('--type', choices=['official', 'user', 'trending'], default='official', help='Pack type')
    parser.add_argument('--stickers-dir', required=True, help='Directory containing sticker images')
    parser.add_argument('--icon', required=True, help='Path to pack icon (512x512 recommended)')
    parser.add_argument('--tags', help='Comma-separated tags (e.g., "cute,animal,funny")')
    parser.add_argument('--paid', action='store_true', help='Make this a paid pack')
    
    args = parser.parse_args()
    
    # Validate inputs
    if not os.path.isdir(args.stickers_dir):
        print(f"❌ Error: Stickers directory not found: {args.stickers_dir}")
        sys.exit(1)
    
    if not os.path.isfile(args.icon):
        print(f"❌ Error: Icon file not found: {args.icon}")
        sys.exit(1)
    
    tags = args.tags.split(',') if args.tags else []
    
    # Upload pack
    success = upload_sticker_pack(
        pack_name=args.pack_name,
        description=args.description,
        pack_type=args.type,
        stickers_dir=args.stickers_dir,
        icon_path=args.icon,
        tags=tags,
        is_free=not args.paid
    )
    
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
