import { createClient, type SupabaseClient, type User } from "npm:@supabase/supabase-js@^2.95.0";
import Stripe from "npm:stripe@^22.0.0";

const STRIPE_API_VERSION = "2026-06-24.dahlia";

export function requiredEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing required server configuration: ${name}`);
  return value;
}

export function createStripeClient(): Stripe {
  return new Stripe(requiredEnv("STRIPE_API_KEY"), {
    apiVersion: STRIPE_API_VERSION,
  });
}

export function createAdminClient(): SupabaseClient {
  return createClient(
    requiredEnv("SUPABASE_URL"),
    requiredEnv("SUPABASE_SERVICE_ROLE_KEY"),
    {
      auth: {
        autoRefreshToken: false,
        persistSession: false,
      },
    },
  );
}

export async function requireUser(
  request: Request,
  admin: SupabaseClient,
): Promise<User> {
  const authorization = request.headers.get("Authorization");
  const token = authorization?.startsWith("Bearer ")
    ? authorization.slice("Bearer ".length)
    : null;
  if (!token) throw new AuthError();

  const { data, error } = await admin.auth.getUser(token);
  if (error || !data.user) throw new AuthError();
  return data.user;
}

export class AuthError extends Error {
  constructor() {
    super("Authentication required.");
  }
}
