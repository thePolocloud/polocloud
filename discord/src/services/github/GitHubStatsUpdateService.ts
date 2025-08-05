import { Client, TextChannel, MessageFlags } from 'discord.js';
import { Logger } from '../../utils/Logger';
import { GitHubStatsService } from './GitHubStatsService';
import { GitHubContainerBuilder } from '../../utils/GitHubContainerBuilder';
import { GITHUB_CONFIG } from '../../config/constants';

interface StoredGitHubContainer {
    guildId: string;
    channelId: string;
    messageId: string;
}

export class GitHubStatsUpdateService {
    private client: Client;
    private logger: Logger;
    private updateInterval?: NodeJS.Timeout;
    private storedContainers: StoredGitHubContainer[] = [];
    private githubStatsService: GitHubStatsService;

    constructor(client: Client) {
        this.client = client;
        this.logger = new Logger('GitHubStatsUpdateService');
        this.githubStatsService = new GitHubStatsService();
        this.startUpdateInterval();
    }

    private startUpdateInterval(): void {
        this.updateInterval = setInterval(() => {
            this.updateAllContainers().catch(error => {
                this.logger.error('Error in update interval:', error);
            });
        }, GITHUB_CONFIG.UPDATE_INTERVAL);
    }

    public async addEmbed(guildId: string, channelId: string, messageId: string): Promise<void> {
        const container: StoredGitHubContainer = {
            guildId,
            channelId,
            messageId
        };

        this.storedContainers = this.storedContainers.filter(
            existing => !(existing.guildId === guildId && existing.channelId === channelId)
        );

        this.storedContainers.push(container);
        this.logger.info(`Added GitHub container in guild ${guildId}, channel ${channelId}`);
    }

    public async removeEmbed(guildId: string, channelId: string): Promise<string | null> {
        const container = this.storedContainers.find(
            c => c.guildId === guildId && c.channelId === channelId
        );
        
        if (container) {
            this.storedContainers = this.storedContainers.filter(
                c => !(c.guildId === guildId && c.channelId === channelId)
            );
            this.logger.info(`Removed GitHub container in guild ${guildId}, channel ${channelId}`);
            return container.messageId;
        }
        
        this.logger.info(`No GitHub container found in guild ${guildId}, channel ${channelId}`);
        return null;
    }

    private async updateAllContainers(): Promise<void> {
        this.logger.info('Starting GitHub container update...');

        for (const container of this.storedContainers) {
            try {
                await this.updateContainer(container);
            } catch (error) {
                this.logger.error(`Error updating container in guild ${container.guildId}:`, error);
            }
        }

        this.logger.info('GitHub container update completed');
    }

    private async updateContainer(container: StoredGitHubContainer): Promise<void> {
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

            const stats = await this.githubStatsService.fetchGitHubStats();
            const newContainer = GitHubContainerBuilder.createGitHubStatsContainer(stats);

            await message.edit({
                components: [newContainer],
                flags: MessageFlags.IsComponentsV2
            });
            this.logger.info(`Updated GitHub container in guild ${container.guildId}, channel ${container.channelId}`);

        } catch (error) {
            this.logger.error(`Error updating GitHub container:`, error);
            throw error;
        }
    }

    public stop(): void {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.logger.info('GitHub update service stopped');
        }
    }
} 