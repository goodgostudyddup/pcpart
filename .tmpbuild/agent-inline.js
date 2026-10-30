const agentState = { base: '/assistant-api', busy: false, sessionId: null, opening: false };

function toggleAgent() {
    const panel = document.getElementById('agentPanel');
    const fab = document.getElementById('agentFab');
    const willOpen = panel.hidden;
    panel.hidden = !willOpen;
    fab.setAttribute('aria-expanded', String(willOpen));
    if (willOpen) {
        if (!agentState.sessionId) agentNewSession();
        else agentPing(false);
        document.getElementById('agentInput').focus();
    }
}

function agentMsg(kind, text) {
    const box = document.getElementById('agentMsgs');
    const div = document.createElement('div');
    div.className = 'agent-msg ' + kind;
    div.textContent = text;
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
    return div;
}

function agentRenderBackendData(data) {
    if (!data || !Array.isArray(data.parts) || !data.parts.length) return;
    const box = document.createElement('div');
    box.className = 'agent-msg assistant';
    const heading = document.createElement('strong');
    heading.textContent = data.summary && data.summary.fullyVerified ? '后端配置建议（兼容性通过）' : '后端检索结果（部分内容待确认）';
    box.appendChild(heading);
    const list = document.createElement('div');
    data.parts.forEach(part => {
        const row = document.createElement('div');
        row.className = 'agent-pick';
        const b = document.createElement('b');
        const labels = {gpu:'显卡',cpu:'CPU',motherboard:'主板',ram:'内存',storage:'存储',psu:'电源',cooler:'散热器',case:'机箱'};
        b.textContent = (labels[part.category] || part.category) + '：';
        const span = document.createElement('span');
        span.textContent = (part.name || '后端未提供名称') + (part.priceCny != null ? ' · ¥' + part.priceCny : ' · 未提供价格') + (part.budgetShare != null ? ' · 占预算 ' + part.budgetShare + '%' : '');
        row.appendChild(b); row.appendChild(span);
        const id = part.fields && (part.fields.opendb_id || part.fields.opendbId) || part.id;
        if (id != null) {
            const actions = document.createElement('div'); actions.className = 'agent-actions';
            const locate = document.createElement('button'); locate.type = 'button'; locate.className = 'op ghost'; locate.textContent = '在配件表定位';
            locate.onclick = () => agentFillPart(part.category, String(id)); actions.appendChild(locate);
            list.appendChild(row); list.appendChild(actions);
        } else list.appendChild(row);
    });
    box.appendChild(list);
    if (data.summary) {
        const total = document.createElement('div'); total.className = 'agent-total';
        if (data.summary.completePriceTotal && data.summary.remainingCny != null) {
            total.textContent = '整机合计 ¥' + Number(data.summary.totalPriceCny).toLocaleString('zh-CN') + ' · 余额 ¥' + Number(data.summary.remainingCny).toLocaleString('zh-CN') + (data.summary.withinBudget ? ' · 未超预算' : ' · 超出预算');
        } else {
            total.textContent = '已返回配件小计 ¥' + Number(data.summary.totalPriceCny || 0).toLocaleString('zh-CN') + ' · 缺少配件或价格，整机预算状态未核算';
        }
        box.appendChild(total);
    }
    if (data.compatibility) {
        const compat = document.createElement('div'); compat.className = 'agent-total';
        const complete = !!(data.summary && data.summary.fullyVerified);
        const status = complete ? (data.compatibility.compatible ? '完整校验通过' : '完整校验未通过') : (data.compatibility.compatible ? '部分规则通过；配置不完整/有警告，不能视为整机校验通过' : '尚未获得完整校验结论');
        compat.textContent = '后端兼容性：' + status + (data.compatibility.violations && data.compatibility.violations.length ? '；' + data.compatibility.violations.map(v => v.message || v.ruleId).join('；') : '');
        box.appendChild(compat);
    }
    if (data.notices && data.notices.length) {
        const notices = document.createElement('div'); notices.className = 'agent-total'; notices.textContent = '提示：' + data.notices.join('；'); box.appendChild(notices);
    }
    if (data.summary && data.summary.fullyVerified) {
        const save = document.createElement('button'); save.type = 'button'; save.className = 'op'; save.textContent = '保存为配置单';
        save.style.marginTop = '8px'; save.onclick = () => agentSaveRecommendation(data); box.appendChild(save);
    }
    document.getElementById('agentMsgs').appendChild(box);
    document.getElementById('agentMsgs').scrollTop = document.getElementById('agentMsgs').scrollHeight;
}

async function agentSaveRecommendation(data) {
    const summary = data && data.summary;
    if (!summary || !Array.isArray(data.parts) || !data.parts.length) return;
    const build = {};
    data.parts.forEach(p => {
        const id = p.fields && (p.fields.opendb_id || p.fields.opendbId) || p.id;
        const field = {cpu:'cpuId',cooler:'coolerId',motherboard:'motherboardId',ram:'ramId',gpu:'gpuId',psu:'psuId',storage:'storageId',case:'caseId'}[p.category];
        if (field && id != null) build[field] = String(id);
    });
    const yes = confirm('将后端返回的配件 ID 交给 Java 后端再次校验并保存为配置单？');
    if (!yes) return;
    const configName = prompt('配置单名称：', 'AI 助手推荐配置');
    if (configName === null || !configName.trim()) return;
    try {
        const result = await fetch('/internal/build-configs/save', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({configName:configName.trim(),...build,remark:'由 Python Agent / DeepSeek 推荐'})}).then(x => x.json());
        if (result.code !== 200) throw new Error(result.msg || '保存失败');
        agentMsg('assistant', '已由 Java 后端再次校验并保存配置单，编号 ' + result.data.id);
        if (cur === 'build-configs') doSearch();
    } catch (error) { agentMsg('error', '配置保存失败：' + String(error.message || error)); }
}

function agentSetDot(ok) {
    const dot = document.getElementById('agentDot');
    dot.classList.remove('ok', 'bad');
    dot.classList.add(ok ? 'ok' : 'bad');
}

function agentSetBusy(busy) {
    agentState.busy = busy;
    document.getElementById('agentSend').disabled = busy;
    document.getElementById('agentSend').textContent = busy ? '发送中…' : '发送';
}

async function agentRequest(path, options = {}) {
    const response = await fetch(agentState.base + path, {
        ...options,
        headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.headers || {}) }
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new Error((data && (data.error || data.msg)) || ('HTTP ' + response.status));
    return data;
}

async function agentPing(loud) {
    try {
        const data = await agentRequest('/health');
        const ok = data && data.status === 'ok';
        agentSetDot(!!ok);
        if (loud) agentMsg(ok ? 'sys' : 'error', ok ? '助手服务已连接' : '助手健康检查失败');
        return !!ok;
    } catch (error) {
        agentSetDot(false);
        if (loud) agentMsg('error', '无法连接助手服务：' + String(error.message || error));
        return false;
    }
}

async function agentNewSession() {
    if (agentState.opening) return;
    agentState.opening = true;
    agentState.sessionId = null;
    document.getElementById('agentMsgs').replaceChildren();
    try {
        const data = await agentRequest('/sessions', { method: 'POST', body: '{}' });
        agentState.sessionId = data.sessionId;
        agentSetDot(true);
        agentMsg('assistant', data.reply || '先告诉我你的预算、主要用途和硬件偏好。');
    } catch (error) {
        agentSetDot(false);
        agentMsg('error', '创建助手会话失败：' + String(error.message || error) + '。确认 Python Agent 已启动后点“新对话”重试。');
    } finally {
        agentState.opening = false;
    }
}

function agentQuick(text) {
    document.getElementById('agentInput').value = text;
    agentChat();
}

async function agentChat() {
    if (agentState.busy) return;
    const input = document.getElementById('agentInput');
    const message = (input.value || '').trim();
    if (!message) return;
    if (!agentState.sessionId) {
        await agentNewSession();
        if (!agentState.sessionId) return;
    }
    input.value = '';
    agentMsg('user', message);
    agentSetBusy(true);
    const pending = agentMsg('sys', '正在查后端数据并整理回答…');
    try {
        const data = await agentRequest('/sessions/' + encodeURIComponent(agentState.sessionId) + '/chat', {
            method: 'POST', body: JSON.stringify({ message })
        });
        pending.remove();
        if (data.sessionId) agentState.sessionId = data.sessionId;
        agentSetDot(true);
        agentMsg(data.error ? 'error' : 'assistant', data.reply || '助手没有返回文本');
        agentRenderBackendData(data.backendData);
    } catch (error) {
        pending.remove();
        agentSetDot(false);
        agentMsg('error', '发送失败：' + String(error.message || error) + '。请确认 Agent 和 Java 后端均已启动，然后点击“新对话”重试。');
    } finally {
        agentSetBusy(false);
        input.focus();
    }
}

document.getElementById('agentInput').addEventListener('keydown', function(event) {
    if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); agentChat(); }
});

