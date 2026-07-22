const fs = require('fs');
const path = require('path');
const {test, expect} = require('playwright/test');

const root = path.resolve(__dirname, '..');
const fixture = fs.readFileSync(path.join(__dirname, 'fixtures', 'agent-workbench.html'), 'utf8');
const jquery = fs.readFileSync(path.join(root, 'paicoding-ui/src/main/resources/static/js/jquery.min.js'), 'utf8');
const workbench = fs.readFileSync(path.join(root, 'paicoding-ui/src/main/resources/static/js/views/chat-agent-workbench.js'), 'utf8');
const styles = fs.readFileSync(path.join(root, 'paicoding-ui/src/main/resources/static/css/views/chat-home.css'), 'utf8');

const runs = [
    {runId: 101, goal: '定位支付超时', mode: 'BUG_DIAGNOSIS', status: 'FAILED', startTime: '2026-07-22T10:00:00Z'},
    {runId: 102, goal: '整理缓存文章', mode: 'KNOWLEDGE_QA', status: 'COMPLETED', startTime: '2026-07-22T09:00:00Z'},
    {runId: 103, goal: '检查执行中的任务', mode: 'CHAT', status: 'EXECUTING', startTime: '2026-07-22T08:00:00Z'}
];

function detail(runId, status) {
    const run = runs.find(item => item.runId === Number(runId));
    return {
        ...run,
        status: status || run.status,
        model: 'deepseek-chat',
        toolCallCount: 1,
        maxToolCalls: 5,
        totalTokenCount: 320,
        maxTokenBudget: 4000,
        failureReason: run.runId === 101 ? 'EXECUTION_TIME_LIMIT_EXCEEDED' : null,
        steps: [{
            stepNo: 1,
            toolName: 'article_search',
            status: 'COMPLETED',
            resultSummary: run.runId === 102 ? '找到 3 篇缓存文章' : '定位到超时调用链',
            argumentSummary: 'secret-token-must-never-render',
            durationMs: 28
        }],
        evidence: [{articleId: 88, title: '超时排查指南', evidenceSummary: '检查连接池与下游耗时', relevance: 0.92}]
    };
}

function ok(result) {
    return {status: {code: 0, msg: 'ok'}, result};
}

async function capture(page, name) {
    const directory = process.env.CODEMATE_E2E_SCREENSHOT_DIR;
    if (!directory) return;
    fs.mkdirSync(directory, {recursive: true});
    await page.screenshot({path: path.join(directory, name), fullPage: true});
}

async function mount(page, options = {}) {
    const calls = [];
    let cancelled = false;
    await page.route('http://codemate.test/**', async route => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname === '/chat') return route.fulfill({contentType: 'text/html; charset=utf-8', body: fixture});
        if (url.pathname === '/assets/jquery.js') return route.fulfill({contentType: 'application/javascript', body: jquery});
        if (url.pathname === '/assets/chat-agent-workbench.js') return route.fulfill({contentType: 'application/javascript', body: workbench});
        if (url.pathname === '/assets/chat-home.css') return route.fulfill({contentType: 'text/css', body: styles});
        if (url.pathname === '/agent-run/api' && request.method() === 'GET') {
            if (options.listError) {
                return route.fulfill({status: 503, contentType: 'application/json', body: JSON.stringify({status: {code: 503, msg: '历史服务暂不可用'}})});
            }
            return route.fulfill({contentType: 'application/json', body: JSON.stringify(ok(runs))});
        }
        const match = url.pathname.match(/^\/agent-run\/api\/(\d+)(\/cancel)?$/);
        if (match && match[2] && request.method() === 'PUT') {
            calls.push({method: request.method(), path: url.pathname});
            cancelled = true;
            return route.fulfill({contentType: 'application/json', body: JSON.stringify(ok(true))});
        }
        if (match && request.method() === 'GET') {
            return route.fulfill({contentType: 'application/json', body: JSON.stringify(ok(detail(match[1], cancelled && match[1] === '103' ? 'CANCELLED' : undefined)))});
        }
        return route.fulfill({status: 404, body: 'not found'});
    });
    await page.goto('http://codemate.test/chat');
    if (!options.listError) await expect(page.locator('#agent-current-run')).toBeVisible();
    return calls;
}

test('renders run details, safe tool summaries, and evidence from production workbench code', async ({page}) => {
    await mount(page);

    await expect(page.locator('#agent-current-title')).toHaveText('定位支付超时');
    await expect(page.locator('#agent-run-steps')).toContainText('article_search');
    await expect(page.locator('#agent-run-steps')).toContainText('定位到超时调用链');
    await expect(page.locator('#agent-run-evidence')).toContainText('超时排查指南');
    await expect(page.locator('#agent-run-evidence a')).toHaveAttribute('href', '/article/detail/88');
    await expect(page.locator('body')).not.toContainText('secret-token-must-never-render');
    await capture(page, 'codemate-agent-workbench-desktop.png');
});

test('switches history and restores a failed run through the retry callback', async ({page}) => {
    await mount(page);

    await page.locator('[data-run-id="102"]').click();
    await expect(page.locator('#agent-current-title')).toHaveText('整理缓存文章');
    await expect(page.locator('#agent-run-steps')).toContainText('找到 3 篇缓存文章');

    await page.locator('[data-run-id="101"]').click();
    await page.locator('#agent-retry-run').click();
    await expect(page.locator('#agent-mode')).toHaveValue('BUG_DIAGNOSIS');
    await expect(page.locator('#input-field')).toHaveValue('定位支付超时');
    await expect.poll(() => page.evaluate(() => window.__retryRun && window.__retryRun.runId)).toBe(101);
});

test('confirms cancellation, sends PUT, and refreshes the terminal state', async ({page}) => {
    const calls = await mount(page);
    await page.locator('[data-run-id="103"]').click();
    await expect(page.locator('#agent-cancel-run')).toBeVisible();
    page.once('dialog', dialog => dialog.accept());
    await page.locator('#agent-cancel-run').click();

    await expect.poll(() => calls.length).toBe(1);
    expect(calls[0]).toEqual({method: 'PUT', path: '/agent-run/api/103/cancel'});
    await expect(page.locator('#agent-cancel-run')).toBeHidden();
    await expect(page.locator('#agent-stream-state')).toHaveAttribute('data-state', 'interrupted');
});

test('opens and closes the responsive workbench drawer on mobile', async ({page}) => {
    await page.setViewportSize({width: 390, height: 844});
    await mount(page);
    const panel = page.locator('#agent-workbench');

    await page.locator('#agent-workbench-toggle').click();
    await expect(panel).toHaveClass(/is-open/);
    await expect(page.locator('#agent-workbench-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(panel).toHaveCSS('transform', 'matrix(1, 0, 0, 1, 0, 0)');
    await capture(page, 'codemate-agent-workbench-mobile.png');

    await page.locator('#agent-workbench-close').click();
    await expect(panel).toHaveClass(/is-collapsed/);
    await expect(page.locator('#agent-workbench-toggle')).toHaveAttribute('aria-expanded', 'false');
});

test('shows an explicit message when run history cannot be loaded', async ({page}) => {
    await mount(page, {listError: true});
    await expect(page.locator('#agent-workbench-message')).toBeVisible();
    await expect(page.locator('#agent-workbench-message')).toContainText('历史服务暂不可用');
    await expect(page.locator('#agent-workbench-message')).toHaveAttribute('data-kind', 'error');
});
