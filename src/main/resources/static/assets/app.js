const page = document.body.dataset.page;
const state = {
  user: null,
  resumes: [],
  jobs: [],
  analyses: [],
  activeResumeId: null,
  activeAnalysisId: null,
  pollTimer: null
};

const $ = (id) => document.getElementById(id);

function has(id) {
  return Boolean($(id));
}

function setText(id, value) {
  const element = $(id);
  if (element) element.textContent = cleanDisplayText(value);
}

function setHtml(id, value) {
  const element = $(id);
  if (element) element.innerHTML = value;
}

function escapeHtml(value) {
  return cleanDisplayText(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function cleanDisplayText(value) {
  return String(value ?? "")
    .replace(/\r\n/g, "\n")
    .replace(/```[a-zA-Z0-9_-]*\n?/g, "")
    .replace(/```/g, "")
    .replace(/(^|\n)\s{0,3}#{1,6}\s*/g, "$1")
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/__([^_]+)__/g, "$1")
    .replace(/\*\*/g, "")
    .replace(/#{3,}/g, "")
    .trim();
}

function fmt(value) {
  return value ? String(value).replace("T", " ").slice(0, 19) : "-";
}

function normalizeValidationMessage(details) {
  const first = details[0] || {};
  const loc = Array.isArray(first.loc) ? first.loc[first.loc.length - 1] : "";
  if (loc === "username") return "用户名需为 3 到 40 位，只能包含字母、数字、下划线、@、. 或 -";
  if (loc === "password") return "密码需为 6 到 120 位";
  return first.msg || "请求参数有误，请检查后重试";
}

function normalizeApiMessage(message) {
  if (Array.isArray(message)) return normalizeValidationMessage(message);
  if (message && typeof message === "object") {
    return message.msg ? normalizeApiMessage(message.msg) : "请求参数有误，请检查后重试";
  }
  const text = String(message || "").trim();
  const messages = {
    "bad username or password": "用户名或密码错误",
    "username already exists": "用户名已存在",
    "invalid username": "用户名格式不正确",
    "Not authenticated": "请先登录",
    Unauthorized: "请先登录",
    "Internal Server Error": "服务暂时不可用，请稍后重试",
    "page not found": "页面不存在"
  };
  return messages[text] || text || "操作失败，请稍后重试";
}

function showToast(message) {
  const toast = $("toast");
  if (!toast) return;
  toast.textContent = normalizeApiMessage(message);
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2800);
}

async function api(path, options = {}) {
  const { headers: customHeaders, ...rest } = options;
  const isForm = rest.body instanceof FormData;
  const headers = isForm
    ? { ...(customHeaders || {}) }
    : { "Content-Type": "application/json", ...(customHeaders || {}) };

  let response;
  try {
    response = await fetch(path, { ...rest, headers, credentials: "same-origin" });
  } catch (_) {
    throw new Error("服务暂时不可用，请稍后重试");
  }
  const rawBody = await response.text();
  let body = null;
  if (rawBody) {
    try {
      body = JSON.parse(rawBody);
    } catch (_) {
      body = rawBody;
    }
  }

  if (!response.ok) {
    if (response.status === 401 && page !== "auth") redirectToAuth();
    const detail = body && typeof body === "object" ? body.message || body.detail : body;
    throw new Error(normalizeApiMessage(detail || response.statusText));
  }
  return body;
}

function redirectToAuth() {
  window.location.href = "/";
}

function setButtonLoading(button, label) {
  if (!button) return () => {};
  const previous = button.textContent;
  button.disabled = true;
  button.textContent = label;
  return () => {
    button.disabled = false;
    button.textContent = previous;
  };
}

function setFieldError(inputId, message) {
  const input = $(inputId);
  const error = $(`${inputId}Error`);
  if (!input) return;
  if (message) input.setAttribute("aria-invalid", "true");
  else input.removeAttribute("aria-invalid");
  if (error) {
    error.textContent = message || "";
    error.classList.toggle("show", Boolean(message));
  }
}

function clearAuthErrors(mode) {
  const ids = mode === "register"
    ? ["registerUsername", "registerPassword"]
    : ["loginUsername", "loginPassword"];
  ids.forEach((id) => setFieldError(id, ""));
}

function readAuthValues(mode) {
  const isRegister = mode === "register";
  const usernameId = isRegister ? "registerUsername" : "loginUsername";
  const passwordId = isRegister ? "registerPassword" : "loginPassword";
  const username = $(usernameId).value.trim();
  const normalizedUsername = username.toLowerCase();
  const password = $(passwordId).value;
  const usernameRule = /^[a-z0-9_@.\-]{3,40}$/;
  let firstInvalidId = "";

  clearAuthErrors(mode);
  if (!username) {
    setFieldError(usernameId, isRegister ? "请设置用户名" : "请输入用户名");
    firstInvalidId = usernameId;
  } else if (username.length < 3 || username.length > 40) {
    setFieldError(usernameId, "用户名需为 3 到 40 位");
    firstInvalidId = usernameId;
  } else if (!usernameRule.test(normalizedUsername)) {
    setFieldError(usernameId, "用户名只能包含字母、数字、下划线、@、. 或 -");
    firstInvalidId = usernameId;
  }

  if (!password.trim()) {
    setFieldError(passwordId, isRegister ? "请设置密码" : "请输入密码");
    if (!firstInvalidId) firstInvalidId = passwordId;
  } else if (password.length < 6 || password.length > 120) {
    setFieldError(passwordId, "密码需为 6 到 120 位");
    if (!firstInvalidId) firstInvalidId = passwordId;
  }

  if (firstInvalidId) {
    $(firstInvalidId).focus();
    showToast($(`${firstInvalidId}Error`).textContent || "请检查输入内容");
    return null;
  }
  return { username: normalizedUsername, password };
}

function setAuthApiError(mode, message) {
  const text = normalizeApiMessage(message);
  if (mode === "login" && text.includes("用户名或密码")) {
    setFieldError("loginPassword", text);
  }
  if (mode === "register" && text.includes("用户名")) {
    setFieldError("registerUsername", text);
  }
  if (mode === "register" && text.includes("密码")) {
    setFieldError("registerPassword", text);
  }
}

function setAuthMode(mode) {
  const loginMode = mode === "login";
  $("loginForm").classList.toggle("hidden", !loginMode);
  $("registerForm").classList.toggle("hidden", loginMode);
  $("loginTab").classList.toggle("active", loginMode);
  $("registerTab").classList.toggle("active", !loginMode);
  $("loginTab").setAttribute("aria-selected", String(loginMode));
  $("registerTab").setAttribute("aria-selected", String(!loginMode));
  clearAuthErrors("login");
  clearAuthErrors("register");
  window.setTimeout(() => (loginMode ? $("loginUsername") : $("registerUsername")).focus(), 0);
}

async function login() {
  const values = readAuthValues("login");
  if (!values) return;
  const restore = setButtonLoading($("loginBtn"), "登录中");
  try {
    await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(values)
    });
    window.location.href = "/dashboard.html";
  } catch (error) {
    setAuthApiError("login", error.message);
    throw error;
  } finally {
    restore();
  }
}

async function register() {
  const values = readAuthValues("register");
  if (!values) return;
  const restore = setButtonLoading($("registerBtn"), "注册中");
  try {
    await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(values)
    });
    window.location.href = "/dashboard.html";
  } catch (error) {
    setAuthApiError("register", error.message);
    throw error;
  } finally {
    restore();
  }
}

async function checkAuth() {
  let response;
  try {
    response = await fetch("/api/auth/me", { credentials: "same-origin" });
  } catch (_) {
    return { authenticated: false };
  }
  if (!response.ok) return { authenticated: false };
  return response.json();
}

async function loadHealth() {
  try {
    const health = await api("/actuator/health");
    setText("healthText", health.status === "UP" ? "服务运行中" : statusText(health.status));
    if (has("healthDot")) $("healthDot").classList.toggle("ok", health.status === "UP");
  } catch (_) {
    setText("healthText", "服务未连接");
    if (has("healthDot")) $("healthDot").classList.remove("ok");
  }
}

function bindAuthEvents() {
  if ($("loginForm").dataset.bound) return;
  $("loginForm").dataset.bound = "true";
  $("registerForm").dataset.bound = "true";
  $("loginTab").addEventListener("click", () => setAuthMode("login"));
  $("registerTab").addEventListener("click", () => setAuthMode("register"));
  $("loginForm").addEventListener("submit", (event) => {
    event.preventDefault();
    login().catch((error) => showToast(error.message));
  });
  $("registerForm").addEventListener("submit", (event) => {
    event.preventDefault();
    register().catch((error) => showToast(error.message));
  });
  ["loginUsername", "loginPassword"].forEach((id) => {
    $(id).addEventListener("input", () => setFieldError(id, ""));
  });
  ["registerUsername", "registerPassword"].forEach((id) => {
    $(id).addEventListener("input", () => setFieldError(id, ""));
  });
}

async function initAuthPage() {
  bindAuthEvents();
  await loadHealth();
  const user = await checkAuth();
  if (user.authenticated) {
    window.location.href = "/dashboard.html";
  }
}

async function initShell() {
  const user = await checkAuth();
  if (!user.authenticated) {
    redirectToAuth();
    return false;
  }
  state.user = user;
  setText("userText", `用户：${user.displayName || user.username}`);
  document.querySelectorAll("[data-nav]").forEach((link) => {
    link.classList.toggle("active", link.dataset.nav === page);
  });
  if (has("logoutBtn")) $("logoutBtn").addEventListener("click", logout);
  if (has("refreshBtn")) $("refreshBtn").addEventListener("click", () => window.location.reload());
  await loadHealth();
  return true;
}

async function logout() {
  try {
    await api("/api/auth/logout", { method: "POST" });
  } catch (_) {
    // Token removal is still enough for this stateless backend.
  }
  redirectToAuth();
}

function statusText(status) {
  const map = {
    PENDING: "等待中",
    RUNNING: "分析中",
    COMPLETED: "已完成",
    FAILED: "失败",
    SUCCESS: "成功",
    UP: "运行中"
  };
  return map[status] || status || "-";
}

function statusClass(status) {
  if (status === "COMPLETED" || status === "SUCCESS") return "ok";
  if (status === "RUNNING" || status === "PENDING") return "warn";
  if (status === "FAILED") return "bad";
  return "";
}

function tagList(values, cls = "") {
  if (!values || values.length === 0) return `<span class="tag">暂无</span>`;
  return values.map((value) => `<span class="tag ${cls}">${escapeHtml(value)}</span>`).join("");
}

function liList(values) {
  if (!values || values.length === 0) return `<li class="muted">暂无</li>`;
  return values.map((value) => `<li>${escapeHtml(value)}</li>`).join("");
}

function safeArray(value) {
  return Array.isArray(value) ? value : [];
}

function readJson(value, fallback) {
  if (!value) return fallback;
  if (typeof value === "object") return value;
  try {
    return JSON.parse(value);
  } catch (_) {
    return fallback;
  }
}

function recommendationSourceMatches(job, resumeId) {
  const source = String(job.source || "");
  return resumeId ? source.includes(String(resumeId)) : true;
}

async function loadResumes() {
  state.resumes = await api("/api/resumes");
  return state.resumes;
}

async function loadJobs() {
  state.jobs = await api("/api/jobs");
  return state.jobs;
}

async function loadAnalyses(limit = 50) {
  state.analyses = await api(`/api/analyses?limit=${limit}`);
  return state.analyses;
}

function renderResumeOptions(selectId, selectedId) {
  const select = $(selectId);
  if (!select) return;
  select.innerHTML = state.resumes.length
    ? state.resumes.map((resume) => `<option value="${resume.id}">${escapeHtml(resume.title)}</option>`).join("")
    : `<option value="">请先上传简历</option>`;
  if (selectedId) select.value = String(selectedId);
}

function renderJobOptions(selectId, resumeId) {
  const select = $(selectId);
  if (!select) return;
  const relatedJobs = state.jobs.filter((job) => recommendationSourceMatches(job, resumeId));
  const jobs = relatedJobs.length ? relatedJobs : state.jobs;
  select.innerHTML = jobs.length
    ? jobs.map((job) => `<option value="${job.id}">${escapeHtml(job.title)}</option>`).join("")
    : `<option value="">请先生成岗位推荐</option>`;
}

function renderSimpleHistory(targetId, analyses) {
  if (!analyses.length) {
    setHtml(targetId, `<div class="empty">暂无分析记录</div>`);
    return;
  }
  setHtml(targetId, `
    <table>
      <thead><tr><th>ID</th><th>状态</th><th>分数</th><th>时间</th></tr></thead>
      <tbody>
        ${analyses.map((item) => `
          <tr data-id="${item.id}">
            <td class="mono">${item.id}</td>
            <td><span class="status ${statusClass(item.status)}">${statusText(item.status)}</span></td>
            <td>${item.matchScore ?? "-"}</td>
            <td class="mono">${fmt(item.createdAt)}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `);
}

async function initDashboardPage() {
  await Promise.all([loadResumes(), loadJobs(), loadAnalyses(8)]);
  setText("resumeCount", state.resumes.length);
  setText("jobCount", state.jobs.length);
  setText("analysisCount", state.analyses.length);
  setText("latestScore", state.analyses.find((item) => item.matchScore != null)?.matchScore ?? "--");
  renderSimpleHistory("dashboardHistory", state.analyses.slice(0, 6));
  renderDashboardResumes();
}

function renderDashboardResumes() {
  if (!state.resumes.length) {
    setHtml("dashboardResumes", `<div class="empty">还没有简历。进入“简历”页面上传第一份简历。</div>`);
    return;
  }
  setHtml("dashboardResumes", `
    <div class="card-list">
      ${state.resumes.slice(0, 4).map((resume) => `
        <article class="item-card">
          <h3>${escapeHtml(resume.title)}</h3>
          <div class="item-meta">
            <span>${escapeHtml(resume.targetRole || "未填写目标岗位")}</span>
            <span>${fmt(resume.updatedAt)}</span>
          </div>
        </article>
      `).join("")}
    </div>
  `);
}

async function initResumesPage() {
  await loadResumePage();
  $("resumeFile").addEventListener("change", () => {
    const file = $("resumeFile").files[0];
    if (file && !$("resumeTitle").value.trim()) $("resumeTitle").value = file.name.replace(/\.[^.]+$/, "");
  });
  $("uploadResumeBtn").addEventListener("click", () => uploadResume().catch((error) => showToast(error.message)));
  $("resumeList").addEventListener("click", (event) => {
    const deleteButton = event.target.closest("[data-delete-resume]");
    if (deleteButton) {
      deleteResume(Number(deleteButton.dataset.deleteResume)).catch((error) => showToast(error.message));
      return;
    }
    const card = event.target.closest("[data-resume-id]");
    if (card) selectResume(Number(card.dataset.resumeId));
  });
}

async function loadResumePage(preferredId) {
  await loadResumes();
  state.activeResumeId = preferredId || state.activeResumeId || state.resumes[0]?.id || null;
  if (!state.resumes.some((resume) => resume.id === state.activeResumeId)) {
    state.activeResumeId = state.resumes[0]?.id || null;
  }
  renderResumeList();
  selectResume(state.activeResumeId);
}

function renderResumeList() {
  setText("resumeTotal", `${state.resumes.length} 份`);
  if (!state.resumes.length) {
    setHtml("resumeList", `<div class="empty">暂无简历。上传后会显示在这里。</div>`);
    return;
  }
  setHtml("resumeList", `
    <div class="resume-list">
      ${state.resumes.map((resume) => `
        <article class="item-card ${resume.id === state.activeResumeId ? "active" : ""}" data-resume-id="${resume.id}">
          <h3>${escapeHtml(resume.title)}</h3>
          <div class="item-meta">
            <span>${escapeHtml(resume.ownerName || "未填写姓名")}</span>
            <span>${escapeHtml(resume.targetRole || "未填写目标岗位")}</span>
            <span>${fmt(resume.updatedAt)}</span>
          </div>
          <div class="actions">
            <button class="danger" type="button" data-delete-resume="${resume.id}">删除</button>
          </div>
        </article>
      `).join("")}
    </div>
  `);
}

function selectResume(id) {
  const resume = state.resumes.find((item) => item.id === id);
  state.activeResumeId = resume?.id || null;
  renderResumeList();
  setText("resumePreviewTitle", resume ? resume.title : "未选择");
  setText("resumePreview", resume ? resume.content : "选择一份简历后，这里会展示系统解析出的文字。");
}

async function uploadResume() {
  const file = $("resumeFile").files[0];
  if (!file) {
    showToast("请先选择 PDF、DOCX 或 TXT 简历");
    return;
  }
  const form = new FormData();
  form.append("file", file);
  if ($("resumeTitle").value.trim()) form.append("title", $("resumeTitle").value.trim());
  if ($("ownerName").value.trim()) form.append("ownerName", $("ownerName").value.trim());
  if ($("targetRole").value.trim()) form.append("targetRole", $("targetRole").value.trim());

  const restore = setButtonLoading($("uploadResumeBtn"), "解析中");
  try {
    const saved = await api("/api/resumes/upload", { method: "POST", body: form });
    $("resumeFile").value = "";
    $("resumeTitle").value = "";
    $("ownerName").value = "";
    $("targetRole").value = "";
    await loadResumePage(saved.id);
    showToast("简历已上传并解析");
  } finally {
    restore();
  }
}

async function deleteResume(id) {
  if (!window.confirm("确定删除这份简历吗？")) return;
  await api(`/api/resumes/${id}`, { method: "DELETE" });
  if (state.activeResumeId === id) state.activeResumeId = null;
  await loadResumePage();
  showToast("简历已删除");
}

async function initRecommendationsPage() {
  await Promise.all([loadResumes(), loadJobs()]);
  renderResumeOptions("recommendResumeSelect");
  renderJobOptions("savedJobSelect", Number($("recommendResumeSelect").value));
  renderRecommendationPlaceholder();
  $("recommendResumeSelect").addEventListener("change", () => {
    renderJobOptions("savedJobSelect", Number($("recommendResumeSelect").value));
    renderRecommendationPlaceholder("已切换简历，可以重新生成岗位推荐。");
  });
  $("generateRecommendationsBtn").addEventListener("click", () => {
    generateRecommendations().catch((error) => showToast(error.message));
  });
}

function renderRecommendationPlaceholder(message = "生成后，这里会展示岗位方向、推荐理由、技能匹配和准备建议。") {
  setText("recommendationStatus", "等待中");
  if (has("recommendationStatus")) $("recommendationStatus").className = "status";
  setText("recommendationSummary", message);
  setHtml("recommendationKeywords", "");
  setHtml("recommendationGaps", liList([]));
  const resumeId = has("recommendResumeSelect") ? Number($("recommendResumeSelect").value) : null;
  const saved = state.jobs.filter((job) => recommendationSourceMatches(job, resumeId));
  if (saved.length) {
    setHtml("recommendationBox", `
      <div class="card-list">
        ${saved.map((job) => `
          <article class="item-card">
            <h3>${escapeHtml(job.title)}</h3>
            <div class="item-meta">
              <span>${escapeHtml(job.company || "已保存岗位")}</span>
              <span>${fmt(job.updatedAt)}</span>
            </div>
            <div class="summary">${escapeHtml(job.description).slice(0, 220)}${job.description.length > 220 ? "..." : ""}</div>
          </article>
        `).join("")}
      </div>
    `);
  } else {
    setHtml("recommendationBox", `<div class="empty">暂无岗位推荐。选择简历后点击生成。</div>`);
  }
}

async function generateRecommendations() {
  const resumeId = Number($("recommendResumeSelect").value);
  if (!resumeId) {
    showToast("请先上传或选择简历");
    return;
  }
  const restore = setButtonLoading($("generateRecommendationsBtn"), "生成中");
  setText("recommendationStatus", "分析中");
  $("recommendationStatus").className = "status warn";
  setText("recommendationSummary", "正在根据简历生成岗位方向和投递建议。");
  setHtml("recommendationBox", `<div class="empty">推荐生成中，请稍等。</div>`);
  try {
    const report = await api(`/api/resumes/${resumeId}/job-recommendations`, { method: "POST" });
    renderRecommendations(report);
    await loadJobs();
    renderJobOptions("savedJobSelect", resumeId);
    showToast("岗位推荐已生成");
  } finally {
    restore();
  }
}

function renderRecommendations(report) {
  setText("recommendationStatus", "已完成");
  $("recommendationStatus").className = "status ok";
  setText("recommendationSummary", report.summary || "已根据简历生成岗位推荐。");
  setHtml("recommendationKeywords", tagList(report.searchKeywords || []));
  setHtml("recommendationGaps", liList(report.overallGaps || []));
  const recommendations = report.recommendations || [];
  if (!recommendations.length) {
    setHtml("recommendationBox", `<div class="empty">暂无岗位推荐</div>`);
    return;
  }
  setHtml("recommendationBox", recommendations.map((item) => `
    <article class="recommendation-card">
      <div class="recommendation-head">
        <div>
          <h3>${escapeHtml(item.roleTitle)}</h3>
          <div class="item-meta">
            <span>适配等级：${escapeHtml(item.fitLevel || "-")}</span>
            <span>${item.jobId ? `岗位 ID：${item.jobId}` : "已生成方向"}</span>
          </div>
        </div>
        <div class="fit-score">${item.fitScore ?? "--"}</div>
      </div>
      <div class="recommendation-body">
        <div class="mini-block">
          <div class="mini-title">推荐理由</div>
          <ul class="list">${liList(item.reasons)}</ul>
        </div>
        <div class="mini-grid">
          <div class="mini-block">
            <div class="mini-title">匹配技能</div>
            <div class="tag-wrap">${tagList(item.matchedSkills, "good")}</div>
          </div>
          <div class="mini-block">
            <div class="mini-title">待补强技能</div>
            <div class="tag-wrap">${tagList(item.missingSkills, "warn")}</div>
          </div>
        </div>
        <div class="mini-block">
          <div class="mini-title">搜索关键词</div>
          <div class="tag-wrap">${tagList(item.searchKeywords)}</div>
        </div>
        <div class="mini-block">
          <div class="mini-title">准备建议</div>
          <ul class="list">${liList(item.preparationTips)}</ul>
        </div>
      </div>
    </article>
  `).join(""));
}

async function initAnalysesPage() {
  await Promise.all([loadResumes(), loadJobs(), loadAnalyses(30)]);
  renderResumeOptions("analysisResumeSelect");
  renderJobOptions("analysisJobSelect", Number($("analysisResumeSelect").value));
  renderSimpleHistory("historyBox", state.analyses);
  renderEmptyReport();
  $("analysisResumeSelect").addEventListener("change", () => {
    renderJobOptions("analysisJobSelect", Number($("analysisResumeSelect").value));
  });
  $("startAnalysisBtn").addEventListener("click", () => startAnalysis().catch((error) => showToast(error.message)));
  $("downloadReportBtn").addEventListener("click", () => downloadReport().catch((error) => showToast(error.message)));
  $("loadHistoryBtn").addEventListener("click", () => refreshHistory().catch((error) => showToast(error.message)));
  $("historyBox").addEventListener("click", (event) => {
    const row = event.target.closest("tr[data-id]");
    if (row) loadAnalysis(Number(row.dataset.id)).catch((error) => showToast(error.message));
  });
}

function renderEmptyReport() {
  state.activeAnalysisId = null;
  setText("analysisStatus", "等待中");
  if (has("analysisStatus")) $("analysisStatus").className = "status";
  if (has("downloadReportBtn")) $("downloadReportBtn").disabled = true;
  setText("scoreBox", "--");
  setText("summaryBox", "选择简历和岗位后启动分析，报告会在这里展示。");
  setHtml("matchedSkills", tagList([]));
  setHtml("missingSkills", tagList([]));
  setHtml("suggestionsList", liList([]));
  setHtml("questionsList", liList([]));
  setHtml("traceBox", `<div class="empty">暂无执行记录</div>`);
}

async function startAnalysis() {
  const resumeId = Number($("analysisResumeSelect").value);
  const jobId = Number($("analysisJobSelect").value);
  if (!resumeId) {
    showToast("请先上传或选择简历");
    return;
  }
  if (!jobId) {
    showToast("请先生成或选择岗位");
    return;
  }
  const restore = setButtonLoading($("startAnalysisBtn"), "分析中");
  try {
    const analysis = await api("/api/analyses", {
      method: "POST",
      body: JSON.stringify({ resumeId, jobId })
    });
    renderReport(analysis);
    pollAnalysis(analysis.id);
    showToast("分析已启动");
  } finally {
    restore();
  }
}

async function refreshHistory() {
  await loadAnalyses(30);
  renderSimpleHistory("historyBox", state.analyses);
}

async function loadAnalysis(id) {
  const analysis = await api(`/api/analyses/${id}`);
  renderReport(analysis);
}

async function downloadReport() {
  if (!state.activeAnalysisId) {
    showToast("请先生成或选择一份分析报告");
    return;
  }
  const restore = setButtonLoading($("downloadReportBtn"), "下载中");
  try {
    const response = await fetch(`/api/analyses/${state.activeAnalysisId}/report.pdf`, {
      credentials: "same-origin"
    });
    if (!response.ok) {
      if (response.status === 401) redirectToAuth();
      const rawBody = await response.text();
      let message = rawBody;
      try {
        const body = JSON.parse(rawBody);
        message = body.message || body.detail || rawBody;
      } catch (_) {
        // Keep the raw response text when the backend did not return JSON.
      }
      throw new Error(normalizeApiMessage(message || response.statusText));
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `ai-job-analysis-${state.activeAnalysisId}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    showToast("PDF 已开始下载");
  } finally {
    restore();
  }
}

function pollAnalysis(id) {
  window.clearInterval(state.pollTimer);
  state.pollTimer = window.setInterval(async () => {
    try {
      const analysis = await api(`/api/analyses/${id}`);
      renderReport(analysis);
      if (analysis.status === "COMPLETED" || analysis.status === "FAILED") {
        window.clearInterval(state.pollTimer);
        await refreshHistory();
      }
    } catch (error) {
      window.clearInterval(state.pollTimer);
      showToast(error.message);
    }
  }, 1000);
}

function renderReport(analysis) {
  const status = analysis.status || "PENDING";
  state.activeAnalysisId = analysis.id;
  setText("analysisStatus", statusText(status));
  $("analysisStatus").className = `status ${statusClass(status)}`;
  if (has("downloadReportBtn")) $("downloadReportBtn").disabled = status !== "COMPLETED";
  setText("scoreBox", analysis.matchScore ?? "--");
  setText("summaryBox", analysis.errorMessage || analysis.summary || "系统正在分析中，请稍等。");

  const match = readJson(analysis.matchDetail, {});
  const suggestions = readJson(analysis.suggestions, {});
  const interview = readJson(analysis.interviewQuestions, {});
  const trace = safeArray(readJson(analysis.agentTrace, []));
  const matchedSkills = match.matchedSkills || match.matched_skills || [];
  const missingSkills = match.missingSkills || match.missing_skills || [];
  const resumeImprovements = suggestions.resumeImprovements || suggestions.resume_improvements || [];
  const projectRewriteTips = suggestions.projectRewriteTips || suggestions.project_rewrite_tips || [];
  const talkingPoints = interview.talkingPoints || interview.talking_points || [];

  setHtml("matchedSkills", tagList(matchedSkills, "good"));
  setHtml("missingSkills", tagList(missingSkills, "warn"));
  setHtml("suggestionsList", liList([
    ...resumeImprovements,
    ...projectRewriteTips,
    suggestions.aiAdvice || suggestions.ai_advice
  ].filter(Boolean)));
  setHtml("questionsList", liList([
    ...(interview.questions || []),
    ...talkingPoints,
    interview.aiAdvice || interview.ai_advice
  ].filter(Boolean)));
  setHtml("traceBox", trace.length
    ? trace.map((step) => `
      <div class="trace-step">
        <span>${escapeHtml(stepTitle(step.agentName || step.agent))}</span>
        <span class="status ${statusClass(step.status)}">${statusText(step.status)}</span>
      </div>
    `).join("")
    : `<div class="empty">暂无执行记录</div>`);
}

function stepTitle(value) {
  const map = {
    ResumeParserAgent: "简历解析",
    JobAnalyzerAgent: "岗位解析",
    MatchScoringAgent: "匹配评分",
    SuggestionAgent: "优化建议",
    InterviewAgent: "面试准备"
  };
  return map[value] || String(value || "分析步骤").replace(/Agent$/i, "");
}

async function init() {
  try {
    if (page === "auth") {
      await initAuthPage();
      return;
    }
    const authenticated = await initShell();
    if (!authenticated) return;
    if (page === "dashboard") await initDashboardPage();
    if (page === "resumes") await initResumesPage();
    if (page === "recommendations") await initRecommendationsPage();
    if (page === "analyses") await initAnalysesPage();
  } catch (error) {
    showToast(error.message);
  }
}

document.addEventListener("DOMContentLoaded", init);
