import assert from "node:assert/strict";
import { mkdir, readFile } from "node:fs/promises";
import { chromium } from "playwright";
const browser = await chromium.launch({
  channel: process.env.BROWSER_CHANNEL || "chrome",
  headless: true,
});
const context = await browser.newContext({
  viewport: { width: 1440, height: 1000 },
  reducedMotion: "reduce",
});
const page = await context.newPage();
const errors = [];
page.on("pageerror", (e) => errors.push(e.message));
page.on("console", (m) => {
  if (m.type() === "error") errors.push(m.text());
});
const base = process.env.BASE_URL || "http://127.0.0.1:8082/";
async function navigate(name) {
  const nav = page.getByRole("navigation", { name: "Workspace navigation" });
  if (!(await nav.getByRole("link", { name, exact: true }).isVisible()))
    await page.getByRole("button", { name: "Open navigation", exact: true }).click();
  await nav.getByRole("link", { name, exact: true }).click();
}
try {
  await page.goto(base);
  await page
    .getByRole("heading", { name: "Good gear. In good hands.", exact: true })
    .waitFor();
  await page.getByRole("button", { name: "Explore the demo", exact: true }).click();
  await page.getByRole("button", { name: "View Aputure Amaran", exact: true }).waitFor();
  await mkdir("docs/screenshots", { recursive: true });
  await page.screenshot({
    path: "docs/screenshots/overview-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: "docs/screenshots/overview-mobile.png", fullPage: true });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await navigate("Gear library");
  await page
    .getByRole("heading", { name: "Find your next possibility.", exact: true })
    .waitFor();
  await page.locator(".equipment-grid .gear-card").nth(7).waitFor();
  await page.screenshot({ path: "docs/screenshots/library-desktop.png", fullPage: true });
  await navigate("People");
  await page
    .locator(".view-heading")
    .getByRole("button", { name: "Add person", exact: true })
    .click();
  let dialog = page.getByRole("dialog");
  await dialog.getByLabel("Full name", { exact: true }).fill("Riley Test");
  await dialog.getByLabel("Team or club", { exact: false }).fill("Media lab");
  await dialog.getByRole("button", { name: "Add person", exact: true }).click();
  await page.getByRole("heading", { name: "Riley Test", exact: true }).waitFor();
  await navigate("Gear library");
  await page
    .locator(".view-heading")
    .getByRole("button", { name: "Add equipment", exact: true })
    .click();
  dialog = page.getByRole("dialog");
  await dialog.getByLabel("Equipment name", { exact: true }).fill("Orbit Test camera");
  await dialog.getByLabel("Model or details", { exact: true }).fill("USB camera");
  await dialog.getByLabel("Total quantity", { exact: true }).fill("2");
  await dialog.getByLabel("Storage location", { exact: true }).fill("Desk B");
  await dialog
    .getByLabel("Description", { exact: false })
    .fill("Fictional equipment. <script>must stay text</script>");
  await dialog.getByRole("button", { name: "Add equipment", exact: true }).click();
  await page.getByRole("button", { name: "Lend Orbit Test camera", exact: true }).click();
  dialog = page.getByRole("dialog");
  await dialog
    .getByLabel("Borrowed by", { exact: false })
    .selectOption({ label: "Riley Test · Media lab" });
  await dialog.getByLabel("Quantity", { exact: true }).fill("2");
  await dialog.getByRole("button", { name: "Create loan", exact: true }).click();
  await page.waitForFunction(() => {
    const card = [...document.querySelectorAll(".gear-card")].find((c) =>
      c.textContent.includes("Orbit Test camera"),
    );
    return card?.textContent.includes("0 of 2 available");
  });
  assert.equal(
    await page
      .getByRole("button", { name: "Lend Orbit Test camera", exact: true })
      .isDisabled(),
    true,
  );
  await navigate("Loans");
  await page.getByRole("textbox", { name: "Search loans", exact: true }).fill("Orbit");
  await page.getByRole("button", { name: /^Orbit Test camera/ }).click();
  await page.getByRole("heading", { name: "The kit’s journey.", exact: true }).waitFor();
  await page.screenshot({ path: "docs/screenshots/loan-details.png", fullPage: true });
  await page.getByRole("button", { name: "Return gear", exact: true }).click();
  await page.getByRole("button", { name: "Keep on loan", exact: true }).click();
  await page.getByRole("button", { name: "Return gear", exact: true }).click();
  await page.getByRole("button", { name: "Confirm return", exact: true }).click();
  await page.locator(".loan-badge.returned").waitFor();
  await navigate("Gear library");
  await page
    .getByRole("textbox", { name: "Search equipment", exact: true })
    .fill("Orbit");
  await page.getByRole("button", { name: "View Orbit Test camera", exact: true }).click();
  await page.getByRole("button", { name: "Edit details", exact: true }).click();
  dialog = page.getByRole("dialog");
  await dialog.getByLabel("Total quantity", { exact: true }).fill("3");
  await dialog.getByRole("button", { name: "Save changes", exact: true }).click();
  await page.waitForFunction(() =>
    document.querySelector(".gear-card")?.textContent.includes("3 of 3 available"),
  );
  await page.reload();
  await page
    .getByRole("heading", { name: "Find your next possibility.", exact: true })
    .waitFor();
  await page
    .getByRole("button", { name: "Open workspace settings", exact: true })
    .click();
  const downloading = page.waitForEvent("download");
  await page.getByRole("button", { name: /Export your workspace/ }).click();
  const download = await downloading;
  const exported = JSON.parse(await readFile(await download.path(), "utf8"));
  assert.equal(exported.equipment.length, 9);
  assert.equal(exported.borrowers.length, 7);
  assert.equal(exported.loans.length, 10);
  assert.equal("workspace_id" in exported.equipment[0], false);
  await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  await navigate("Overview");
  if (
    await page.getByRole("button", { name: "Dismiss notification", exact: true }).count()
  )
    await page.getByRole("button", { name: "Dismiss notification", exact: true }).click();
  for (const width of [320, 390, 768]) {
    await page.setViewportSize({ width, height: 844 });
    assert.equal(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
      true,
      `Overflow at ${width}px`,
    );
  }
  const other = await browser.newContext({
    viewport: { width: 1280, height: 900 },
    reducedMotion: "reduce",
  });
  const otherPage = await other.newPage();
  await otherPage.goto(base);
  await otherPage
    .getByRole("button", { name: "Explore the demo", exact: true })
    .waitFor();
  const privateList = await otherPage.request.get(new URL("api/equipment", base).href);
  assert.equal((await privateList.json()).totalItems, 0);
  await otherPage.getByRole("button", { name: "Explore the demo", exact: true }).click();
  await otherPage
    .getByRole("button", { name: "View Aputure Amaran", exact: true })
    .waitFor();
  await otherPage
    .getByRole("button", { name: "Open workspace settings", exact: true })
    .click();
  await otherPage.getByRole("button", { name: /Delete workspace/ }).click();
  await otherPage.getByLabel("Type DELETE to confirm", { exact: true }).fill("DELETE");
  await otherPage
    .getByRole("button", { name: "Delete all records", exact: true })
    .click();
  await otherPage
    .getByRole("button", { name: "Explore the demo", exact: true })
    .waitFor();
  assert.equal(
    (await (await otherPage.request.get(new URL("api/equipment", base).href)).json())
      .totalItems,
    0,
  );
  await other.close();
  assert.deepEqual(errors, []);
  console.log(
    "Browser journey passed: demo, equipment, borrowers, full checkout/return, stock reconciliation, edit, reload, export, private workspace isolation, deletion and responsive layouts.",
  );
} catch (error) {
  await page
    .screenshot({ path: "/private/tmp/fieldkit-browser-failure.png", fullPage: true })
    .catch(() => {});
  throw error;
} finally {
  await context.close();
  await browser.close();
}
