export interface GitHubContributor {
    login: string;
    id: number;
    avatar_url: string;
    contributions: number;
    html_url: string;
    commits?: number;
} 