(function (window, $) {
    'use strict';

    const TERMINAL = ['COMPLETED', 'FAILED', 'CANCELLED'];
    const statusText = {
        CREATED: '已创建', PLANNING: '规划中', WAITING_CONFIRMATION: '等待确认',
        EXECUTING: '执行中', COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消'
    };
    const failureText = {
        EXECUTION_TIME_LIMIT_EXCEEDED: '执行时间超过上限',
        TOKEN_BUDGET_EXCEEDED: 'Token 预算已用尽',
        TOOL_CALL_LIMIT_EXCEEDED: '工具调用次数超过上限',
        DUPLICATE_TOOL_CALL: '检测到重复工具调用',
        RUN_CANCELLED: '任务已被取消',
        'Cancelled by user': '由你主动取消'
    };

    let options = {};
    let currentRun = null;
    let pollTimer = null;

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
    }

    function apiError(xhr, fallback) {
        return (xhr && xhr.responseJSON && xhr.responseJSON.status && xhr.responseJSON.status.msg) || fallback;
    }

    function unwrap(response) {
        if (!response || !response.status || response.status.code !== 0) {
            throw new Error((response && response.status && response.status.msg) || '服务返回异常');
        }
        return response.result;
    }

    function setStreamState(kind, text) {
        $('#agent-stream-state').attr('data-state', kind).find('.agent-stream-state-text').text(text);
    }

    function setPanelMessage(text, kind) {
        $('#agent-workbench-message').attr('data-kind', kind || 'info').text(text || '').toggle(Boolean(text));
    }

    function formatTime(value) {
        if (!value) return '—';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? escapeHtml(value) : date.toLocaleString('zh-CN', {hour12: false});
    }

    function isTerminal(status) {
        return TERMINAL.indexOf(status) >= 0;
    }

    function renderRecent(runs) {
        const list = $('#agent-run-list');
        if (!runs || !runs.length) {
            list.html('<div class="agent-empty">还没有 Agent Run。发送知识问答、Bug 排查或任务规划后会出现在这里。</div>');
            return;
        }
        list.html(runs.map(function (run) {
            const active = currentRun && currentRun.runId === run.runId ? ' is-active' : '';
            return '<button type="button" class="agent-run-item' + active + '" data-run-id="' + escapeHtml(run.runId) + '">'
                + '<span class="agent-run-item-main"><strong>' + escapeHtml(run.goal || '未命名任务') + '</strong>'
                + '<small>' + escapeHtml(run.mode || 'AGENT') + ' · ' + formatTime(run.startTime) + '</small></span>'
                + '<span class="agent-status agent-status-' + escapeHtml((run.status || '').toLowerCase()) + '">' + escapeHtml(statusText[run.status] || run.status) + '</span>'
                + '</button>';
        }).join(''));
    }

    function renderSteps(steps, terminal) {
        const container = $('#agent-run-steps');
        if (!steps || !steps.length) {
            container.html('<div class="agent-empty">' + (terminal ? '本次 Run 没有产生工具步骤。' : '正在等待第一个执行步骤…') + '</div>');
            return;
        }
        container.html(steps.map(function (step) {
            const summary = step.resultSummary || (step.errorType ? ('错误：' + step.errorType) : '等待结果');
            return '<div class="agent-step">'
                + '<span class="agent-step-index">' + escapeHtml(step.stepNo || '·') + '</span>'
                + '<div class="agent-step-body"><div><strong>' + escapeHtml(step.toolName || step.type || '步骤') + '</strong>'
                + '<span class="agent-status agent-status-' + escapeHtml((step.status || '').toLowerCase()) + '">' + escapeHtml(statusText[step.status] || step.status || '未知') + '</span></div>'
                + '<p>' + escapeHtml(summary) + '</p>'
                + (step.durationMs == null ? '' : '<small>耗时 ' + escapeHtml(step.durationMs) + ' ms</small>')
                + '</div></div>';
        }).join(''));
    }

    function renderEvidence(evidence, terminal) {
        const container = $('#agent-run-evidence');
        if (!evidence || !evidence.length) {
            container.html('<div class="agent-empty">' + (terminal ? '未检索到可引用的站内证据，本次回答不应被当作有来源结论。' : '正在检索相关证据…') + '</div>');
            return;
        }
        container.html(evidence.map(function (item) {
            const href = item.articleId ? '/article/detail/' + encodeURIComponent(item.articleId) : '';
            const title = escapeHtml(item.title || ('文章 #' + item.articleId));
            return '<article class="agent-evidence">'
                + (href ? '<a href="' + href + '" target="_blank" rel="noopener noreferrer">' + title + '</a>' : '<strong>' + title + '</strong>')
                + '<p>' + escapeHtml(item.evidenceSummary || item.excerpt || '暂无摘录') + '</p>'
                + (item.relevance == null ? '' : '<small>相关度 ' + Math.round(Number(item.relevance) * 100) + '%</small>')
                + '</article>';
        }).join(''));
    }

    function renderRun(run) {
        currentRun = run;
        const terminal = isTerminal(run.status);
        $('#agent-current-empty').hide();
        $('#agent-current-run').show();
        $('#agent-current-title').text(run.goal || '未命名任务');
        $('#agent-current-status').attr('class', 'agent-status agent-status-' + String(run.status || '').toLowerCase()).text(statusText[run.status] || run.status);
        $('#agent-current-meta').text((run.mode || 'AGENT') + ' · Run #' + run.runId + ' · ' + (run.model || '默认模型'));
        $('#agent-current-budget').text('工具 ' + (run.toolCallCount || 0) + '/' + (run.maxToolCalls || '—')
            + ' · Token ' + (run.totalTokenCount || 0) + '/' + (run.maxTokenBudget || '—'));
        const failure = run.failureReason ? (failureText[run.failureReason] || run.failureReason) : '';
        $('#agent-current-failure').text(failure).toggle(Boolean(failure));
        $('#agent-cancel-run').toggle(!terminal);
        $('#agent-retry-run').toggle(run.status === 'FAILED' || run.status === 'CANCELLED');
        renderSteps(run.steps, terminal);
        renderEvidence(run.evidence, terminal);
        $('.agent-run-item').removeClass('is-active').filter('[data-run-id="' + run.runId + '"]').addClass('is-active');
        if (terminal) {
            stopPolling();
            setStreamState(run.status === 'COMPLETED' ? 'complete' : 'interrupted', statusText[run.status] || run.status);
        } else {
            schedulePoll(run.runId);
        }
    }

    function loadRun(runId, quiet) {
        if (!runId) return;
        if (!quiet) setPanelMessage('正在加载 Run #' + runId + '…', 'info');
        $.ajax({url: '/agent-run/api/' + encodeURIComponent(runId), method: 'GET', dataType: 'json'})
            .done(function (response) {
                try {
                    renderRun(unwrap(response));
                    setPanelMessage('', 'info');
                } catch (error) {
                    setPanelMessage(error.message, 'error');
                }
            }).fail(function (xhr) {
                setPanelMessage(apiError(xhr, '网络异常，Agent Run 详情加载失败。'), 'error');
                stopPolling();
            });
    }

    function loadRecent() {
        if (!options.isLogin) return;
        $('#agent-run-list').attr('aria-busy', 'true');
        $.ajax({url: '/agent-run/api', method: 'GET', data: {limit: 12}, dataType: 'json'})
            .done(function (response) {
                try {
                    const runs = unwrap(response) || [];
                    renderRecent(runs);
                    if (!currentRun && runs.length) loadRun(runs[0].runId, true);
                } catch (error) {
                    setPanelMessage(error.message, 'error');
                }
            }).fail(function (xhr) {
                setPanelMessage(apiError(xhr, '网络异常，历史 Agent Run 加载失败。'), 'error');
            }).always(function () {
                $('#agent-run-list').removeAttr('aria-busy');
            });
    }

    function stopPolling() {
        if (pollTimer) window.clearTimeout(pollTimer);
        pollTimer = null;
    }

    function schedulePoll(runId) {
        stopPolling();
        pollTimer = window.setTimeout(function () { loadRun(runId, true); }, options.pollInterval || 1800);
    }

    function onAnswer(answer) {
        if (!answer) return;
        if (answer.answerType === 'STREAM') setStreamState('streaming', '正在接收流式回答…');
        if (answer.answerType === 'STREAM_END') setStreamState('complete', '回答已完整接收');
        if (answer.citations && answer.citations.length && (!currentRun || !currentRun.evidence || !currentRun.evidence.length)) {
            renderEvidence(answer.citations.map(function (citation) {
                return {articleId: citation.articleId, title: citation.title, evidenceSummary: citation.excerpt, relevance: citation.relevance};
            }), answer.answerType === 'STREAM_END');
        }
        if (answer.agentRunId) {
            if (!currentRun || currentRun.runId !== answer.agentRunId || answer.answerType === 'STREAM_END') {
                loadRun(answer.agentRunId, true);
            }
            if (answer.answerType !== 'STREAM') loadRecent();
        }
    }

    function init(config) {
        options = config || {};
        $('#agent-workbench-toggle').attr('aria-expanded', String(!(window.matchMedia && window.matchMedia('(max-width: 900px)').matches)));
        $('#agent-workbench-toggle').on('click', function () {
            const panel = $('#agent-workbench');
            const mobile = window.matchMedia && window.matchMedia('(max-width: 900px)').matches;
            const currentlyOpen = mobile ? panel.hasClass('is-open') : !panel.hasClass('is-collapsed');
            const open = !currentlyOpen;
            panel.toggleClass('is-open', open).toggleClass('is-collapsed', !open);
            $(this).attr('aria-expanded', String(open));
        });
        $('#agent-workbench-close').on('click', function () {
            $('#agent-workbench').removeClass('is-open').addClass('is-collapsed');
            $('#agent-workbench-toggle').attr('aria-expanded', 'false');
        });
        $('#agent-refresh-runs').on('click', loadRecent);
        $('#agent-run-list').on('click', '.agent-run-item', function () { loadRun($(this).data('runId'), false); });
        $('#agent-cancel-run').on('click', function () {
            if (!currentRun || !window.confirm('确认取消当前 Agent Run 吗？已经完成的步骤会保留。')) return;
            const button = $(this).prop('disabled', true).text('取消中…');
            $.ajax({url: '/agent-run/api/' + encodeURIComponent(currentRun.runId) + '/cancel', method: 'PUT', dataType: 'json'})
                .done(function (response) {
                    try { unwrap(response); loadRun(currentRun.runId, true); loadRecent(); }
                    catch (error) { setPanelMessage(error.message, 'error'); }
                }).fail(function (xhr) {
                    setPanelMessage(apiError(xhr, '网络异常，取消操作未完成。'), 'error');
                }).always(function () { button.prop('disabled', false).text('取消 Run'); });
        });
        $('#agent-retry-run').on('click', function () {
            if (currentRun && typeof options.retry === 'function') options.retry(currentRun);
        });
        if (options.isLogin) loadRecent();
        else $('#agent-run-list').html('<div class="agent-empty">登录后可查看 Agent Run 历史。</div>');
    }

    window.CodeMateAgentWorkbench = {
        init: init,
        onAnswer: onAnswer,
        loadRecent: loadRecent,
        streamStarted: function () { setStreamState('streaming', '请求已发送，等待模型响应…'); },
        connected: function () { setStreamState('ready', '连接正常，可以开始任务'); },
        interrupted: function () { setStreamState('interrupted', '流式连接已中断，请重连后重试'); stopPolling(); }
    };
})(window, window.jQuery);
