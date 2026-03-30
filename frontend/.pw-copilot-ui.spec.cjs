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
test('Copilot 新建对话与历史切换', async () => {
  test.setTimeout(120000);
  await goto('/copilot/000001');
  await page.getByRole('button', { name: '新建对话' }).first().click();
  await expect(page.locator('aside .text-sm.font-semibold.text-white').filter({ hasText: '新对话' })).toBeVisible();
  await page.locator('aside').getByRole('button').filter({ hasText: '现在更适合观望还是分批介入？' }).first().click();
  await page.getByRole('button', { name: '发送给 AI' }).click();
  await expect(page.getByText('AI 思考中')).toBeVisible();
  await expect(page.getByRole('button', { name: '发送给 AI' })).toBeVisible({ timeout: 90000 });
  await expect(page.locator('section').getByText('这是当前新建对话，会随你的继续追问向下展开。').first()).toBeVisible();
  const archiveButton = page.locator('aside').getByRole('button').filter({ hasText: '打开' }).first();
  await archiveButton.click();
  await expect(page.locator('section').getByText('正在查看').first()).toBeVisible();
  await page.getByRole('button', { name: '新建对话' }).first().click();
  await expect(page.locator('section').getByText('点击快捷问题或直接输入，开始一段新的问答。').first()).toBeVisible();
});
