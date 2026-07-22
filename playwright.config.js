const os = require('os');
const path = require('path');
const {defineConfig, devices} = require('playwright/test');

module.exports = defineConfig({
    testDir: './frontend-e2e',
    fullyParallel: true,
    forbidOnly: Boolean(process.env.CI),
    retries: process.env.CI ? 1 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: [['list']],
    outputDir: process.env.CODEMATE_E2E_OUTPUT_DIR || path.join(os.tmpdir(), 'codemate-playwright-results'),
    use: {
        ...devices['Desktop Chrome'],
        launchOptions: process.env.CODEMATE_CHROME_PATH ? {executablePath: process.env.CODEMATE_CHROME_PATH} : {},
        headless: true,
        screenshot: 'only-on-failure',
        trace: 'retain-on-failure'
    },
    projects: [
        {name: 'chromium', use: {...devices['Desktop Chrome']}}
    ]
});
