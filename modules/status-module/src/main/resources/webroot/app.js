const REFRESH_INTERVAL_MS = 10_000;

const groupsEl = document.getElementById("groups");
const subtitleEl = document.getElementById("subtitle");
const errorEl = document.getElementById("error");
const updatedEl = document.getElementById("updated");
const cardTemplate = document.getElementById("group-card-template");

const AVAILABILITY_LABEL = {
    ONLINE: "Online",
    OFFLINE: "Offline",
    MAINTENANCE: "Maintenance",
};

async function refresh() {
    try {
        const response = await fetch("/api/status", { cache: "no-store" });
        if (!response.ok) throw new Error(`status API returned ${response.status}`);
        const snapshot = await response.json();
        render(snapshot);
        errorEl.hidden = true;
    } catch (err) {
        errorEl.hidden = false;
        console.error("Failed to load status:", err);
    }
}

function render(snapshot) {
    const onlineCount = snapshot.groups.filter((g) => g.availability === "ONLINE").length;
    subtitleEl.textContent = `${onlineCount} / ${snapshot.groups.length} group(s) online — node ${snapshot.node}`;
    updatedEl.textContent = `Last updated ${new Date(snapshot.generatedAt).toLocaleTimeString()}`;

    groupsEl.replaceChildren(...snapshot.groups.map(renderCard));
}

function renderCard(group) {
    const node = cardTemplate.content.cloneNode(true);
    const card = node.querySelector(".card");

    card.classList.add(group.availability.toLowerCase());
    node.querySelector(".name").textContent = group.displayName;
    node.querySelector(".badge").textContent = AVAILABILITY_LABEL[group.availability] ?? group.availability;
    node.querySelector(".players").textContent = `${group.onlinePlayers} / ${group.maxPlayers} players`;
    node.querySelector(".services").textContent = `${group.onlineServices} service(s) running`;

    return card;
}

refresh();
setInterval(refresh, REFRESH_INTERVAL_MS);
