import { expect, test } from '@playwright/test';

test('restock increments inventory and stays usable across viewport widths', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('http://127.0.0.1:18080/');
  await expect(page.locator('#store')).toHaveText('memory');

  for (const selector of ['#restockProduct', '#restockQty']) {
    const height = await page.locator(selector).evaluate(element => element.getBoundingClientRect().height);
    expect(height, `${selector} tap target height`).toBeGreaterThanOrEqual(44);
  }

  const product = page.locator('#products tr').filter({ hasText: 'SKU-001' });
  const stockCell = product.locator('td').nth(2);
  const initialStock = Number(await stockCell.innerText());

  await page.getByLabel('Produk untuk restock').selectOption('SKU-001');
  await page.getByLabel('Jumlah stok masuk').fill('0');
  await page.getByRole('button', { name: 'Tambah stok', exact: true }).click();
  await expect(page.locator('#restockStatus')).toContainText('Jumlah stok harus berupa bilangan bulat lebih dari nol');
  await expect(stockCell).toHaveText(String(initialStock));

  await page.getByLabel('Jumlah stok masuk').fill('3');
  await page.getByRole('button', { name: 'Tambah stok', exact: true }).click();

  await expect(page.locator('#restockStatus')).toContainText('Stok berhasil ditambahkan');
  await expect(stockCell).toHaveText(String(initialStock + 3));
  await expect(product.locator('input[data-sku="SKU-001"]')).toHaveAttribute('max', String(initialStock + 3));

  for (const width of [320, 390, 560, 768, 1024, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const overflow = await page.evaluate(() =>
      document.documentElement.scrollWidth > document.documentElement.clientWidth);
    expect(overflow, `horizontal overflow at ${width}px`).toBe(false);
  }
});

test('data views show loading, empty, and error states', async ({ page }) => {
  await page.route('**/api/products', async route => {
    await new Promise(resolve => setTimeout(resolve, 300));
    await route.continue();
  });
  await page.goto('/');
  await expect(page.locator('#products')).toContainText('Memuat produk...');
  await expect(page.locator('#orders')).toContainText('Memuat pesanan...');
  await expect(page.locator('#notif')).toContainText('Memuat notifikasi...');
  await expect(page.locator('#log')).toContainText('Memuat log transaksi...');
  await expect(page.locator('#products')).toContainText('Kopi Arabika');

  await page.unroute('**/api/products');
  await page.route('**/api/products', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: '[]',
  }));
  await page.reload();
  await expect(page.locator('#products')).toContainText('Belum ada produk.');
  await expect(page.locator('#orders')).toContainText('Belum ada pesanan.');
  await expect(page.locator('#notif')).toContainText('Belum ada notifikasi.');
  await expect(page.locator('#log')).toContainText('Belum ada log transaksi.');
  await expect(page.locator('#restockProduct')).toBeDisabled();
  await expect(page.locator('#btnRestock')).toBeDisabled();

  await page.unroute('**/api/products');
  await page.route('**/api/products', route => route.fulfill({
    status: 503,
    contentType: 'application/json',
    body: JSON.stringify({ error: 'service unavailable' }),
  }));
  await page.reload();
  await expect(page.locator('#products')).toContainText('Produk gagal dimuat.');
  await expect(page.locator('#orders')).toContainText('Pesanan gagal dimuat.');
  await expect(page.locator('#notif')).toContainText('Notifikasi gagal dimuat.');
  await expect(page.locator('#log')).toContainText('Log transaksi gagal dimuat.');
});
