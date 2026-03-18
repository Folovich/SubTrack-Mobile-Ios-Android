# Mobile

Expo mobile client for SubTrack.
Contains screens for auth, subscriptions and analytics.
Uses shared backend API.

Subscriptions screen supports quick support email flow:
- quick cancel
- quick pause

From generated draft user can:
- open mail client via `Linking.openURL(mailto...)`
- copy email text to clipboard
