import { createClient } from "npm:@supabase/supabase-js@2";

type NotificationRecord = {
  id: string;
  user_id: string;
  title: string;
  message: string | null;
  type: string | null;
  reference_id: string | null;
  created_at: number | string;
  group_key: string | null;
  actor_id: string | null;
  actor_name: string | null;
  context_name: string | null;
};

type DatabaseWebhook = {
  type: string;
  table: string;
  schema: string;
  record: NotificationRecord;
};

type FirebaseServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

const encoder = new TextEncoder();
const supabaseUrl = Deno.env.get("SUPABASE_URL");
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const webhookSecret = Deno.env.get("PUSH_WEBHOOK_SECRET");
const serviceAccountJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");

function required(value: string | undefined, name: string): string {
  if (!value) throw new Error(`Missing ${name} secret.`);
  return value;
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}

function encodeJson(value: unknown): string {
  return base64Url(encoder.encode(JSON.stringify(value)));
}

async function getFirebaseAccessToken(account: FirebaseServiceAccount): Promise<string> {
  const issuedAt = Math.floor(Date.now() / 1000);
  const unsignedAssertion = [
    encodeJson({ alg: "RS256", typ: "JWT" }),
    encodeJson({
      iss: account.client_email,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: "https://oauth2.googleapis.com/token",
      iat: issuedAt,
      exp: issuedAt + 3600,
    }),
  ].join(".");

  const pem = account.private_key
    .replaceAll("\\n", "\n")
    .replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const key = await crypto.subtle.importKey(
    "pkcs8",
    Uint8Array.from(atob(pem), (char) => char.charCodeAt(0)),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    encoder.encode(unsignedAssertion),
  );
  const assertion = `${unsignedAssertion}.${base64Url(new Uint8Array(signature))}`;
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) {
    throw new Error(`Google OAuth token request failed (${response.status}).`);
  }
  const result = await response.json();
  if (typeof result.access_token !== "string") {
    throw new Error("Google OAuth response did not contain an access token.");
  }
  return result.access_token;
}

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return new Response("Method not allowed.", { status: 405 });
  }

  try {
    const secret = required(webhookSecret, "PUSH_WEBHOOK_SECRET");
    if (request.headers.get("x-push-webhook-secret") !== secret) {
      return new Response("Unauthorized.", { status: 401 });
    }

    const event = (await request.json()) as DatabaseWebhook;
    if (event.type !== "INSERT" || event.schema !== "public" ||
      event.table !== "notifications" || !event.record?.user_id) {
      return new Response("Ignored event.", { status: 200 });
    }
    if (![
      "CHAT_MESSAGE",
      "JOIN_REQUEST",
      "TONTINE_SETTINGS",
      "CAISSE_SETTINGS",
      "CAISSE_MEMBERSHIP",
      "COTISATION_RECEIPT",
    ].includes(event.record.type ?? "")) {
      return new Response("Notification type is not enabled for push.", { status: 200 });
    }

    const account = JSON.parse(
      required(serviceAccountJson, "FIREBASE_SERVICE_ACCOUNT"),
    ) as FirebaseServiceAccount;
    const client = createClient(
      required(supabaseUrl, "SUPABASE_URL"),
      required(serviceRoleKey, "SUPABASE_SERVICE_ROLE_KEY"),
      { auth: { persistSession: false, autoRefreshToken: false } },
    );
    const { data: devices, error } = await client
      .from("user_devices")
      .select("id,fcm_token")
      .eq("user_id", event.record.user_id);
    if (error) throw new Error(`Unable to load push devices: ${error.message}`);
    if (!devices?.length) return new Response("No registered devices.", { status: 200 });

    const accessToken = await getFirebaseAccessToken(account);
    const results = await Promise.all(devices.map(async (device) => {
      const response = await fetch(
        `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(account.project_id)}/messages:send`,
        {
          method: "POST",
          headers: {
            authorization: `Bearer ${accessToken}`,
            "content-type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token: device.fcm_token,
              data: {
                notification_id: event.record.id,
                title: event.record.title,
                body: event.record.message ?? "",
                type: event.record.type ?? "",
                reference_id: event.record.reference_id ?? "",
                group_key: event.record.group_key ?? `event:${event.record.id}`,
                actor_id: event.record.actor_id ?? "",
                actor_name: event.record.actor_name ?? "",
                context_name: event.record.context_name ?? "",
                created_at: String(event.record.created_at),
              },
              android: { priority: "HIGH" },
            },
          }),
        },
      );
      if (response.ok) return null;

      const details = await response.text();
      if (details.includes("UNREGISTERED")) {
        const { error: deleteError } = await client
          .from("user_devices")
          .delete()
          .eq("id", device.id);
        if (deleteError) {
          console.error("Could not remove an expired FCM token:", deleteError.message);
        }
        return null;
      }
      return `FCM send failed (${response.status}): ${details}`;
    }));

    const failures = results.filter((result): result is string => result !== null);
    if (failures.length) {
      console.error("Some FCM messages failed:", failures);
      return new Response("One or more FCM sends failed.", { status: 502 });
    }
    return new Response("Push sent.", { status: 200 });
  } catch (error) {
    console.error("Push delivery failed:", error);
    return new Response("Push delivery failed.", { status: 500 });
  }
});
