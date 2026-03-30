const { test, expect } = require('playwright/test');
const base = 'http://127.0.0.1:5173';
const demo = { email: 'demo@smartstock.ai', password: 'Passw0rd!' };
let context;
let page;
async function goto(path) { await page.goto(`${base}${path}`, { waitUntil: 'domcontentloaded' }); }
async function login() {
  await goto('/login');
  await page.locator('input[type="email"]').fill(demo.email);
  await page.locator('input[type="password"]').fill(demo.password);
  await page.getByRole('button', { name: '进入终端' }).click();
  await page.waitForURL(url => url.toString().includes('/dashboard'));
}
test.beforeAll(async ({ browser }) => {
  context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  page = await context.newPage();
  await login();
});
test.afterAll(async () => { await context?.close(); });
test('Terminal AI 发送恢复按钮状态', async () => {
  test.setTimeout(120000);
  await goto('/terminal/000001');
  await page.locator('button').filter({ hasText: '短线怎么看？' }).first().click();
  const textarea = page.locator('textarea').first();
  await expect(textarea).not.toHaveValue('');
  await page.getByRole('button', { name: '发送问题' }).click();
  await expect(page.getByRole('button', { name: '分析中' })).toBeVisible();
  await expect(page.getByRole('button', { name: '发送问题' })).toBeVisible({ timeout: 90000 });
  await expect(page.locator('article').first()).toBeVisible();
});
