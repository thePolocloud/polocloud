import { Client, TextChannel, MessageFlags, ContainerBuilder, Colors } from 'discord.js';
import { Logger } from '../../utils/Logger';
import { GitHubContributor } from '../../interfaces/GitHubContributor';
import { GITHUB_CONFIG } from '../../config/constants';
import axios from 'axios';

interface StoredContributorsContainer {
    guildId: string;
    channelId: string;
    messageId: string;
}

export class ContributorsUpdateService {
    private logger: Logger;
    private client: Client;
    private updateInterval?: NodeJS.Timeout;
    private storedContainers: Map<string, StoredContributorsContainer> = new Map();

    constructor(client: Client) {
        this.client = client;
        this.logger = new Logger('ContributorsUpdateService');
    }

    public start(): void {
        this.updateInterval = setInterval(async () => {
            await this.updateAllContainers();
        }, 900000);

        this.logger.info('Contributors Update Service started');
    }

    public stop(): void {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
        this.logger.info('Contributors Update Service stopped');
    }

    public async addContainer(guildId: string, channelId: string, messageId: string): Promise<void> {
        const key = `${guildId}-${channelId}`;
        this.storedContainers.set(key, { guildId, channelId, messageId });
        this.logger.info(`Added Contributors container for tracking: ${key}`);
    }

    public async removeContainer(guildId: string, channelId: string): Promise<void> {
        const key = `${guildId}-${channelId}`;
        this.storedContainers.delete(key);
        this.logger.info(`Removed Contributors container from tracking: ${key}`);
    }

    private async updateAllContainers(): Promise<void> {
        this.logger.info('Starting Contributors container update...');

        for (const [key, container] of this.storedContainers) {
            try {
                await this.updateContainer(container);
            } catch (error) {
                this.logger.error(`Failed to update Contributors container ${key}:`, error);
            }
        }

        this.logger.info('Contributors container update completed');
    }

    private async updateContainer(container: StoredContributorsContainer): Promise<void> {
        try {
            const guild = await this.client.guilds.fetch(container.guildId);
            const channel = await guild.channels.fetch(container.channelId) as TextChannel;

            if (!channel) {
                this.logger.warn(`Channel ${container.channelId} not found in guild ${container.guildId}`);
                return;
            }

            const message = await channel.messages.fetch(container.messageId);
            if (!message) {
                this.logger.warn(`Message ${container.messageId} not found in channel ${container.channelId}`);
                return;
            }

            const contributors = await this.fetchContributorsWithCommits();

            if (contributors.length === 0) {
                this.logger.warn('No contributors found, skipping update');
                return;
            }

            const newContainer = this.createContributorsContainer(contributors);

            await message.edit({
                components: [newContainer],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`Updated Contributors container in guild ${container.guildId}, channel ${container.channelId}`);

        } catch (error) {
            this.logger.error(`Error updating Contributors container:`, error);
            throw error;
        }
    }

    private async fetchContributorsWithCommits(): Promise<GitHubContributor[]> {
        try {
            const headers: any = {
                'Accept': 'application/vnd.github.v3+json',
                'User-Agent': 'PoloCloud-DiscordBot/1.0.0'
            };

            if (process.env['GITHUB_TOKEN']) {
                headers['Authorization'] = `token ${process.env['GITHUB_TOKEN']}`;
            }

            const contributorsResponse = await axios.get(`${GITHUB_CONFIG.REPO_URL}/contributors?per_page=100`, { headers });
            const contributors = contributorsResponse.data;

            const contributorsWithCommits = await Promise.all(
                contributors.map(async (contributor: GitHubContributor) => {
                    try {
                        const commitsResponse = await axios.get(
                            `${GITHUB_CONFIG.REPO_URL}/commits?author=${contributor.login}&per_page=1`,
                            { headers }
                        );

                        if (commitsResponse.data.length === 0) {
                            return {
                                ...contributor,
                                commits: 0
                            };
                        }

                        const linkHeader = commitsResponse.headers['link'];
                        let totalCommits = contributor.contributions;

                        if (linkHeader) {
                            const links = linkHeader.split(',');
                            const lastLink = links.find((link: string) => link.includes('rel="last"'));
                            if (lastLink) {
                                const match = lastLink.match(/page=(\d+)>/);
                                if (match) {
                                    totalCommits = parseInt(match[1]);
                                }
                            }
                        }

                        if (totalCommits === contributor.contributions) {
                            let manualCount = 0;
                            let page = 1;
                            const perPage = 100;

                            while (true) {
                                const pageResponse = await axios.get(
                                    `${GITHUB_CONFIG.REPO_URL}/commits?author=${contributor.login}&per_page=${perPage}&page=${page}`,
                                    { headers }
                                );

                                const pageCommits = pageResponse.data;
                                if (pageCommits.length === 0) break;

                                manualCount += pageCommits.length;

                                if (pageCommits.length < perPage) break;
                                page++;

                                if (page > 20) break;
                            }

                            totalCommits = manualCount;
                        }

                        return {
                            ...contributor,
                            commits: totalCommits
                        };
                    } catch (error) {
                        this.logger.warn(`Could not fetch commit count for ${contributor.login}, using fallback`);
                        return {
                            ...contributor,
                            commits: contributor.contributions
                        };
                    }
                })
            );

            return contributorsWithCommits;
        } catch (error) {
            this.logger.error('Error fetching contributors:', error);
            throw new Error('Failed to fetch contributors');
        }
    }

    private createContributorsContainer(contributors: GitHubContributor[]): any {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Blue);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 👥 PoloCloud Contributors\n\nAll contributors to the **PoloCloud** GitHub repository`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        const sortedContributors = contributors.sort((a, b) => {
            const aCommits = a.commits || a.contributions;
            const bCommits = b.commits || b.contributions;
            return bCommits - aCommits;
        });

        const contributorsList = sortedContributors.map((contributor, index) => {
            const rank = index + 1;
            const rankEmoji = rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : `${rank}.`;
            const commitCount = contributor.commits || contributor.contributions;
            return `${rankEmoji} **${contributor.login}** - \`${commitCount} commits\``;
        }).join('\n');

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📊 Contributors (${contributors.length})\n\n${contributorsList || 'No contributors found'}`)
        );

        container.addSeparatorComponents(
            separator => separator
        );


        return container;
    }
}