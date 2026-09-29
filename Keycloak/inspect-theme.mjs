import { spawn } from "node:child_process";
import {
  mkdirSync,
  mkdtempSync,
  existsSync,
  readFileSync,
  writeFileSync,
} from "node:fs";
import { resolve } from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import { createHash, randomBytes } from "node:crypto";

const target = resolve("Backend/target/keycloak-theme-preview");
mkdirSync(target, { recursive: true });
const profile = mkdtempSync(resolve(target, "edge-"));
const edge = spawn(
  "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
  [
    "--headless=new",
    "--no-first-run",
    "--no-default-browser-check",
    "--disable-background-networking",
    "--remote-debugging-port=0",
    "--user-data-dir=" + profile,
    "--autoplay-policy=no-user-gesture-required",
    "about:blank",
  ],
  { windowsHide: true, stdio: "ignore" },
);
let socket;
try {
  const activePort = resolve(profile, "DevToolsActivePort");
  for (let i = 0; i < 100 && !existsSync(activePort); i++) await delay(100);
  if (!existsSync(activePort)) throw new Error("Browser did not start");
  const port = readFileSync(activePort, "utf8").split("\n")[0];
  const tab = await (
    await fetch("http://127.0.0.1:" + port + "/json/new?about:blank", {
      method: "PUT",
    })
  ).json();
  socket = new WebSocket(tab.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener("open", resolve, { once: true });
    socket.addEventListener("error", reject, { once: true });
  });
  let serial = 0;
  const pending = new Map();
  const errors = [];
  socket.addEventListener("message", (event) => {
    const message = JSON.parse(event.data);
    if (message.id && pending.has(message.id)) {
      const { resolve, reject } = pending.get(message.id);
      pending.delete(message.id);
      message.error
        ? reject(new Error(message.error.message))
        : resolve(message.result);
    }
    if (message.method === "Runtime.exceptionThrown")
      errors.push(message.params.exceptionDetails.text);
  });
  function cdp(method, params = {}) {
    const id = ++serial;
    return new Promise((resolve, reject) => {
      pending.set(id, { resolve, reject });
      socket.send(JSON.stringify({ id, method, params }));
    });
  }
  async function evaluate(expression) {
    const result = await cdp("Runtime.evaluate", {
      expression,
      returnByValue: true,
      awaitPromise: true,
    });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
    return result.result.value;
  }
  await cdp("Page.enable");
  await cdp("Runtime.enable");
  await cdp("Emulation.setDeviceMetricsOverride", {
    width: 1365,
    height: 900,
    deviceScaleFactor: 1,
    mobile: false,
  });
  let url;
  try {
    const response = await fetch("http://localhost:8080/api/auth/authorize", {
      redirect: "manual",
    });
    url = response.headers.get("location");
  } catch {}
  if (!url) {
    const verifier = randomBytes(32).toString("base64url");
    const params = new URLSearchParams({
      client_id: "shopping-backend",
      response_type: "code",
      redirect_uri: "http://localhost:8080/api/auth/callback",
      scope: "openid profile email",
      state: randomBytes(32).toString("base64url"),
      code_challenge_method: "S256",
      code_challenge: createHash("sha256").update(verifier).digest("base64url"),
      kc_locale: "vi",
    });
    url =
      "http://localhost:8081/realms/Shopping/protocol/openid-connect/auth?" +
      params;
  }
  const localizedUrl = new URL(url);
  localizedUrl.searchParams.set("kc_locale", "vi");
  localizedUrl.searchParams.set("ui_locales", "vi");
  await cdp("Page.navigate", { url: localizedUrl.toString() });
  let ready = false;
  for (let i = 0; i < 100; i++) {
    ready = await evaluate(
      'document.readyState === "complete" && !!document.querySelector("#kc-form-login")',
    );
    if (ready) break;
    await delay(100);
  }
  if (!ready) throw new Error("Keycloak login form failed to render");
  await delay(1500);
  const desktop = await evaluate(
    '({lang:document.documentElement.lang,locale:document.querySelector("#kc-current-locale-link")?.textContent,theme:document.body.classList.contains("enterprise-login"),title:document.querySelector("#kc-page-title").textContent.trim(),cardWidth:document.querySelector(".card-pf").getBoundingClientRect().width,buttonColor:getComputedStyle(document.querySelector("#kc-login")).backgroundColor,blur:getComputedStyle(document.querySelector(".card-pf")).backdropFilter,subtitleColor:getComputedStyle(document.querySelector(".enterprise-subtitle")).color,videoReady:document.querySelector("video").readyState,videoUrl:document.querySelector("video source").src,cssUrl:[...document.querySelectorAll("link[rel=stylesheet]")].find(x=>x.href.includes("enterprise.css"))?.href,hasRegistration:!!document.querySelector("#kc-registration"),hasForgotPassword:!!document.querySelector("a[href*=reset-credentials]")})',
  );
  if (
    !desktop.theme ||
    desktop.title !== "Đăng nhập" ||
    desktop.cardWidth !== 420
  )
    throw new Error("Unexpected desktop theme: " + JSON.stringify(desktop));
  if (desktop.subtitleColor !== "rgba(255, 255, 255, 0.9)")
    throw new Error("Subtitle contrast style was overridden");
  if (!desktop.cssUrl || !desktop.videoUrl)
    throw new Error("Theme assets missing");
  const cssResponse = await fetch(desktop.cssUrl);
  const videoResponse = await fetch(desktop.videoUrl, { method: "HEAD" });
  if (cssResponse.status !== 200 || videoResponse.status !== 200)
    throw new Error("Theme assets failed");
  const screenshot = await cdp("Page.captureScreenshot", {
    format: "png",
    captureBeyondViewport: false,
  });
  writeFileSync(
    resolve(target, "desktop.png"),
    Buffer.from(screenshot.data, "base64"),
  );
  const toggled = await evaluate(
    '(()=>{const input=document.querySelector("#password");const button=document.querySelector("button[data-password-toggle]");if(!button)return false;button.click();const visible=input.type==="text";button.click();return visible && input.type==="password";})()',
  );
  if (!toggled) throw new Error("Native password visibility control failed");
  await cdp("Emulation.setDeviceMetricsOverride", {
    width: 390,
    height: 844,
    deviceScaleFactor: 1,
    mobile: true,
  });
  await delay(300);
  const mobile = await evaluate(
    '({width:innerWidth,scrollWidth:document.documentElement.scrollWidth,cardWidth:document.querySelector(".card-pf").getBoundingClientRect().width})',
  );
  if (mobile.scrollWidth > mobile.width)
    throw new Error("Horizontal overflow on mobile");
  const mobileShot = await cdp("Page.captureScreenshot", {
    format: "png",
    captureBeyondViewport: false,
  });
  writeFileSync(
    resolve(target, "mobile.png"),
    Buffer.from(mobileShot.data, "base64"),
  );
  await cdp("Emulation.setEmulatedMedia", {
    features: [{ name: "prefers-reduced-motion", value: "reduce" }],
  });
  await delay(100);
  const reducedMotion = await evaluate(
    'document.querySelector("video").paused && getComputedStyle(document.querySelector("video")).display === "none"',
  );
  if (!reducedMotion)
    throw new Error("Reduced-motion preference was not respected");
  await cdp("Emulation.setEmulatedMedia", { features: [] });
  await cdp("Emulation.setDeviceMetricsOverride", {
    width: 1365,
    height: 900,
    deviceScaleFactor: 1,
    mobile: false,
  });
  const forgotUrl = await evaluate(
    'document.querySelector("a[href*=reset-credentials]")?.href',
  );
  if (forgotUrl) {
    await cdp("Page.navigate", { url: forgotUrl });
    let forgotReady = false;
    for (let i = 0; i < 80; i++) {
      forgotReady = await evaluate(
        'document.readyState === "complete" && !!document.querySelector("#kc-reset-password-form")',
      );
      if (forgotReady) break;
      await delay(100);
    }
    if (!forgotReady) throw new Error("Forgot password page failed to render");
    const inheritedTheme = await evaluate(
      'document.body.classList.contains("enterprise-login") && !!document.querySelector("video") && getComputedStyle(document.querySelector(".card-pf")).borderRadius === "24px"',
    );
    if (!inheritedTheme) throw new Error("Forgot-password theme missing");
    const shot = await cdp("Page.captureScreenshot", {
      format: "png",
      captureBeyondViewport: false,
    });
    writeFileSync(
      resolve(target, "forgot-password.png"),
      Buffer.from(shot.data, "base64"),
    );
  }
  if (errors.length) throw new Error("Browser errors: " + errors.join(", "));
  delete desktop.videoUrl;
  delete desktop.cssUrl;
  console.log(
    JSON.stringify(
      {
        desktop,
        mobile,
        passwordToggleWorks: toggled,
        reducedMotion,
        assets: "HTTP 200",
        screenshots: target,
      },
      null,
      2,
    ),
  );
  await cdp("Browser.close").catch(() => {});
} finally {
  socket?.close();
  edge.kill();
}
