import { Client, TextChannel } from 'discord.js';
import { Logger } from '../../utils/Logger';
import { GitHubStatsService } from './GitHubStatsService';
import { GitHubEmbedBuilder } from '../../utils/GitHubEmbedBuilder';
import { GITHUB_CONFIG } from '../../config/constants';

interface StoredGitHubEmbed {
    guildId: string;
    channelId: string;
    messageId: string;
}

export class GitHubStatsUpdateService {
    private client: Client;
    private logger: Logger;
    private updateInterval: NodeJS.Timeout | null = null;
    private storedEmbeds: Map<string, StoredGitHubEmbed> = new Map();
    private githubStatsService: GitHubStatsService;

    constructor(client: Client) {
        this.client = client;
        this.logger = new Logger('GitHubStatsUpdateService');
        this.githubStatsService = new GitHubStatsService();
    }

    public start(): void {
        this.updateInterval = setInterval(async () => {
            await this.updateAllEmbeds();
        }, GITHUB_CONFIG.UPDATE_INTERVAL);

        this.logger.info('GitHub Stats Update Service started');
    }

    public stop(): void {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
        this.logger.info('GitHub Stats Update Service stopped');
    }

    public async addEmbed(guildId: string, channelId: string, messageId: string): Promise<void> {
        const key = `${guildId}-${channelId}`;
        this.storedEmbeds.set(key, { guildId, channelId, messageId });
        this.logger.info(`Added GitHub embed for tracking: ${key}`);
    }

    public async removeEmbed(guildId: string, channelId: string): Promise<void> {
        const key = `${guildId}-${channelId}`;
        this.storedEmbeds.delete(key);
        this.logger.info(`Removed GitHub embed from tracking: ${key}`);
    }

    private async updateAllEmbeds(): Promise<void> {
        this.logger.info('Starting GitHub embed update...');

        for (const [key, embed] of this.storedEmbeds) {
            try {
                await this.updateEmbed(embed);
            } catch (error) {
                this.logger.error(`Failed to update GitHub embed ${key}:`, error);
            }
        }

        this.logger.info('GitHub embed update completed');
    }

    private async updateEmbed(embed: StoredGitHubEmbed): Promise<void> {
        try {
            const guild = await this.client.guilds.fetch(embed.guildId);
            const channel = await guild.channels.fetch(embed.channelId) as TextChannel;

            if (!channel) {
                this.logger.warn(`Channel ${embed.channelId} not found in guild ${embed.guildId}`);
                return;
            }

            const message = await channel.messages.fetch(embed.messageId);
            if (!message) {
                this.logger.warn(`Message ${embed.messageId} not found in channel ${embed.channelId}`);
                return;
            }

            const stats = await this.githubStatsService.fetchGitHubStats();
            const newEmbed = GitHubEmbedBuilder.createGitHubStatsEmbed(stats);

            await message.edit({ embeds: [newEmbed] });
            this.logger.info(`Updated GitHub embed in guild ${embed.guildId}, channel ${embed.channelId}`);

        } catch (error) {
            this.logger.error(`Error updating GitHub embed:`, error);
            throw error;
        }
    }
} 