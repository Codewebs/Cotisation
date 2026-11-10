# Supabase setup

The migration in `migrations/20261104115400_create_cotisation_schema.sql` creates
a fresh PostgreSQL schema for the current Sequelize business models. It does
not copy data from MySQL.

## Apply the schema

Create a Supabase project, open **SQL Editor → New query**, and run the files in
`migrations` in timestamp order. Start with
`20261104115400_create_cotisation_schema.sql` for the fresh schema, then run
`20261105095600_create_tontine_with_initiator.sql`. Do not copy only the table
definitions: the migrations configure RLS, grants, RPCs, Realtime and Storage.
The project must have Supabase Auth and Storage enabled. Storage already
enables RLS on its managed `storage.objects` table; the migrations add
bucket-specific policies without altering that managed table. The schema
migration:

- creates `profiles` linked to `auth.users` and the tontine/finance tables;
- enables row-level security and grants access only to the `authenticated` role;
- adds `join_requests` and `notifications` to the Realtime publication;
- creates a private `comment-images` bucket (5 MiB maximum, images only).
- provides authenticated `search_tontines`, `get_tontine_invite`, and
  `join_tontine_with_invite` RPCs for discovery and invitation flows;
- provides administrator-checked RPCs for approving join requests, broadcasting
  reports, approving and repaying loans, distributing a pot, and paying fines.
  These keep each multi-table financial action atomic.

The follow-up migration adds authenticated RPCs that create a tontine and its
initiator membership in one transaction, and let the first user claim an
existing empty "Les amis du dimanche" meeting as initiator (creating its simple
redistribution caisse if needed). It also exposes the creator id in search
results so the initiator can configure the meeting. Run it on existing projects
after the base schema migration; the client relies on these RPCs instead of
inserting into `tontines` directly.

Run `20261105163700_register_push_devices.sql` after the other migrations to
enable authenticated Android clients to register their FCM tokens in
`user_devices`. Keep Firebase service-account credentials out of Android and
store them only in Supabase Edge Function secrets when configuring push delivery.
Apply `20261105201000_group_chat_and_join_notifications.sql` after
`20261105170000_notify_chat_and_invite_events.sql` to attach sender and meeting
metadata used to group chat and join-request notifications. Apply
`20261106143000_notify_cotisation_recorded.sql` to notify a member when their
cotisation is recorded.

## Push notifications

Deploy `functions/send-push` as a Supabase Edge Function. Configure these
function secrets:

- `FIREBASE_SERVICE_ACCOUNT`: the full Firebase service-account JSON (never
  commit or send this file to clients);
- `PUSH_WEBHOOK_SECRET`: a long random secret used to authenticate database
  webhooks.

Also set up a Database Webhook on `public.notifications`, for `INSERT` events,
calling the deployed function URL with an `x-push-webhook-secret` header whose
value is `PUSH_WEBHOOK_SECRET`. The function resolves registered tokens and
sends `CHAT_MESSAGE`, `JOIN_REQUEST`, `TONTINE_SETTINGS`, `CAISSE_SETTINGS`,
`CAISSE_MEMBERSHIP`, and `COTISATION_RECEIPT` notifications through Firebase
Cloud Messaging HTTP v1.

After applying `20261105170000_notify_chat_and_invite_events.sql` and
`20261105201000_group_chat_and_join_notifications.sql`, chat messages create
notifications for the other members. Join requests generated from an invitation
notify the tontine administrators; accepting a request notifies the requester.
The Edge Function sends high-priority data-only FCM messages so Android can
update one grouped notification per sender and meeting while the app is in the
background. Cotisation receipts are displayed as individual Android
notifications. The Android notification channel uses the device's default
notification sound; users can change or silence it in Android notification
settings. Redeploy `functions/send-push` after updating it. Invite links are
shareable URLs and do not identify their recipient before they are opened, so a
push cannot be sent at link-share time without adding an explicit recipient to
the invitation flow.

Enable email/password sign-up in **Authentication → Providers → Email**. If
email confirmation is enabled, users must confirm their address before they can
sign in. The profile trigger copies the auth user's name and phone metadata into
`profiles`.

Configure the project's allowed redirect URLs for web and Android email
confirmation flows in **Authentication → URL Configuration**.

## Troubleshooting: `PGRST205` / missing `public.caisses` schema-cache entry

`Could not find the table 'public.caisses' in the schema cache` (`PGRST205`)
means the Supabase project receiving the REST request does not currently expose
`public.caisses` to PostgREST. The migration file in this repository does not
create tables until it is run in the correct Supabase project.

1. In the Supabase dashboard, confirm the project's URL matches the Android
   `SUPABASE_URL` and web `server/public/js/supabase-config.js`.
2. In that project's SQL Editor, check whether the table exists:

   ```sql
   SELECT to_regclass('public.caisses') AS caisses_table;
   ```

   A result of `public.caisses` means the table exists. `NULL` means the
   migration has not created it in this project.
3. If it is `NULL` and this is the new, empty project, run the complete
   migration file above. The target database in this migration is intentionally
   fresh; it is not an incremental script for an existing partial schema.
4. If the table exists but REST still reports a missing schema-cache entry,
   run this in the SQL Editor and retry after a few seconds:

   ```sql
   NOTIFY pgrst, 'reload schema';
   ```

5. In **Project Settings → API → Exposed schemas**, ensure `public` is exposed.

The migration grants authenticated access to `caisses` and applies RLS. Do not
work around this error by disabling RLS or granting anonymous access.

If running the migration reports `must be owner of table objects`, use the
latest copy of the migration in this repository. Supabase owns its managed
Storage table and enables its RLS itself; the migration must not run
`ALTER TABLE storage.objects`. The script still creates the bucket and its
access policies.

Before retrying a failed full migration, check whether its earlier statements
were committed:

```sql
SELECT
    to_regclass('public.profiles') AS profiles_table,
    to_regclass('public.caisses') AS caisses_table;
```

If these tables already exist, do not rerun this fresh-database migration
unchanged: it creates tables without `IF NOT EXISTS`. Confirm whether the prior
SQL run committed and use the fresh project's migration state before retrying.

## Client keys

Clients must use only the project's URL and public anon/publishable key. Never
put a `service_role` key in Android, browser code, or a checked-in file; RLS is
the security boundary for client access.

The Node.js process is only a static file host; clients authenticate with
Supabase Auth and access Postgres/Realtime/Storage directly under RLS. Existing
server-side MySQL data is intentionally not imported into this fresh project.

For the web client, replace the example values in
`server/public/js/supabase-config.js` with the project URL and public key.
For Android, provide `SUPABASE_URL` and `SUPABASE_ANON_KEY` as Gradle
properties or environment variables when building. For example, in PowerShell:

```powershell
$env:SUPABASE_URL = "https://YOUR_PROJECT_ID.supabase.co"
$env:SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_OR_PUBLISHABLE_KEY"
.\gradlew.bat :app:assembleDebug
```

The anon/publishable key and project URL are public client configuration and
will be visible in browser and Android builds; RLS—not concealing that key—
protects the data. Keep project-specific configuration out of Git if desired.
The `service_role` key must never be exposed to clients.

## Storage object paths

Comment images belong in the private `comment-images` bucket at:

`tontines/{tontine_uuid}/{uploader_auth_user_uuid}/{filename}`

The Storage policies allow group members to read group images and restrict
uploads to the authenticated uploader, group members, and image MIME types.
Store the returned object path in `comments.image_url`; do not store a public
URL or a Base64 data URI.
