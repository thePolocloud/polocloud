const REFRESH_INTERVAL_MS = 10_000;

const groupsEl = document.getElementById("groups");
const subtitleEl = document.getElementById("subtitle");
const emptyEl = document.getElementById("empty");
const errorEl = document.getElementById("error");
const updatedEl = document.getElementById("updated");
const cardTemplate = document.getElementById("group-card-template");

const statGroupsEl = document.getElementById("stat-groups");
const statPlayersEl = document.getElementById("stat-players");
const statServicesEl = document.getElementById("stat-services");

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
    const groups = snapshot.groups;
    const onlineGroups = groups.filter((g) => g.availability === "ONLINE");
    const totalPlayers = groups.reduce((sum, g) => sum + g.onlinePlayers, 0);
    const totalServices = groups.reduce((sum, g) => sum + g.onlineServices, 0);

    subtitleEl.textContent = `Node ${snapshot.node}`;
    statGroupsEl.textContent = `${onlineGroups.length} / ${groups.length}`;
    statPlayersEl.textContent = totalPlayers.toLocaleString();
    statServicesEl.textContent = totalServices.toLocaleString();
    updatedEl.textContent = `Last updated ${new Date(snapshot.generatedAt).toLocaleTimeString()}`;

    emptyEl.hidden = groups.length > 0;
    groupsEl.replaceChildren(...groups.map(renderCard));
}

function renderCard(group) {
    const node = cardTemplate.content.cloneNode(true);
    const card = node.querySelector(".card");
    const fillPercent = group.maxPlayers > 0
        ? Math.min(100, Math.round((group.onlinePlayers / group.maxPlayers) * 100))
        : 0;

    card.classList.add(group.availability.toLowerCase());
    node.querySelector(".name").textContent = group.displayName;
    node.querySelector(".badge").textContent = AVAILABILITY_LABEL[group.availability] ?? group.availability;
    node.querySelector(".meter-fill").style.width = `${fillPercent}%`;
    node.querySelector(".players").textContent = `${group.onlinePlayers} / ${group.maxPlayers} players`;
    node.querySelector(".services").textContent = `${group.onlineServices} service(s)`;

    return card;
}

refresh();
setInterval(refresh, REFRESH_INTERVAL_MS);
