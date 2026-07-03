class BasePage {
  constructor(page) {
    this.page = page;
  }

  async navigateTo(path) {
    const url = path.startsWith('http') ? path : `http://localhost:5173${path}`;
    await this.page.goto(url);
  }

  async waitForPageLoad() {
    await this.page.waitForLoadState('load');
  }

  async getElement(selector) {
    return this.page.locator(selector);
  }

  async clickElement(selector) {
    await this.page.locator(selector).click();
  }

  async fillInput(selector, value) {
    await this.page.locator(selector).fill(value);
  }

  async waitForElement(selector, timeout = 10000) {
    await this.page.locator(selector).waitFor({ timeout });
  }

  async waitForResponse(urlPattern) {
    return await this.page.waitForResponse(urlPattern);
  }

  async takeScreenshot(name) {
    await this.page.screenshot({ path: `../test-results/screenshots/${name}.png` });
  }

  async waitForText(text, timeout = 10000) {
    await this.page.waitForSelector(`text=${text}`, { timeout });
  }

  async isElementVisible(selector) {
    return await this.page.locator(selector).isVisible();
  }

  async getElementText(selector) {
    return await this.page.locator(selector).textContent();
  }
}

export default BasePage;
