# Server requirements and live health checks

| Service | Requirement | Verification | Start/recovery |
|---|---|---|---|
| Core model catalogue/UI | Required | Open app and verify catalogue/local workflows | No backend unless an active feature calls one. |
| Meshy API | Required only for Meshy-powered cloud 3D generation | Valid authenticated request, successful job state and returned asset download | Configure Meshy key securely; verify account/quota and internet. Never commit the key. |
| Gemini API | Optional/feature-specific | Valid key and real supported model request | Configure via secure settings or secret storage. |
| Remote asset URL | Required only for remote generated assets | Download returned asset and validate file | Use the actual provider-returned URL; do not invent endpoints. |

## Health states
Separate provider reachable from generation working. HEALTHY requires an accepted request and successful job/result. Also report DEGRADED, UNREACHABLE, AUTH REQUIRED, QUOTA/BILLING, NOT CONFIGURED and UNVERIFIED. Show check time, safe endpoint, error and recovery steps.

## Startup
Cloud services require internet and valid credentials, not a local Termux server. Run a small real generation, poll the real job state and validate the resulting 3D asset.

## Verification boundary
No live Meshy/Gemini key or working account is claimed to be configured.
