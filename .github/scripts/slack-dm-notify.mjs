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
async function openDM(userId) {
    const res = await fetch(
        "https://slack.com/api/conversations.open",
        {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ users: userId }),
        }
    );
    const json = await res.json();
    if (!json.ok) throw new Error(json.error);
    return json.channel.id;
}

async function postMessage(channel, text) {
    const res = await fetch(
        "https://slack.com/api/chat.postMessage",
        {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ channel, text }),
        }
    );
    const json = await res.json();
    if (!json.ok) throw new Error(json.error);
}

/* =====================
   4. Recipient & message builder
===================== */
function buildNotification(e) {
    const recipients = [];
    let title = "";
    let url = "";

    const repo = process.env.GITHUB_REPOSITORY;
    const action = e.action ? ` (${e.action})` : "";
    const actor = e.sender?.login ? `@${e.sender.login}` : "someone";

    // ----- Pull Request -----
    if (e.pull_request) {
        const pr = e.pull_request;
        title = `PR #${pr.number}: ${pr.title}`;
        url = pr.html_url;

        // 리뷰 요청: 단일 reviewer 우선
        if (e.action === "review_requested" && e.requested_reviewer) {
            recipients.push(e.requested_reviewer.login);
        }

        // assign
        (pr.assignees || []).forEach((a) =>
            recipients.push(a.login)
        );

        // 본문 멘션
        recipients.push(...extractMentions(pr.body));
    }

    // ----- Issue (PR 제외) -----
    if (e.issue && !e.issue.pull_request) {
        const is = e.issue;
        title = `Issue #${is.number}: ${is.title}`;
        url = is.html_url;

        (is.assignees || []).forEach((a) =>
            recipients.push(a.login)
        );

        recipients.push(...extractMentions(is.body));
    }

    // ----- Issue / PR Comment -----
    if (e.comment) {
        url = e.comment.html_url;
        recipients.push(...extractMentions(e.comment.body));

        // issue_comment on a PR: 작성자 + assignee에게도 알림
        if (e.issue?.pull_request) {
            recipients.push(e.issue.user?.login);
            (e.issue.assignees || []).forEach((a) => recipients.push(a.login));
        }
    }

    // ----- PR Review Submitted -----
    if (e.review && e.pull_request) {
        const pr = e.pull_request;
        title = `Review on PR #${pr.number}: ${pr.title} (${e.review.state})`;
        url = e.review.html_url || pr.html_url;

        // PR 작성자에게 알림
        recipients.push(pr.user?.login);
        recipients.push(...extractMentions(e.review.body));
    }

    const text =
        `🔔 GitHub 알림${action}\n` +
        `Repo: ${repo}\n` +
        `By: ${actor}\n` +
        `${title}\n` +
        `🔗 ${url}`;

    const sender = e.sender?.login;
    return { recipients: uniq(recipients).filter((r) => r !== sender), text };
}

/* =====================
   5. Main
===================== */
async function main() {
    const { recipients, text } = buildNotification(event);

    const slackUsers = uniq(
        recipients.map(ghToSlack).filter(Boolean)
    );

    if (slackUsers.length === 0) {
        console.log("No matching Slack users. Skipping.");
        return;
    }

    for (const uid of slackUsers) {
        const channel = await openDM(uid);
        await postMessage(channel, text);
    }

    console.log(`Sent Slack DM to ${slackUsers.length} user(s).`);
}

main().catch((err) => {
    console.error("Slack notify failed:", err.message);
    process.exit(1);
});
