import { randomUUID } from "node:crypto";

const supabaseUrl = process.env.SUPABASE_URL;
const anonKey = process.env.SUPABASE_ANON_KEY;

if (!supabaseUrl || !anonKey) {
  throw new Error("SUPABASE_URL and SUPABASE_ANON_KEY are required.");
}

const stamp = Date.now();
const account = {
  email: `flipledger.qa.${stamp}@example.com`,
  password: "FlipLedger-QA-2026!",
};

async function request(path, { token = anonKey, body, method = "POST" } = {}) {
  const response = await fetch(`${supabaseUrl}${path}`, {
    method,
    headers: {
      apikey: anonKey,
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  return { response, data };
}

const signup = await request("/auth/v1/signup", {
  body: {
    email: account.email,
    password: account.password,
    data: {
      full_name: "FlipLedger QA",
      business_name: "QA Solo Subscription",
    },
  },
});

if (!signup.response.ok || !signup.data?.access_token || !signup.data?.user?.id) {
  throw new Error(
    `Account creation failed (${signup.response.status}): ${
      JSON.stringify(signup.data)
    }`,
  );
}

const accessToken = signup.data.access_token;
const addedDevices = [];

for (let index = 1; index <= 5; index += 1) {
  const result = await request("/rest/v1/rpc/add_device", {
    token: accessToken,
    body: {
      p_device: {
        id: randomUUID(),
        category: "Phone",
        model: `QA iPhone ${index}`,
        identifier: `QA-${stamp}-${index}`,
        condition: "Good",
        storage: "128 GB",
        lock: "Unlocked",
        purchase_price_cents: 10000 + index,
        source: "QA test",
        purchase_date: new Date().toISOString().slice(0, 10),
        status: "Purchased",
        days_held: 0,
      },
      p_costs: [],
    },
  });
  addedDevices.push({ index, status: result.response.status });
  if (!result.response.ok) {
    throw new Error(
      `Device ${index} failed (${result.response.status}): ${
        JSON.stringify(result.data)
      }`,
    );
  }
}

const sixthDevice = await request("/rest/v1/rpc/add_device", {
  token: accessToken,
  body: {
    p_device: {
      id: randomUUID(),
      category: "Phone",
      model: "QA iPhone 6 blocked",
      identifier: `QA-${stamp}-6`,
      condition: "Good",
      storage: "128 GB",
      lock: "Unlocked",
      purchase_price_cents: 10006,
      source: "QA test",
      purchase_date: new Date().toISOString().slice(0, 10),
      status: "Purchased",
      days_held: 0,
    },
    p_costs: [],
  },
});

const billingBeforeCheckout = await request(
  "/rest/v1/billing_accounts?select=lifetime_devices_created,subscription_status,plan_tier",
  { token: accessToken, method: "GET" },
);

const checkout = await request("/functions/v1/create-checkout-session", {
  token: accessToken,
  body: { plan: "solo" },
});

if (!checkout.response.ok || !checkout.data?.url) {
  throw new Error(
    `Solo checkout creation failed (${checkout.response.status}): ${
      JSON.stringify(checkout.data)
    }`,
  );
}

const limitWasEnforced =
  sixthDevice.response.status >= 400 &&
  JSON.stringify(sixthDevice.data).includes("FREE_DEVICE_LIMIT_REACHED");

console.log(JSON.stringify({
  account,
  userId: signup.data.user.id,
  addedDevices,
  sixthDevice: {
    status: sixthDevice.response.status,
    limitWasEnforced,
    errorCode: limitWasEnforced ? "FREE_DEVICE_LIMIT_REACHED" : null,
  },
  billingBeforeCheckout: billingBeforeCheckout.data,
  checkoutUrl: checkout.data.url,
}, null, 2));
