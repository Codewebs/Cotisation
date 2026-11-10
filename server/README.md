# Cotisation web client

The Node.js process only serves the static web client. Authentication, database
access, realtime subscriptions, and image storage are provided directly by
Supabase; this process does not connect to MySQL or run a WebSocket server.

## Setup

1. Follow [the Supabase setup guide](../supabase/README.md) and apply the SQL
   migration in `../supabase/migrations/`.
2. Configure the Supabase project URL and public anon/publishable key using the
   web client's local configuration instructions.
3. Install and start the static web server:

   ```bash
   npm install
   npm start
   ```

The site is served at `http://localhost:3000`. To host it elsewhere, serve the
`public/` directory and configure its client-side Supabase settings there.

Only the project's public anon/publishable key may be used in browser code.
Never put a `service_role` key in this directory.
