const labels: Record<string, { title: string; message: string }> = {
  success: {
    title: "Subscription received",
    message: "Return to FlipLedger and refresh your subscription status.",
  },
  cancelled: {
    title: "Checkout cancelled",
    message: "No charge was made. You can return to FlipLedger.",
  },
  portal: {
    title: "Subscription updated",
    message: "Return to FlipLedger to see your current plan.",
  },
};

Deno.serve((request) => {
  const result = new URL(request.url).searchParams.get("result") ?? "portal";
  const copy = labels[result] ?? labels.portal;
  const appUrl = `flipledger://subscription?result=${encodeURIComponent(result)}`;

  return new Response(
    `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${copy.title}</title>
  <style>
    body { margin: 0; min-height: 100vh; display: grid; place-items: center; background: #f9f7f7; color: #282829; font-family: system-ui, sans-serif; }
    main { width: min(32rem, calc(100% - 3rem)); text-align: center; }
    a { display: inline-block; margin-top: 1.25rem; padding: .9rem 1.25rem; border-radius: .75rem; background: #282829; color: white; text-decoration: none; font-weight: 650; }
  </style>
</head>
<body>
  <main>
    <h1>${copy.title}</h1>
    <p>${copy.message}</p>
    <a href="${appUrl}">Return to FlipLedger</a>
  </main>
  <script>window.setTimeout(() => { window.location.href = ${JSON.stringify(appUrl)}; }, 350);</script>
</body>
</html>`,
    { headers: { "Content-Type": "text/html; charset=utf-8" } },
  );
});
