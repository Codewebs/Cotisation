import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function jsonResponse(body: Record<string, string>, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function required(value: string | undefined, name: string): string {
  if (!value) throw new Error(`Missing ${name} configuration.`);
  return value;
}

async function removeUserCommentImages(
  admin: ReturnType<typeof createClient>,
  userId: string,
): Promise<void> {
  const { data: memberships, error: membershipsError } = await admin
    .from("members")
    .select("tontine_id")
    .eq("user_id", userId);
  if (membershipsError) throw new Error(`Unable to load account memberships: ${membershipsError.message}`);

  const { data: comments, error: commentsError } = await admin
    .from("comments")
    .select("tontine_id")
    .eq("user_id", userId);
  if (commentsError) throw new Error(`Unable to load account comments: ${commentsError.message}`);

  const tontineIds = new Set([
    ...(memberships ?? []).map((membership) => membership.tontine_id as string),
    ...(comments ?? []).map((comment) => comment.tontine_id as string),
  ]);
  const storage = admin.storage.from("comment-images");

  for (const tontineId of tontineIds) {
    const folder = `tontines/${tontineId}/${userId}`;
    const paths: string[] = [];
    for (let offset = 0; ; offset += 1000) {
      const { data, error } = await storage.list(folder, { limit: 1000, offset });
      if (error) throw new Error(`Unable to list account images: ${error.message}`);

      const files = (data ?? []).filter((entry) => entry.id !== null);
      paths.push(...files.map((file) => `${folder}/${file.name}`));
      if ((data ?? []).length < 1000) break;
    }

    for (let start = 0; start < paths.length; start += 100) {
      const { error } = await storage.remove(paths.slice(start, start + 100));
      if (error) throw new Error(`Unable to remove account images: ${error.message}`);
    }
  }
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed." }, 405);
  }

  try {
    const authorization = request.headers.get("authorization");
    if (!authorization?.startsWith("Bearer ")) {
      return jsonResponse({ error: "Authentication is required." }, 401);
    }

    let body: unknown;
    try {
      body = await request.json();
    } catch {
      return jsonResponse({ error: "Invalid request body." }, 400);
    }
    const userId = (body as { userId?: unknown })?.userId;
    if (typeof userId !== "string" ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(userId)) {
      return jsonResponse({ error: "Invalid account identifier." }, 400);
    }

    const supabaseUrl = required(Deno.env.get("SUPABASE_URL"), "SUPABASE_URL");
    const anonKey = required(Deno.env.get("SUPABASE_ANON_KEY"), "SUPABASE_ANON_KEY");
    const serviceRoleKey = required(
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY"),
      "SUPABASE_SERVICE_ROLE_KEY",
    );
    const authenticatedClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authorization } },
      auth: { autoRefreshToken: false, persistSession: false },
    });
    const { data: { user }, error: authError } = await authenticatedClient.auth.getUser();
    if (authError || !user) {
      return jsonResponse({ error: "The session is invalid or expired. Sign in again." }, 401);
    }
    if (user.id.toLowerCase() !== userId.toLowerCase()) {
      return jsonResponse({ error: "The authenticated account does not match the requested account." }, 403);
    }

    const admin = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });
    await removeUserCommentImages(admin, user.id);

    const { error: anonymizeError } = await admin
      .from("members")
      .update({ user_id: null, nom_saisi: "Membre supprimé", phone: null })
      .eq("user_id", user.id);
    if (anonymizeError) {
      throw new Error(`Unable to anonymize account memberships: ${anonymizeError.message}`);
    }

    const { error: deleteError } = await admin.auth.admin.deleteUser(user.id);
    if (deleteError) throw new Error(`Unable to delete account: ${deleteError.message}`);

    return jsonResponse({ message: "Account and personal data deleted." }, 200);
  } catch (error) {
    console.error("Account deletion failed:", error);
    return jsonResponse({ error: "Deletion failed. Please retry or contact support." }, 500);
  }
});
