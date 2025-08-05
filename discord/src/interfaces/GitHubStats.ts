export interface GitHubStatsData {
    stars: number;
    forks: number;
    watchers: number;
    openIssues: number;
    lastUpdated: number;
}

export interface StoredGitHubContainer {
    guildId: string;
    channelId: string;
    messageId: string;
    lastUpdate: number;
} 