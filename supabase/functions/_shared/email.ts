import type { SupabaseClient } from "npm:@supabase/supabase-js@^2.95.0";
import { requiredEnv } from "./clients.ts";

interface SendEmailInput {
  to: string;
  subject: string;
  html: string;
}

/**
 * Sends a transactional email via Resend. Provider details are isolated here so switching to
 * SendGrid/Postmark/etc. only touches this function. Throws on a non-2xx response so callers
 * can decide whether to swallow the failure.
 */
export async function sendEmail({ to, subject, html }: SendEmailInput): Promise<void> {
  const apiKey = requiredEnv("RESEND_API_KEY");
  const from = requiredEnv("EMAIL_FROM"); // e.g. "BlackInk <noreply@yourdomain.com>"

  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ from, to, subject, html }),
  });

  if (!response.ok) {
    const detail = await response.text();
    throw new Error(`Resend send failed (${response.status}): ${detail}`);
  }
}

/**
 * Emails a newly registered user a welcome message. Best-effort by contract: callers should catch
 * and log so a mail failure never fails the auth flow / webhook that triggered it.
 */
export async function sendWelcomeEmail(
  email: string,
  fullName?: string | null,
  businessName?: string | null,
): Promise<void> {
  const name = fullName?.trim() || email.split("@")[0];
  const business = businessName?.trim();

  const html = `
  <div style="background:#f5f5f7;padding:32px 0;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
    <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #ececf1;">
      <div style="background:#6c5ce7;padding:28px 32px;">
        <div style="color:#ffffff;font-size:20px;font-weight:700;">BlackInk</div>
      </div>
      <div style="padding:28px 32px;">
        <h1 style="margin:0 0 12px;font-size:22px;color:#1c1c26;">Welcome, ${name} 👋</h1>
        <p style="margin:0 0 16px;color:#4b4b57;font-size:15px;line-height:22px;">
          Thanks for creating your BlackInk account${
    business ? ` for <strong>${business}</strong>` : ""
  }. You're all set to log devices, track costs, and see your profit on every flip.
        </p>
        <p style="margin:0 0 8px;color:#1c1c26;font-size:14px;font-weight:600;">Getting started</p>
        <ul style="margin:0 0 20px;padding-left:20px;">
          <li style="margin:0 0 6px;color:#4b4b57;font-size:14px;line-height:20px;">Add your first device to inventory</li>
          <li style="margin:0 0 6px;color:#4b4b57;font-size:14px;line-height:20px;">Record a sale to watch your profit update</li>
          <li style="margin:0 0 6px;color:#4b4b57;font-size:14px;line-height:20px;">Review your numbers on the dashboard</li>
        </ul>
        <p style="margin:16px 0 0;color:#a0a0ad;font-size:12px;">— The BlackInk team</p>
      </div>
    </div>
  </div>`;

  await sendEmail({
    to: email,
    subject: "Welcome to BlackInk 👋",
    html,
  });
}

const TIER_NAMES: Record<string, string> = { solo: "Solo", partner: "Partner" };

const TIER_PERKS: Record<string, string[]> = {
  solo: ["Unlimited device records", "Full profit tracking", "CSV export"],
  partner: [
    "Everything in Solo",
    "Partner cost & profit splits",
    "Partner settlement reports",
  ],
};

/**
 * Emails the subscriber a confirmation after a successful subscription purchase.
 * Best-effort by contract: callers should catch and log so a mail failure never fails the webhook.
 */
export async function sendSubscriptionWelcomeEmail(
  admin: SupabaseClient,
  userId: string,
  tier: string,
): Promise<void> {
  const { data, error } = await admin.auth.admin.getUserById(userId);
  if (error || !data.user?.email) {
    throw new Error(
      `Cannot resolve email for user ${userId}: ${error?.message ?? "no email on account"}`,
    );
  }

  const email = data.user.email;
  const name =
    (data.user.user_metadata?.full_name as string | undefined)?.trim() ||
    email.split("@")[0];
  const planName = TIER_NAMES[tier] ?? tier;
  const perks = TIER_PERKS[tier] ?? [];

  const perksHtml = perks
    .map(
      (perk) =>
        `<li style="margin:0 0 6px;color:#4b4b57;font-size:14px;line-height:20px;">${perk}</li>`,
    )
    .join("");

  const html = `
  <div style="background:#f5f5f7;padding:32px 0;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
    <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #ececf1;">
      <div style="background:#6c5ce7;padding:28px 32px;">
        <div style="color:#ffffff;font-size:20px;font-weight:700;">BlackInk</div>
      </div>
      <div style="padding:28px 32px;">
        <h1 style="margin:0 0 12px;font-size:22px;color:#1c1c26;">You're on ${planName} 🎉</h1>
        <p style="margin:0 0 16px;color:#4b4b57;font-size:15px;line-height:22px;">
          Hi ${name}, thanks for subscribing to <strong>BlackInk ${planName}</strong>.
          Your plan is active and you can keep flipping without limits.
        </p>
        ${
    perksHtml
      ? `<p style="margin:0 0 8px;color:#1c1c26;font-size:14px;font-weight:600;">What's included</p>
             <ul style="margin:0 0 20px;padding-left:20px;">${perksHtml}</ul>`
      : ""
  }
        <p style="margin:0 0 4px;color:#8a8a99;font-size:13px;line-height:19px;">
          Manage or cancel anytime from <strong>More → Subscription</strong> in the app.
        </p>
        <p style="margin:16px 0 0;color:#a0a0ad;font-size:12px;">— The BlackInk team</p>
      </div>
    </div>
  </div>`;

  await sendEmail({
    to: email,
    subject: `You're on BlackInk ${planName} 🎉`,
    html,
  });
}
