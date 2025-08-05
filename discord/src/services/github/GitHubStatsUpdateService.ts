import { Client, TextChannel, MessageFlags } from 'discord.js';
import { Logger } from '../../utils/Logger';
import { GitHubContainerBuilder } from '../../utils/GitHubContainerBuilder';
import { StoredGitHubContainer } from '../../interfaces/GitHubStats';
import { GitHubStatsService } from './GitHubStatsService';

export class GitHubStatsUpdateService {
    private storedContainers: StoredGitHubContainer[] = [];
    private logger: Logger;
    private client: Client;
    private updateInterval?: NodeJS.Timeout;
    private statsService: GitHubStatsService;

    constructor(client: Client) {
        this.client = client;
        this.logger = new Logger('GitHubStatsUpdateService');
        this.statsService = new GitHubStatsService();
        this.startUpdateInterval();
    }

    private startUpdateInterval(): void {
        this.updateInterval = setInterval(() => {
            this.updateAllContainers().catch(error => {
                this.logger.error('Error in update interval:', error);
            });
        }, 900000);
    }

    public addContainer(guildId: string, channelId: string, messageId: string): void {
        const existingContainer = this.storedContainers.find(
            container => container.guildId === guildId && container.channelId === channelId
        );

        if (existingContainer) {
            this.removeContainer(guildId, channelId);
        }

        this.storedContainers.push({
            guildId,
            channelId,
            messageId,
            lastUpdate: Date.now()
        });

        this.logger.info(`Added GitHub stats container for guild ${guildId}, channel ${channelId}`);
    }

    public async removeContainer(guildId: string, channelId: string): Promise<string | null> {
        const container = this.storedContainers.find(
            c => c.guildId === guildId && c.channelId === channelId
        );
        
        if (container) {
            this.storedContainers = this.storedContainers.filter(
                c => !(c.guildId === guildId && c.channelId === channelId)
            );
            this.logger.info(`Removed GitHub stats container in guild ${guildId}, channel ${channelId}`);
            return container.messageId;
        }
        
        this.logger.info(`No GitHub stats container found in guild ${guildId}, channel ${channelId}`);
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

            const stats = await this.statsService.fetchGitHubStats();
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