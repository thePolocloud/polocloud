import axios from 'axios';
import { Logger } from '../utils/Logger';
import { GITHUB_CONFIG } from '../config/constants';

export interface GitHubStats {
    stars: number;
    forks: number;
    watchers: number;
    openIssues: number;
    openPullRequests: number;
    contributors: number;
    branches: string[];
    languages: Array<{
        name: string;
        percentage: number;
    }>;
    lastCommit: string;
    license: string;
    description: string;
}

export class GitHubStatsService {
    private logger: Logger;

    constructor() {
        this.logger = new Logger('GitHubStatsService');
    }

    public async fetchGitHubStats(): Promise<GitHubStats> {
        try {
            const headers: any = {
                'Accept': 'application/vnd.github.v3+json',
                'User-Agent': 'PoloCloud-DiscordBot/1.0.0'
            };

            if (process.env['GITHUB_TOKEN']) {
                headers['Authorization'] = `token ${process.env['GITHUB_TOKEN']}`;
            }

            const repoResponse = await axios.get(GITHUB_CONFIG.REPO_URL, { headers });
            const repoData = repoResponse.data;

            // GitHub API for Languages
            const languagesResponse = await axios.get(`${GITHUB_CONFIG.REPO_URL}/languages`, { headers });

            // GitHub API for Pull Requests (open)
            const pullRequestsResponse = await axios.get(`${GITHUB_CONFIG.REPO_URL}/pulls?state=open`, { headers });

            // GitHub API for Contributors
            const contributorsResponse = await axios.get(`${GITHUB_CONFIG.REPO_URL}/contributors`, { headers });

            // GitHub API for Branches
            const branchesResponse = await axios.get(`${GITHUB_CONFIG.REPO_URL}/branches`, { headers });

            // Use repository updated_at for last commit if we can't fetch commits due to rate limit
            let lastCommitDate: string;
            try {
                const commitsResponse = await axios.get(`${GITHUB_CONFIG.REPO_URL}/commits?per_page=1`, { headers });
                lastCommitDate = commitsResponse.data.length > 0
                    ? new Date(commitsResponse.data[0].commit.author.date).toLocaleDateString('de-DE')
                    : new Date(repoData.updated_at).toLocaleDateString('de-DE');
            } catch (commitError) {
                this.logger.warn('Could not fetch commits, using repository updated_at as fallback');
                lastCommitDate = new Date(repoData.updated_at).toLocaleDateString('de-DE');
            }

            const languagesData = languagesResponse.data;
            const totalBytes = Object.values(languagesData).reduce((sum: number, bytes: any) => sum + bytes, 0);

            const languages = Object.entries(languagesData)
                .map(([name, bytes]: [string, any]) => ({
                    name,
                    percentage: Math.round((bytes / totalBytes) * 100)
                }))
                .sort((a, b) => b.percentage - a.percentage)
                .slice(0, GITHUB_CONFIG.TOP_LANGUAGES);

            return {
                stars: repoData.stargazers_count,
                forks: repoData.forks_count,
                watchers: repoData.subscribers_count,
                openIssues: repoData.open_issues_count,
                openPullRequests: pullRequestsResponse.data.length,
                contributors: contributorsResponse.data.length,
                branches: branchesResponse.data.map((branch: any) => branch.name),
                languages,
                lastCommit: lastCommitDate,
                license: repoData.license?.name,
                description: repoData.description
            };

        } catch (error) {
            this.logger.error('Error loading GitHub statistics:', error);
            throw new Error('Failed to fetch GitHub statistics');
        }
    }
}