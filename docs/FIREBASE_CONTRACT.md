# CineStream Ecosystem - Unified Firebase Contract

This document defines the strict, authoritative, and unified Firebase Contract shared between:
1. **CineStream User App** (Android Client)
2. **CineStream Admin Dashboard** (Admin Portal)

Both applications connect to the same Firebase project and must adhere to the exact paths, document structures, field names, data types, and ownership rules defined below.

---

## 1. Unified Collection Paths & Schemas

### 1.1 Users Collection: `/users/{uid}`
- **Document ID**: Equal to the Firebase Authentication UID (`request.auth.uid`). Random IDs are strictly prohibited.
- **Data Ownership**:
  - **User App owns**: `displayName`, `photoUrl`, `lastLoginAt`, `updatedAt`, `appVersion`, user-specific subcollections (e.g., watch history, favorites, device tokens).
  - **Admin App owns**: `role`, `isActive`, `isPremium`, `subscriptionTier`, `subscriptionExpiresAt`, permission flags (`canWatch`, `canDownload`, `canChat`, `canStory`, `canP2P`), ban flags (`watchBan`, `downloadBan`, `chatBan`, `storyBan`, `p2pBan`), device limits (`deviceLimit`), DRM overrides (`offlineDaysOverride`, `forcedAdsOverride`).
  - **Partial Updates Rule**: Admin App must NEVER use un-merged `set()` on `/users/{uid}`. It must strictly use `update()` or `set(..., SetOptions.merge())` to prevent accidental deletion of user-owned state.

#### Field Specifications:
| Field Name | Type | Default | Description |
|---|---|---|---|
| `uid` | String | - | User Auth UID (matches doc ID) |
| `email` | String | "" | Account email address |
| `displayName` | String | "" | User public display name |
| `photoUrl` | String | "" | Avatar URL |
| `username` | String | "" | Normalized username handle |
| `createdAt` | Long / Timestamp | Server timestamp | Account creation epoch millis |
| `updatedAt` | Long / Timestamp | Server timestamp | Last update epoch millis |
| `lastLoginAt` | Long / Timestamp | Server timestamp | Last sign-in epoch millis |
| `isActive` | Boolean | `true` | Account active state |
| `isPremium` | Boolean | `false` | Premium PRO subscription flag |
| `subscriptionTier` | String | `"free"` | `"free"`, `"pro"`, `"vip"` |
| `subscriptionExpiresAt` | Long? | `null` | Expiration epoch millis (if applicable) |
| `role` | String | `"user"` | `"user"`, `"moderator"`, `"admin"`, `"superadmin"` |
| `canWatch` | Boolean | `true` | Permission to stream/play media |
| `canDownload` | Boolean | `true` | Permission to download media for offline |
| `canChat` | Boolean | `true` | Permission to use chat / community |
| `canStory` | Boolean | `true` | Permission to view/post stories |
| `canP2P` | Boolean | `true` | Permission to use P2P sharing |
| `watchBan` | Boolean | `false` | Explicit streaming restriction |
| `downloadBan` | Boolean | `false` | Explicit download restriction |
| `chatBan` | Boolean | `false` | Explicit chat restriction |
| `storyBan` | Boolean | `false` | Explicit story restriction |
| `p2pBan` | Boolean | `false` | Explicit P2P restriction |
| `deviceLimit` | Int | `2` | Maximum concurrent active devices |
| `offlineDaysOverride` | Int? | `null` | Custom DRM offline cache days override |
| `forcedAdsOverride` | Int? | `null` | Custom ad interval override |
| `appVersion` | String | `""` | User client build version |

---

### 1.2 Administrators Collection: `/admins/{uid}`
- **Document ID**: Equal to the admin's Firebase Auth UID.
- **Purpose**: Authoritative lookup for administrator privileges. Security rules check `exists(/databases/$(database)/documents/admins/$(request.auth.uid))` with `resource.data.enabled == true`.
- **Field Specifications**:
  - `uid`: String
  - `email`: String
  - `role`: String (`"admin"`, `"superadmin"`)
  - `enabled`: Boolean (`true`)
  - `createdAt`: Long / Timestamp

---

### 1.3 System Configuration & OTA: `/config/app`
- **Document Path**: `/config/app` (Single canonical document).
- **Prohibited Paths**: Do NOT create `/system_config/app_config` or `/system_config/app_updates`. All config and updates reside in `/config/app`.
- **Data Ownership**: Admin App writes; User App reads (read-only in rules).

#### Field Specifications:
| Field Name | Type | Default | Description |
|---|---|---|---|
| `maintenanceEnabled` | Boolean | `false` | Global maintenance toggle (NOT `maintenanceMode`) |
| `maintenanceTitle` | String | `"Under Scheduled Maintenance"` | Title displayed on maintenance banner |
| `maintenanceMessage` | String | - | Explanatory message for users |
| `minimumVersionCode` | Int | `1` | Minimum supported app version code (forced upgrade cutoff) |
| `latestVersionCode` | Int | `1` | Current release version code for OTA |
| `latestVersionName` | String | `"1.0.0"` | Current release semantic version string |
| `apkUrl` | String | `""` | Direct download link for the latest APK |
| `apkSha256` | String | `""` | SHA-256 integrity hash of the update APK |
| `mandatoryUpdate` | Boolean | `false` | If true, user cannot dismiss update prompt |
| `releaseNotes` | String | `""` | Markdown or text release notes |
| `providersJson` | String | `"{}"` | Dynamic JSON catalog of media providers and scrapers |
| `defaultOfflineDays` | Int | `2` | Default days offline video licenses remain valid |
| `defaultForcedAds` | Int | `5` | Default number of media starts between interstitial ads |
| `updatedAt` | Long / Timestamp | Server timestamp | Timestamp of last configuration publish |

---

### 1.4 Notifications Collection: `/notifications/{notificationId}`
- **Document Path**: `/notifications/{notificationId}`
- **Data Ownership**: Admin App writes; User App reads.
- **FCM Architecture**:
  - Writing to `/notifications` creates an in-app announcement record.
  - For real-time system push notifications (FCM), a server-side Firebase Cloud Function triggers on `document.onCreate("/notifications/{id}")` and calls `admin.messaging().sendEachForMulticast()`. The Android client itself does NOT pretend client-write triggers push without backend dispatch.

#### Field Specifications:
| Field Name | Type | Allowed Values | Description |
|---|---|---|---|
| `id` | String | - | Document ID |
| `title` | String | Non-empty | Notification headline |
| `body` | String | Non-empty | Notification content body (canonical name; `message` supported as alias) |
| `type` | String | `"SYSTEM"`, `"ANNOUNCEMENT"`, `"ALERT"`, `"PROMO"` | Category of notification |
| `target` | String | `"ALL"`, `"PRO"`, `"UID"` | Delivery audience |
| `targetUid` | String | "" | Recipient UID when `target == "UID"` |
| `createdBy` | String | UID of admin | Admin author ID |
| `createdAt` | Long / Timestamp | Server timestamp | Creation timestamp |
| `expiresAt` | Long? | `null` | Optional expiration date |
| `isActive` | Boolean | `true` | Active display status |
| `status` | String | `"PENDING"`, `"SENT"` | Processing status |

---

### 1.5 Extensions Collection: `/extensions/{extensionId}`
- **Document Path**: `/extensions/{extensionId}`
- **Data Ownership**: Admin App writes; User App reads.

#### Field Specifications:
| Field Name | Type | Description |
|---|---|---|
| `id` | String | Unique extension ID / doc ID |
| `name` | String | Extension display name |
| `packageName` | String | Android package identifier |
| `versionCode` | Int | Version integer |
| `versionName` | String | Version string (e.g., "1.2.0") |
| `apkUrl` | String | Download URL for the provider APK |
| `apkSha256` | String | SHA-256 cryptographic checksum (canonical name; `sha256` supported as alias) |
| `minAppVersionCode` | Int | Minimum CineStream app version required to load extension |
| `enabled` | Boolean | Whether extension is active and downloadable |
| `mandatory` | Boolean | Whether extension is required for core streaming |
| `releaseNotes` | String | Changelog notes |
| `createdAt` | Long / Timestamp | Creation epoch millis |
| `updatedAt` | Long / Timestamp | Update epoch millis |

---

### 1.6 Audit Logs Collection: `/auditLogs/{logId}`
- **Document Path**: `/auditLogs/{logId}` (Canonical camelCase. `/audit_logs` is strictly prohibited).
- **Data Ownership**: Admin App appends; only Admins can read.

#### Field Specifications:
| Field Name | Type | Description |
|---|---|---|
| `id` | String | Auto-generated document ID |
| `actorUid` | String | UID of admin performing the action (canonical name; `adminUid` supported as alias) |
| `adminEmail` | String | Email of admin |
| `action` | String | Operation code (e.g. `UPDATE_USER`, `GRANT_ADMIN`, `UPDATE_APP_CONFIG`, `RESOLVE_REPORT`) |
| `targetType` | String | Entity category (`"USER"`, `"CONFIG"`, `"NOTIFICATION"`, `"EXTENSION"`, `"REPORT"`, `"AUTH"`) |
| `targetId` | String | ID of affected entity |
| `details` | String | Human-readable change summary |
| `createdAt` | Long / Timestamp | Action timestamp |

---

### 1.7 User Reports Collection: `/reports/{reportId}`
- **Document Path**: `/reports/{reportId}`
- **Data Ownership**:
  - **User App**: Creates issue and bug reports; reads own submitted reports.
  - **Admin App**: Reads all reports; updates `status` (`"PENDING"`, `"RESOLVED"`, `"DISMISSED"`), `resolutionNotes`, and resolves/deletes reports.

#### Field Specifications:
| Field Name | Type | Description |
|---|---|---|
| `id` | String | Document ID |
| `userId` | String | UID of reporting user |
| `userEmail` | String | Contact email of reporting user |
| `type` | String | Issue category (`"ISSUE"`, `"STREAM"`, `"BUG"`, `"CONTENT"`) |
| `targetType` | String | Target category (`"STREAM"`, `"MEDIA"`, `"EXTENSION"`, `"USER"`) |
| `targetId` | String | ID of media, provider, or item reported |
| `title` | String | Short issue summary |
| `description` | String | Detailed description of the malfunction |
| `status` | String | Status (`"PENDING"`, `"RESOLVED"`, `"DISMISSED"`) |
| `resolutionNotes` | String | Admin notes regarding the fix or outcome |
| `createdAt` | Long / Timestamp | Submission timestamp |
| `resolvedAt` | Long? / Timestamp? | Resolution timestamp (null while pending) |
| `resolvedBy` | String | Admin UID who handled the report |

---

## 2. Media Storage & Delivery Architecture (Cloudinary)

1. **Approved Media Service**:
   - **Cloudinary** is the authoritative media storage and CDN delivery service for CineStream.
   - **Firebase Storage is NOT USED** for CineStream media assets (posters, backgrounds, thumbnails, avatars).
2. **Firestore Stores Only References**:
   - Binary media files must NEVER be stored in Firestore documents.
   - Firestore stores only the media URL (e.g. `https://res.cloudinary.com/cinestream/image/upload/...`), Cloudinary `public_id`, dimensions, and format.
3. **Client-Side Security Policy (CRITICAL)**:
   - Client applications (User App & Admin Dashboard) MUST NOT contain or expose the Cloudinary **API Secret**.
   - Client-side uploads are executed exclusively using **Unsigned Upload Presets** (`cinestream_unsigned`) targeting safe destination folders.
   - Dynamic transformation URLs (resizing, WebP/AVIF auto-formatting, quality optimization) are constructed via standard Cloudinary delivery URL conventions (`/upload/f_auto,q_auto,w_...,h_.../`).

---

## 3. Authentication & Admin Authorization
1. **Cold Start Protection**:
   - Checking `currentUser != null` is NOT sufficient to access the Admin Dashboard.
   - The app must route to the authentication verification barrier where `checkIsAdmin(user)` confirms membership in `/admins/{uid}` with `enabled == true` (or superadmin bootstrap email).
   - If admin verification fails, the user session is immediately terminated with `auth.signOut()` and access is refused.
2. **User Pre-provisioning**:
   - In standard client-side Android, creating a Firestore record in `/users/{uid}` does not register a Firebase Auth password account (which requires Firebase Admin SDK or Cloud Function).
   - When Admin creates a user profile, it documents that the profile will be bound to the user's account upon their authentication with that email/UID.
