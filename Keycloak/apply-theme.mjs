import { readFileSync, mkdirSync, writeFileSync } from "node:fs";
const properties = readFileSync(
  new URL(
    "../Backend/src/main/resources/application.properties",
    import.meta.url,
  ),
  "utf8",
);
const property = (name) =>
  properties
    .split(/\r?\n/)
    .find((line) => line.startsWith(name + "="))
    ?.slice(name.length + 1)
    .trim();
const base = (
  process.env.KEYCLOAK_URL || property("keycloak.auth-server-url")
).replace(/\/$/, "");
const realm = process.env.KEYCLOAK_REALM || property("keycloak.realm");
const response = await fetch(
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
if (!response.ok)
  throw new Error("Admin authentication failed: " + response.status);
const { access_token: token } = await response.json();
const url = base + "/admin/realms/" + encodeURIComponent(realm);
const headers = {
  Authorization: "Bearer " + token,
  "Content-Type": "application/json",
};
const currentResponse = await fetch(url, { headers });
if (!currentResponse.ok) throw new Error("Cannot read realm");
const current = await currentResponse.json();
const directory = new URL(
  "../Backend/target/keycloak-config-backups/",
  import.meta.url,
);
mkdirSync(directory, { recursive: true });
writeFileSync(
  new URL("theme-" + Date.now() + ".json", directory),
  JSON.stringify(
    {
      loginTheme: current.loginTheme || "",
      internationalizationEnabled: current.internationalizationEnabled,
      defaultLocale: current.defaultLocale,
      supportedLocales: current.supportedLocales,
    },
    null,
    2,
  ),
);
const update = await fetch(url, {
  method: "PUT",
  headers,
  body: JSON.stringify({
    loginTheme: "enterprise",
    internationalizationEnabled: true,
    defaultLocale: "vi",
    supportedLocales: [
      ...new Set([...(current.supportedLocales || []), "vi", "en"]),
    ],
  }),
});
if (!update.ok) throw new Error("Cannot apply theme: " + update.status);
const after = await (await fetch(url, { headers })).json();
if (JSON.stringify(after.smtpServer) !== JSON.stringify(current.smtpServer))
  throw new Error("SMTP unexpectedly changed");
if (after.registrationAllowed !== current.registrationAllowed)
  throw new Error("Registration setting unexpectedly changed");
console.log(
  JSON.stringify({
    loginTheme: after.loginTheme,
    defaultLocale: after.defaultLocale,
    smtpUnchanged: true,
    registrationUnchanged: true,
  }),
);
