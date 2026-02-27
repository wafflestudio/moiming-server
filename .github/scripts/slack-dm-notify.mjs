import fs from "fs";

/* =====================
   0. Guard: secrets 없음 → 조용히 종료
===================== */
const token = process.env.SLACK_BOT_TOKEN;
if (!token) {
    console.log("No SLACK_BOT_TOKEN (likely fork PR). Skipping.");
    process.exit(0);
}

/* =====================
   1. Load event + user map
===================== */
const event = JSON.parse(
    fs.readFileSync(process.env.GITHUB_EVENT_PATH, "utf8")
);

// user map: Secret 우선, 없으면 파일 fallback
let userMap = {};
if (process.env.SLACK_USER_MAP) {
    userMap = JSON.parse(process.env.SLACK_USER_MAP);
} else if (fs.existsSync(".github/slack-map.json")) {
    userMap = JSON.parse(
        fs.readFileSync(".github/slack-map.json", "utf8")
    );
}

/* =====================
   2. Utilities
===================== */
const uniq = (arr) => [...new Set(arr.filter(Boolean))];

const extractMentions = (text) => {
    if (!text) return [];
    const re = /@([a-zA-Z0-9-]+)/g;
    const out = [];
    let m;
    while ((m = re.exec(text)) !== null) out.push(m[1]);
    return out;
};

const ghToSlack = (login) => userMap[login] || null;

/* =====================
   3. Slack API helpers
===================== */
async function slackPost(endpoint, body) {
    const res = await fetch(`https://slack.com/api/${endpoint}`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
    });
    if (res.status === 429) {
        const err = new Error("ratelimited");
        err.retryAfterMs = parseInt(res.headers.get("Retry-After") || "5", 10) * 1000;
        throw err;
    }
    const json = await res.json();
    if (!json.ok) throw new Error(json.error);
    return json;
}

async function sendDM(userId, text) {
    const { channel } = await slackPost("conversations.open", { users: userId });
    await slackPost("chat.postMessage", { channel: channel.id, text });
}

/* =====================
   4. GitHub API helper
===================== */

// 대기 중인 reviewer(requested_reviewers) + 이미 리뷰 제출한 reviewer 모두 반환
async function fetchPRReviewers(prNumber) {
    const repo = process.env.GITHUB_REPOSITORY;
    const headers = {
        Authorization: `Bearer ${process.env.GITHUB_TOKEN}`,
        Accept: "application/vnd.github.v3+json",
    };

    const [prRes, reviewsRes] = await Promise.all([
        fetch(`https://api.github.com/repos/${repo}/pulls/${prNumber}`, { headers }),
        fetch(`https://api.github.com/repos/${repo}/pulls/${prNumber}/reviews`, { headers }),
    ]);

    const pr = await prRes.json();
    const reviews = await reviewsRes.json();

    const reviewers = new Set(
        (pr.requested_reviewers || []).map((r) => r.login)
    );
    (Array.isArray(reviews) ? reviews : []).forEach((r) => {
        if (r.user?.login) reviewers.add(r.user.login);
    });

    return [...reviewers];
}

/* =====================
   5. Event handlers
   각 핸들러는 { title, body, url, recipients } 를 반환
===================== */

// pull_request: opened, reopened, ready_for_review, review_requested, assigned
function handlePullRequest(e) {
    const pr = e.pull_request;
    const title = `PR #${pr.number}: ${pr.title}`;
    const url = pr.html_url;
    const recipients = [];

    // 리뷰 요청: 새로 요청된 reviewer에게만
    if (e.action === "review_requested" && e.requested_reviewer) {
        recipients.push(e.requested_reviewer.login);
    }

    // assign: 새로 assign된 사람에게만
    if (e.action === "assigned") {
        recipients.push(e.assignee?.login);
    }

    // 본문 멘션: 처음 열릴 때만
    if (e.action === "opened") {
        recipients.push(...extractMentions(pr.body));
    }

    return { title, body: pr.body, url, recipients };
}

// pull_request_review: submitted
function handlePullRequestReview(e) {
    const pr = e.pull_request;
    const title = `PR #${pr.number}: ${pr.title}의 새 리뷰 (${e.review.state})`;
    const url = e.review.html_url || pr.html_url;
    const recipients = [
        pr.user?.login,
        ...extractMentions(e.review.body),
    ];

    return { title, body: e.review.body, url, recipients };
}

// pull_request_review_comment: created
async function handlePullRequestReviewComment(e) {
    const pr = e.pull_request;
    const title = `PR #${pr.number} ${pr.title}의 새 코멘트`;
    const url = e.comment.html_url;
    const reviewers = await fetchPRReviewers(pr.number);
    const recipients = [
        pr.user?.login,
        ...reviewers,
        ...extractMentions(e.comment.body),
    ];

    return { title, body: e.comment.body, url, recipients };
}

// issue_comment on PR: created
async function handleIssueCommentOnPR(e) {
    const is = e.issue;
    const title = `PR #${is.number}: ${is.title}의 새 코멘트`;
    const url = e.comment.html_url;
    const reviewers = await fetchPRReviewers(is.number);
    const recipients = [
        is.user?.login,
        ...reviewers,
        ...extractMentions(e.comment.body),
    ];

    return { title, body: e.comment.body, url, recipients };
}

// issue_comment on Issue: created
function handleIssueComment(e) {
    const is = e.issue;
    const title = `Issue #${is.number}: ${is.title}의 새 코멘트`;
    const url = e.comment.html_url;
    const recipients = [
        is.user?.login,
        ...extractMentions(e.comment.body),
    ];

    return { title, body: e.comment.body, url, recipients };
}

// issues: opened, assigned
function handleIssues(e) {
    const is = e.issue;
    const title = `Issue #${is.number}: ${is.title}`;
    const url = is.html_url;
    const recipients = [];

    // assign: 새로 assign된 사람에게만
    if (e.action === "assigned") {
        recipients.push(e.assignee?.login);
    }

    // 본문 멘션: 처음 열릴 때만
    if (e.action === "opened") {
        recipients.push(...extractMentions(is.body));
    }

    return { title, body: is.body, url, recipients };
}

/* =====================
   6. Notification builder (이벤트 디스패처)
===================== */
async function buildNotification(e) {
    let result;

    if (e.review && e.pull_request) {
        result = handlePullRequestReview(e);
    } else if (e.comment && e.pull_request) {
        result = await handlePullRequestReviewComment(e);
    } else if (e.comment && e.issue?.pull_request) {
        result = await handleIssueCommentOnPR(e);
    } else if (e.comment && e.issue) {
        result = handleIssueComment(e);
    } else if (e.pull_request) {
        result = handlePullRequest(e);
    } else if (e.issue) {
        result = handleIssues(e);
    } else {
        return { recipients: [], text: "" };
    }

    const { title, body, url, recipients } = result;
    const repo = process.env.GITHUB_REPOSITORY;
    const action = e.action ? ` (${e.action})` : "";
    const actor = e.sender?.login ? `@${e.sender.login}` : "someone";

    const text =
        `🔔 GitHub 알림${action}\n` +
        `Repo: ${repo}\n` +
        `By: ${actor}\n` +
        `${title}\n` +
        (body ? `${body}\n` : "") +
        `🔗 ${url}`;

    const sender = e.sender?.login;
    return { recipients: uniq(recipients).filter((r) => r !== sender), text };
}

/* =====================
   7. Main
===================== */

// 토큰/스코프/계정 문제 등 모든 요청에 영향을 주는 전역 오류 → 즉시 job 실패
const FATAL_SLACK_ERRORS = new Set([
    "invalid_auth",
    "not_authed",
    "account_inactive",
    "token_revoked",
    "token_expired",
    "no_permission",
    "missing_scope",
]);

const MAX_ATTEMPTS = 3;

async function main() {
    const { recipients, text } = await buildNotification(event);

    const slackUsers = uniq(
        recipients.map(ghToSlack).filter(Boolean)
    );

    if (slackUsers.length === 0) {
        console.log("No matching Slack users. Skipping.");
        return;
    }

    let successCount = 0;
    for (const uid of slackUsers) {
        let sent = false;
        for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                await sendDM(uid, text);
                sent = true;
                break;
            } catch (err) {
                // 전역 오류: 재시도 없이 즉시 throw → job 실패
                if (FATAL_SLACK_ERRORS.has(err.message)) throw err;

                if (attempt === MAX_ATTEMPTS) {
                    console.error(`Failed to DM ${uid} after ${MAX_ATTEMPTS} attempts: ${err.message}`);
                    break;
                }
                const delay = err.retryAfterMs ?? 1000 * 2 ** (attempt - 1);
                console.warn(`Retry ${attempt}/${MAX_ATTEMPTS} for ${uid} in ${delay}ms: ${err.message}`);
                await new Promise((r) => setTimeout(r, delay));
            }
        }
        if (sent) successCount++;
    }

    if (successCount === 0) {
        throw new Error(`All ${slackUsers.length} DM(s) failed — check bot token and user mappings.`);
    }
    console.log(`Sent Slack DM to ${successCount}/${slackUsers.length} user(s).`);
}

main().catch((err) => {
    console.error("Slack notify failed:", err.message);
    process.exit(1);
});
