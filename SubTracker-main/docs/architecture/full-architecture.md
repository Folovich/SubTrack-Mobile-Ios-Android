# Full Architecture - Subscription Manager

## Modules
- auth
- user
- subscription
- category
- importing
- integration
- analytics
- forecast
- notification
- recommendation
- common

## Data Layer
Flyway migration `V2__full_architecture_tables.sql` defines full database schema for all required features in the technical assignment.

## API Groups
- `/auth/*`
- `/api/v1/users/*`
- `/api/v1/subscriptions/*`
- `/api/v1/categories/*`
- `/api/v1/imports/*`
- `/api/v1/integrations/*`
- `/api/v1/analytics/*`
- `/api/v1/forecast`
- `/api/v1/notifications/*`
- `/api/v1/recommendations/*`
