import { readFileSync, mkdirSync, writeFileSync } from "node:fs";
const text = readFileSync(
  new URL("../src/main/resources/application.properties", import.meta.url),
  "utf8",
);
const property = (key) =>
  text
    .split(/\r?\n/)
    .find((line) => line.startsWith(key + "="))
    ?.slice(key.length + 1)
    .trim();
const base = (
  process.env.KEYCLOAK_URL || property("keycloak.auth-server-url")
).replace(/\/$/, "");
const realm = process.env.KEYCLOAK_REALM || property("keycloak.realm");
const clientId =
  process.env.KEYCLOAK_CLIENT_ID || property("keycloak.client-id");
const callback =
  process.env.OIDC_CALLBACK_URI || "http://localhost:8080/api/auth/callback";
const apply = process.argv.includes("--apply");
const auth = await fetch(
  base + "/realms/master/protocol/openid-connect/token",
  {
    method: "POST",
    body: new URLSearchParams({
      grant_type: "password",
      client_id: "admin-cli",
      username:
        process.env.KEYCLOAK_ADMIN || property("keycloak.admin-username"),
      password:
        process.env.KEYCLOAK_ADMIN_PASSWORD ||
        property("keycloak.admin-password"),
    }),
    signal: AbortSignal.timeout(15000),
  },
);
if (!auth.ok)
  throw new Error("Keycloak admin authentication failed: " + auth.status);
const { access_token: token } = await auth.json();
const root = base + "/admin/realms/" + encodeURIComponent(realm);
async function request(path, method = "GET", body) {
  const result = await fetch(root + path, {
    method,
    headers: {
      Authorization: "Bearer " + token,
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(15000),
  });
  if (!result.ok)
    throw new Error(`Keycloak ${method} request failed: ${result.status}`);
  return result.status === 204 ? null : result.json();
}
const before = await request("");
const clients = await request(
  "/clients?clientId=" + encodeURIComponent(clientId),
);
if (clients.length !== 1)
  throw new Error("Expected exactly one application client");
const client = clients[0];
const smtp = JSON.stringify(before.smtpServer || {});
if (!before.smtpServer?.host || !before.smtpServer?.from)
  throw new Error("Configure real SMTP in Keycloak before enabling onboarding");
if (/mailhog|localhost|127\.0\.0\.1/i.test(before.smtpServer.host))
  throw new Error("Keycloak verification must use real SMTP, not MailHog");
const actions = await request("/authentication/required-actions");
if (apply) {
  const backupDirectory = new URL(
    "../target/keycloak-config-backups/",
    import.meta.url,
  );
  mkdirSync(backupDirectory, { recursive: true });
  writeFileSync(
    new URL(`onboarding-${Date.now()}.json`, backupDirectory),
    JSON.stringify(
      {
        realm,
        registrationAllowed: before.registrationAllowed,
        client: {
          id: client.id,
          standardFlowEnabled: client.standardFlowEnabled,
          directAccessGrantsEnabled: client.directAccessGrantsEnabled,
          implicitFlowEnabled: client.implicitFlowEnabled,
          redirectUris: client.redirectUris,
          pkceMethod: client.attributes?.["pkce.code.challenge.method"] ?? null,
        },
        requiredActions: actions.filter((action) =>
          ["UPDATE_PASSWORD", "VERIFY_EMAIL"].includes(action.alias),
        ),
      },
      null,
      2,
    ),
  );
  await request("", "PUT", {
    registrationAllowed: false,
  });
  await request("/clients/" + client.id, "PUT", {
    standardFlowEnabled: true,
    directAccessGrantsEnabled: false,
    implicitFlowEnabled: false,
    redirectUris: [callback],
    attributes: { ...client.attributes, "pkce.code.challenge.method": "S256" },
  });
  for (const alias of ["UPDATE_PASSWORD", "VERIFY_EMAIL"]) {
    const action = actions.find((value) => value.alias === alias);
    if (!action) throw new Error("Required action missing: " + alias);
    await request("/authentication/required-actions/" + alias, "PUT", {
      ...action,
      enabled: true,
    });
  }
}
const after = await request("");
const updatedClient = await request("/clients/" + client.id);
if (JSON.stringify(after.smtpServer || {}) !== smtp)
  throw new Error("SMTP changed unexpectedly");
const secret = await request("/clients/" + client.id + "/client-secret");
const updatedActions = await request("/authentication/required-actions");
const result = {
  requiredActionsEnabled: ["UPDATE_PASSWORD", "VERIFY_EMAIL"].every((alias) =>
    updatedActions.some((action) => action.alias === alias && action.enabled),
  ),
  applied: apply,
  smtpConfigured: true,
  smtpUnchanged: true,
  registrationDisabled: after.registrationAllowed === false,
  standardFlowEnabled: updatedClient.standardFlowEnabled,
  directGrantsDisabled: updatedClient.directAccessGrantsEnabled === false,
  pkce: updatedClient.attributes?.["pkce.code.challenge.method"],
  callbackAllowed: updatedClient.redirectUris?.includes(callback),
  backendClientSecretMatches:
    secret.value ===
    (process.env.KEYCLOAK_CLIENT_SECRET || property("keycloak.client-secret")),
  userStorageProviders: (
    await request("/components?type=org.keycloak.storage.UserStorageProvider")
  ).length,
};
console.log(JSON.stringify(result, null, 2));
if (
  apply &&
  (!result.requiredActionsEnabled ||
    !result.registrationDisabled ||
    !result.standardFlowEnabled ||
    !result.directGrantsDisabled ||
    result.pkce !== "S256" ||
    !result.callbackAllowed ||
    !result.backendClientSecretMatches)
)
  process.exitCode = 1;
