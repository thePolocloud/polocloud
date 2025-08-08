export interface GitHubStats {
    stars: number;
    forks: number;
    releases: number;
    downloads: number;
    commits: number;
    lastUpdated: string;
}

export interface GitHubContributor {
    login: string;
    id: number;
    avatar_url: string;
    contributions: number;
    commits: number;
    html_url: string;
    type: string;
}

export interface GitHubRelease {
    id: number;
    tag_name: string;
    name: string;
    published_at: string;
    html_url: string;
    draft: boolean;
    prerelease: boolean;
    assets: Array<{
        name: string;
        size: number;
        download_count: number;
        browser_download_url: string;
    }>;
}

const GITHUB_API_BASE = 'https://api.github.com';
const REPO_OWNER = 'HttpMarco';
const REPO_NAME = 'polocloud';
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;

let serverCache: GitHubStats | null = null;
let serverCacheTimestamp: number = 0;
const SERVER_CACHE_DURATION = 10 * 60 * 1000;

let lastRequestTime: number = 0;
const MIN_REQUEST_INTERVAL = 1000;

async function makeGitHubRequest(url: string): Promise<Response> {
    const now = Date.now();
    const timeSinceLastRequest = now - lastRequestTime;

    if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
        await new Promise(resolve => setTimeout(resolve, MIN_REQUEST_INTERVAL - timeSinceLastRequest));
    }

    lastRequestTime = Date.now();

    const headers: HeadersInit = {
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'PoloCloud-Docs'
    };

    if (GITHUB_TOKEN) {
        headers['Authorization'] = `token ${GITHUB_TOKEN}`;
    }

    const response = await fetch(url, { headers });

    if (response.status === 403) {
        throw new Error('GitHub API rate limit exceeded');
    }

    if (!response.ok) {
        throw new Error(`GitHub API error: ${response.status} ${response.statusText}`);
    }

    return response;
}

export async function fetchGitHubStats(): Promise<GitHubStats> {
    try {
        const now = Date.now();
        if (serverCache && (now - serverCacheTimestamp) < SERVER_CACHE_DURATION) {
            return serverCache;
        }

        const repoResponse = await makeGitHubRequest(`${GITHUB_API_BASE}/repos/${REPO_OWNER}/${REPO_NAME}`);
        const repoData = await repoResponse.json();

        const releasesResponse = await makeGitHubRequest(`${GITHUB_API_BASE}/repos/${REPO_OWNER}/${REPO_NAME}/releases`);
        const releasesData: GitHubRelease[] = await releasesResponse.json();

        // Fetch total commits count
        const commitsResponse = await makeGitHubRequest(`${GITHUB_API_BASE}/repos/${REPO_OWNER}/${REPO_NAME}/commits?per_page=1`);
        const linkHeader = commitsResponse.headers.get('link');
        let totalCommits = 0;

        if (linkHeader) {
            const lastLinkMatch = linkHeader.match(/<[^>]*page=(\d+)[^>]*>;\s*rel="last"/);
            if (lastLinkMatch) {
                totalCommits = parseInt(lastLinkMatch[1]);
            }
        }

        // If we couldn't get the total from headers, try to get it from the response
        if (totalCommits === 0) {
            const commitsData = await commitsResponse.json();
            totalCommits = commitsData.length;
        }

        const totalDownloads = releasesData.reduce((total, release) => {
            return total + release.assets.reduce((assetTotal, asset) => {
                return assetTotal + asset.download_count;
            }, 0);
        }, 0);

        const stats: GitHubStats = {
            stars: repoData.stargazers_count || 0,
            forks: repoData.forks_count || 0,
            releases: releasesData.length || 0,
            downloads: totalDownloads,
            commits: totalCommits,
            lastUpdated: new Date().toISOString(),
        };

        serverCache = stats;
        serverCacheTimestamp = now;

        return stats;
    } catch (error) {
        console.error('Error fetching GitHub stats:', error);

        if (serverCache) {
            console.log('Returning cached GitHub stats due to API error');
            return serverCache;
        }

        return {
            stars: 61,
            forks: 22,
            releases: 4,
            downloads: 0,
            commits: 3206,
            lastUpdated: new Date().toISOString(),
        };
    }
}

let clientCache: GitHubStats | null = null;
let clientCacheTimestamp: number = 0;
const CLIENT_CACHE_DURATION = 5 * 60 * 1000;

export async function getCachedGitHubStats(): Promise<GitHubStats> {
    const now = Date.now();

    if (clientCache && (now - clientCacheTimestamp) < CLIENT_CACHE_DURATION) {
        return clientCache;
    }

    const stats = await fetchGitHubStats();

    clientCache = stats;
    clientCacheTimestamp = now;

    return stats;
}

let contributorsCache: GitHubContributor[] | null = null;
let contributorsCacheTimestamp: number = 0;
const CONTRIBUTORS_CACHE_DURATION = 60 * 60 * 1000;

export async function fetchGitHubContributors(): Promise<GitHubContributor[]> {
    try {
        const response = await makeGitHubRequest(`${GITHUB_API_BASE}/repos/${REPO_OWNER}/${REPO_NAME}/contributors?per_page=100`);
        const contributors: GitHubContributor[] = await response.json();

        const filteredContributors = contributors
            .filter(contributor => contributor.type === 'User')
            .sort((a, b) => b.contributions - a.contributions);

        const contributorsWithCommits = await Promise.all(
            filteredContributors.map(async (contributor) => {
                try {
                    const commitsResponse = await makeGitHubRequest(
                        `${GITHUB_API_BASE}/repos/${REPO_OWNER}/${REPO_NAME}/commits?author=${contributor.login}&per_page=1`
                    );

                    const linkHeader = commitsResponse.headers.get('link');
                    let commitCount = 0;

                    if (linkHeader) {
                        const lastLinkMatch = linkHeader.match(/<[^>]*page=(\d+)[^>]*>;\s*rel="last"/);
                        if (lastLinkMatch) {
                            commitCount = parseInt(lastLinkMatch[1]);
                        }
                    }

                    if (commitCount === 0) {
                        const commitsData = await commitsResponse.json();
                        commitCount = commitsData.length;
                    }

                    return {
                        ...contributor,
                        commits: commitCount
                    };
                } catch (error) {
                    console.error(`Error fetching commits for ${contributor.login}:`, error);
                    return {
                        ...contributor,
                        commits: contributor.contributions
                    };
                }
            })
        );

        return contributorsWithCommits;
    } catch (error) {
        console.error('Error fetching GitHub contributors:', error);

        if (contributorsCache) {
            console.log('Returning cached GitHub contributors due to API error');
            return contributorsCache;
        }

        return [
            {
                login: 'HttpMarco',
                id: 1,
                avatar_url: 'https://github.com/HttpMarco.png',
                contributions: 156,
                commits: 156,
                html_url: 'https://github.com/HttpMarco',
                type: 'User'
            }
        ];
    }
}

export async function getCachedGitHubContributors(): Promise<GitHubContributor[]> {
    const now = Date.now();

    if (contributorsCache && (now - contributorsCacheTimestamp) < CONTRIBUTORS_CACHE_DURATION) {
        return contributorsCache;
    }

    const contributors = await fetchGitHubContributors();

    contributorsCache = contributors;
    contributorsCacheTimestamp = now;

    return contributors;
}

export function clearGitHubCache(): void {
    serverCache = null;
    serverCacheTimestamp = 0;
    clientCache = null;
    clientCacheTimestamp = 0;
    contributorsCache = null;
    contributorsCacheTimestamp = 0;
}

export function getCacheStatus(): {
    serverCacheAge: number;
    clientCacheAge: number;
    contributorsCacheAge: number;
    hasServerCache: boolean;
    hasClientCache: boolean;
    hasContributorsCache: boolean;
} {
    const now = Date.now();
    return {
        serverCacheAge: serverCache ? now - serverCacheTimestamp : 0,
        clientCacheAge: clientCache ? now - clientCacheTimestamp : 0,
        contributorsCacheAge: contributorsCache ? now - contributorsCacheTimestamp : 0,
        hasServerCache: !!serverCache,
        hasClientCache: !!clientCache,
        hasContributorsCache: !!contributorsCache,
    };
} 