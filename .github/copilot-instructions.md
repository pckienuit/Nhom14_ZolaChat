# Copilot Instructions - Zalo Clone Project

## Project Overview
This is a full-stack messaging application (Zalo clone) combining:
- **React frontend** (in `src/`) - chat UI, real-time messaging, social features
- **Android app** (in `app/`) - Kotlin/Java mobile client (Gradle-based)
- Dual environment support: backend integration OR demo mode with mock data

## Architecture & Key Components

### Frontend Structure
- **Layouts**: `MainLayout` (authenticated), `BlankLayout` (auth pages) - wrap all routes
- **Routing**: Custom `PrivateRouter` checks token + Redux state, redirects to `/login` on auth failure
- **State Management**: Redux with thunk middleware, logger enabled globally
- **Real-time**: Socket.io namespaces (`/chat`, `/notification`) with Bearer token in polling headers

### Redux Patterns
- **Store**: `src/redux/store/store.js` - single store with combined reducers
- **Actions**: Thunk-based async actions in `src/redux/actions/index.js`
- **Naming Convention**: `redux{FeatureName}` (e.g., `reduxUserData`, `reduxConversation`)
- **Key States**: 
  - `reduxShowBlur` - controls modal backdrop
  - `reduxUserData` - current user info
  - `reduxConversation` - chat messages
  - `reduxListFriend`, `reduxConfirmFriend`, `reduxRequestAddFriend` - friend system

### API Services Pattern
All services in `src/services/` follow this structure:
```javascript
const DEMO_MODE = true; // Toggle mock vs real API

const apiName = {
  methodName: (params) => {
    if (DEMO_MODE) return Promise.resolve(mockAPI.method(params));
    return axiosClient.get/post/put(url, params);
  }
};
```
- **Demo Mode**: Uses `src/mockServer.js` for standalone development
- **Backend Mode**: Toggle `DEMO_MODE = false` + `USE_BACKEND = true` in `src/setupProxy.js`
- **Base URL**: `/api/v1` (proxied to `localhost:3003` in backend mode)

### Socket.io Integration
Sockets use Bearer auth in transport headers (see `src/App.js`, `src/containers/Room/Room.js`):
```javascript
io("/namespace", {
  transportOptions: {
    polling: { extraHeaders: { Authorization: `Bearer ${token}` }}
  }
});
```
- **Namespaces**: `/chat` (messages), `/notification` (toasts)
- **Room Pattern**: `socket.emit("join", { id })` on conversation enter

### Authentication Flow
1. Token stored in `localStorage.getItem("token")`
2. Token saved to IndexedDB via `src/helpers/CreateIndexDB.js`
3. `PrivateRouter` validates token + dispatches `doCheckLogin()`
4. Failed auth → redirect to `/login`, clear state

## Development Workflows

### Running Demo Mode (No Backend)
```bash
npm start  # React dev server on port 3000
```
Default config: `DEMO_MODE = true`, `USE_BACKEND = false` - uses mock data

### Backend Integration
1. Set `USE_BACKEND = true` in `src/setupProxy.js`
2. Set `DEMO_MODE = false` in service files
3. Ensure backend running on `localhost:3003`
4. Proxy handles `/api` and `/socket` routes

### Android App
```bash
./gradlew build              # Build Android app
./gradlew installDebug       # Install to connected device
```
Config: `app/build.gradle.kts` (minSdk 28, targetSdk 36)

## Project Conventions

### Component Organization
- **Containers**: Page-level components with business logic (`src/containers/`)
- **Components**: Reusable UI elements (`src/components/`)
- **Index Exports**: Each folder has `index.js` for clean imports
- **Colocated Styles**: Component SCSS files next to JSX (e.g., `Nav.js` + `Nav.scss`)

### File Naming
- Components: PascalCase (`HeaderBar.js`)
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
