# Copilot Instructions - Zalo Clone Android App

## Project Overview
This is a native Android messaging application (Zalo clone) built with Java:
- **Android app** (in `app/`) - Native Android client using Material Design
- **Demo mode** - Standalone app with mock data (no backend required yet)
- **Architecture** - Activity/Fragment pattern with RecyclerView adapters

## Architecture & Key Components

### Android Structure
```
app/src/main/java/com/example/doan_zaloclone/
├── models/              # Data models (Conversation, Message, FriendRequest)
├── ui/
│   ├── login/          # LoginActivity, SignUpActivity
│   ├── home/           # HomeFragment, ConversationAdapter
│   ├── contact/        # ContactFragment, FriendRequestAdapter
│   └── room/           # RoomActivity, MessageAdapter
└── MainActivity.java   # Main with BottomNavigationView
```

### Navigation Pattern
- **LoginActivity** - Launcher activity with email/password validation
- **SignUpActivity** - Registration with confirm password
- **MainActivity** - Bottom navigation (Messages/Contacts tabs)
- **HomeFragment** - RecyclerView of conversations → click opens RoomActivity
- **ContactFragment** - Friend requests with Accept/Reject buttons
- **RoomActivity** - Chat screen with sent/received messages

### Activity Lifecycle
- LoginActivity → MainActivity (after successful login)
- MainActivity manages HomeFragment ↔ ContactFragment via BottomNavigationView
- RoomActivity opened via Intent with conversationId and conversationName extras
- Back navigation: RoomActivity → MainActivity, SignUpActivity → LoginActivity

### UI Components Pattern
All lists use **RecyclerView** with custom adapters:
```java
public class ConversationAdapter extends RecyclerView.Adapter<ViewHolder> {
    private List<Conversation> conversations;
    private OnConversationClickListener listener;
    
    // ViewHolder pattern with bind() method
    // updateConversations() for data refresh
}
```

### Demo Mode (Current Implementation)
- Hardcoded data in Fragments/Activities
- No network calls, no backend required
- Login auto-succeeds with any valid email/password (6+ chars)
- 3 mock conversations, 2 mock friend requests
- Messages stored in-memory only

## Development Workflows

### Running Demo Mode (No Backend)
```bash
npm start  # React dev server on port 3000
## Development Workflows

### Running Android App
```bash
# Windows
.\gradlew build              # Build APK
.\gradlew installDebug       # Install to emulator/device
.\gradlew clean              # Clean build cache

# Or use Android Studio
Run → Run 'app'
```

### Project Configuration
- **Min SDK**: 28 (Android 9.0)
- **Target SDK**: 36
## Project Conventions

### File Organization
- **Activities**: PascalCase ending in `Activity` (`LoginActivity.java`)
- **Fragments**: PascalCase ending in `Fragment` (`HomeFragment.java`)
- **Adapters**: PascalCase ending in `Adapter` (`ConversationAdapter.java`)
- **Models**: PascalCase (`Conversation.java`, `Message.java`)
- **Layouts**: snake_case (`activity_login.xml`, `item_conversation.xml`)
- **Resources**: snake_case with type prefix (`ic_send.xml`, `fragment_home.xml`)

### Layout Naming Convention
- Activities: `activity_{name}.xml`
- Fragments: `fragment_{name}.xml`
- List items: `item_{type}.xml`
- Menus: `{name}_menu.xml`

### Android Patterns
**Activity/Fragment Initialization:**
```java
private void initViews() {
    // findViewById() calls
}

private void setupListeners() {
## Critical Integration Points

### AndroidManifest.xml
- **LoginActivity**: Launcher activity (MAIN/LAUNCHER intent filter)
- **MainActivity**: No exported, requires authentication
- **RoomActivity**: Has parentActivityName for proper back navigation
- All activities use `Theme.DoAn_ZaloClone`

### Colors & Theming
- **Primary**: `#0084FF` (Zalo blue)
- **Text Secondary**: `#757575` (gray)
- **Error**: `#F44336` (red for reject buttons)
- **Background**: `#F5F5F5` (light gray)

### ViewBinding
Enabled in `build.gradle.kts`:
```kotlin
buildFeatures {
    viewBinding = true
}
```
Access views type-safe via binding objects.
        // Bind data to views
    }
}

public void updateData(List<Model> newData) {
    this.data = newData;
    notifyDataSetChanged();
}
```
- No backend connection required
- Test all screens: Login → Home → Chat → Contactsgic (`src/containers/`)
- **Components**: Reusable UI elements (`src/components/`)
## Common Pitfalls & Best Practices

### RecyclerView
- Always call `notifyDataSetChanged()` after updating adapter data
- Use `LinearLayoutManager` with `setStackFromEnd(true)` for chat (scroll to bottom)
- Remember to set both adapter AND layout manager

### Fragment Transactions
- Use `getSupportFragmentManager()` in Activities
- Always `commit()` after `beginTransaction()`
- Use `replace()` not `add()` to swap fragments in MainActivity

### Intent Extras
- Pass data via Intent: `intent.putExtra("key", value)`
- Retrieve in target: `getIntent().getStringExtra("key")`
- Used in: HomeFragment → RoomActivity (conversationId, conversationName)

### Back Navigation
- Override `onSupportNavigateUp()` for toolbar back button
- Or use `toolbar.setNavigationOnClickListener(v -> finish())`
- Set `parentActivityName` in manifest for proper back stack

### Demo Data
- Conversations: 3 hardcoded in HomeFragment's `loadConversations()`
- Friend Requests: 2 hardcoded in ContactFragment's `loadFriendRequests()`
- Messages: Initial 3 in RoomActivity, new ones added in-memory
- Replace with API calls when backend ready
- Services: camelCase with `api` prefix (`apiFriends.js`)
- Reducers: camelCase with `reducer/reduce` prefix (`reducerListUser.js`)
- Helpers: PascalCase (`CreateIndexDB.js`)

### Redux Action Pattern
```javascript
export const actionName = (params) => (dispatch) => {
  apiService.method(params)
    .then(res => dispatch({ type: SUCCESS_CONSTANT, playload: res }))
    .catch(err => dispatch({ type: ERROR_CONSTANT, playload: err.response.data }));
};
```
Note: Uses `playload` (typo in codebase) instead of `payload`

## Critical Integration Points

### Mock Server (`src/mockServer.js`)
Provides demo data: users, messages, conversations, friend requests. Methods mirror backend API structure.

### Proxy Configuration (`src/setupProxy.js`)
- WebSocket proxy on `/socket`
- API proxy on `/api`
- `USE_BACKEND` flag controls proxy activation

### Service Worker (`src/serviceWorker.js`)
PWA support with push notifications via `src/helpers/registerPush.js`

## Common Pitfalls
- **Typo**: Redux actions use `playload` not `payload` - maintain consistency
- **Socket Cleanup**: Always `socket.off()` in `useEffect` cleanup to prevent memory leaks
- **Token Sync**: Token in localStorage must match axiosClient headers (set on app init)
- **Demo vs Backend**: Remember to toggle BOTH `DEMO_MODE` in services AND `USE_BACKEND` in proxy
