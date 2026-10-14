import { expect, type Page } from "@playwright/test";

export async function elegirSelector(page: Page, nombre: string, valor: string, busqueda?: string) {
  const control = page.getByRole("combobox", { name: nombre, exact: true });
  await control.click();
  if (busqueda !== undefined) await control.fill(busqueda);
  await page.getByRole("listbox").locator(`[role="option"][data-valor="${valor}"]`).click();
  await expect(control).toHaveAttribute("data-valor", valor);
}
