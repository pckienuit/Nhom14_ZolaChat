#!/usr/bin/env python3
"""
Admin Script - Bulk Import Sticker Packs
========================================

Import multiple sticker packs from a JSON configuration file.

Usage:
    python bulk_import_stickers.py --config packs_config.json

Config Format:
{
  "packs": [
    {
      "name": "Cute Animals",
      "description": "Adorable animal stickers",
      "type": "official",
      "icon": "./icons/animals.png",
      "tags": ["cute", "animal"],
      "stickers": [
        {"file": "./animals/cat1.png", "tags": ["cat"]},
        {"file": "./animals/dog1.png", "tags": ["dog"]}
      ]
    }
  ]
}

Requirements:
    pip install firebase-admin requests pillow
"""

import argparse
import json
import sys
import time
import uuid
from pathlib import Path

import requests
from PIL import Image
import firebase_admin
from firebase_admin import credentials, firestore

# VPS Configuration
VPS_UPLOAD_URL = "https://zolachat.site/api/stickers/upload"
VPS_BASE_URL = "https://zolachat.site/stickers"

# Firebase
FIREBASE_CRED_PATH = "service-account-key.json"

db = None


def init_firebase():
    """Initialize Firebase Admin SDK"""
    global db
    
    if not Path(FIREBASE_CRED_PATH).exists():
        print(f"❌ Error: {FIREBASE_CRED_PATH} not found!")
        print("Download service account key from Firebase Console")
        sys.exit(1)
    
    cred = credentials.Certificate(FIREBASE_CRED_PATH)
    firebase_admin.initialize_app(cred)
    db = firestore.client()


def upload_file(file_path: str) -> str:
    """Upload file to VPS"""
    try:
        with open(file_path, 'rb') as f:
            files = {'sticker': f}
            data = {'userId': 'admin'}
            response = requests.post(VPS_UPLOAD_URL, files=files, data=data, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            if result.get('success'):
                return result.get('url', f"{VPS_BASE_URL}/{Path(file_path).name}")
        
        return None
    except Exception as e:
        print(f"  Upload error: {e}")
        return None


def create_thumbnail(image_path: str) -> str:
    """Create and upload thumbnail"""
    img = Image.open(image_path)
    img.thumbnail((128, 128), Image.Resampling.LANCZOS)
    
    thumb_dir = Path("./temp")
    thumb_dir.mkdir(exist_ok=True)
    thumb_path = thumb_dir / f"thumb_{Path(image_path).name}"
    
    img.save(thumb_path, format=img.format or 'PNG')
    thumb_url = upload_file(str(thumb_path))
    thumb_path.unlink()  # Delete temp file
    
    return thumb_url


def import_pack(pack_config: dict, base_path: Path) -> bool:
    """Import single pack"""
    
    print(f"\n📦 Importing: {pack_config['name']}")
    print("=" * 60)
    
    pack_id = f"pack_{uuid.uuid4().hex[:8]}"
    
    # Upload icon
    print("Uploading icon...", end=" ")
    icon_path = base_path / pack_config['icon']
    icon_url = upload_file(str(icon_path))
    if not icon_url:
        print("✗ Failed")
        return False
    print(f"✓")
    
    # Upload stickers
    print(f"Uploading {len(pack_config['stickers'])} stickers...")
    sticker_docs = []
    
    for idx, sticker_config in enumerate(pack_config['stickers'], 1):
        sticker_path = base_path / sticker_config['file']
        print(f"  [{idx}/{len(pack_config['stickers'])}] {sticker_path.name}...", end=" ")
        
        # Upload main
        sticker_url = upload_file(str(sticker_path))
        if not sticker_url:
            print("✗")
            continue
        
        # Thumbnail
        thumb_url = create_thumbnail(str(sticker_path))
        if not thumb_url:
            thumb_url = sticker_url
        
        sticker_id = f"sticker_{uuid.uuid4().hex[:8]}"
        sticker_docs.append({
            'id': sticker_id,
            'packId': pack_id,
            'imageUrl': sticker_url,
            'thumbnailUrl': thumb_url,
            'isAnimated': sticker_path.suffix.lower() in ['.webp', '.gif'],
            'format': sticker_path.suffix[1:].upper(),
            'creatorId': 'admin',
            'tags': pack_config.get('tags', []) + sticker_config.get('tags', []),
            'createdAt': int(time.time() * 1000)
        })
        
        print("✓")
    
    if not sticker_docs:
        print("❌ No stickers uploaded!")
        return False
    
    # Create pack document
    print("Creating Firestore documents...", end=" ")
    
    pack_doc = {
        'id': pack_id,
        'name': pack_config['name'],
        'description': pack_config['description'],
        'type': pack_config.get('type', 'official'),
        'iconUrl': icon_url,
        'creatorId': 'admin',
        'stickerCount': len(sticker_docs),
        'downloadCount': 0,
        'isPublished': True,
        'isFree': pack_config.get('isFree', True),
        'price': pack_config.get('price', 0),
        'createdAt': int(time.time() * 1000),
        'updatedAt': int(time.time() * 1000)
    }
    
    try:
        # Create pack
        db.collection('stickerPacks').document(pack_id).set(pack_doc)
        
        # Create stickers
        for sticker in sticker_docs:
            db.collection('stickerPacks').document(pack_id).collection('stickers').document(sticker['id']).set(sticker)
        
        print("✓")
        print(f"✅ Pack '{pack_config['name']}' imported successfully! (ID: {pack_id})")
        return True
        
    except Exception as e:
        print(f"✗ Error: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(description='Bulk import sticker packs')
    parser.add_argument('--config', required=True, help='Path to JSON config file')
    args = parser.parse_args()
    
    # Load config
    config_path = Path(args.config)
    if not config_path.exists():
        print(f"❌ Config file not found: {args.config}")
        sys.exit(1)
    
    with open(config_path, 'r', encoding='utf-8') as f:
        config = json.load(f)
    
    # Initialize Firebase
    init_firebase()
    
    # Import packs
    base_path = config_path.parent
    packs = config.get('packs', [])
    
    print(f"\n🚀 Starting bulk import of {len(packs)} packs...")
    print("=" * 60)
    
    success_count = 0
    for pack in packs:
        if import_pack(pack, base_path):
            success_count += 1
    
    # Summary
    print("\n" + "=" * 60)
    print(f"✅ Import complete!")
    print(f"   Successful: {success_count}/{len(packs)}")
    print("=" * 60 + "\n")


if __name__ == '__main__':
    main()
