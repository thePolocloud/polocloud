import { Client, TextChannel, MessageFlags } from 'discord.js';
import { Logger } from '../../utils/Logger';
import { BStatsContainerBuilder } from '../../utils/BStatsContainerBuilder';
import { BStatsServiceData, StoredBStatsContainer } from '../../interfaces/BStats';
import { BStatsService } from './BStatsService';

export class BStatsUpdateService {
    private storedContainers: StoredBStatsContainer[] = [];
    private logger: Logger;
    private client: Client;
    private updateInterval?: NodeJS.Timeout;
    private bStatsService: BStatsService;

    constructor(client: Client) {
        this.client = client;
        this.logger = new Logger('BStatsUpdateService');
        this.bStatsService = new BStatsService();
        this.startUpdateInterval();
    }

    private startUpdateInterval(): void {
        this.updateInterval = setInterval(() => {
            this.updateAllContainers().catch(error => {
                this.logger.error('Error in update interval:', error);
            });
        }, 900000);
    }

    public addContainer(guildId: string, channelId: string, messageId: string, platform: string): void {
        const existingContainer = this.storedContainers.find(
            container => container.guildId === guildId && container.channelId === channelId && container.platform === platform
        );

        if (existingContainer) {
            this.removeContainer(guildId, channelId, platform);
        }

        this.storedContainers.push({
            guildId,
            channelId,
            messageId,
            platform,
            lastUpdate: Date.now()
        });

        this.logger.info(`Added bStats container for guild ${guildId}, channel ${channelId}, platform ${platform}`);
    }

    public async removeContainer(guildId: string, channelId: string, platform: string): Promise<string | null> {
        const container = this.storedContainers.find(
            c => c.guildId === guildId && c.channelId === channelId && c.platform === platform
        );
        
        if (container) {
            this.storedContainers = this.storedContainers.filter(
                c => !(c.guildId === guildId && c.channelId === channelId && c.platform === platform)
            );
            this.logger.info(`Removed bStats container: ${platform} in guild ${guildId}, channel ${channelId}`);
            return container.messageId;
        }
        
        this.logger.info(`No bStats container found: ${platform} in guild ${guildId}, channel ${channelId}`);
        return null;
    }

    private async updateAllContainers(): Promise<void> {
        this.logger.info('Starting bStats container update...');

        for (const container of this.storedContainers) {
            try {
                await this.updateContainer(container);
            } catch (error) {
                this.logger.error(`Error updating container ${container.platform} in guild ${container.guildId}:`, error);
            }
        }

        this.logger.info('BStats container update completed');
    }

    private async updateContainer(container: StoredBStatsContainer): Promise<void> {
        try {
            const channel = await this.client.channels.fetch(container.channelId) as TextChannel;
            if (!channel) {
                this.logger.warn(`Channel ${container.channelId} not found, removing container`);
                this.removeContainer(container.guildId, container.channelId, container.platform);
                return;
            }

            const message = await channel.messages.fetch(container.messageId);
            if (!message) {
                this.logger.warn(`Message ${container.messageId} not found, removing container`);
                this.removeContainer(container.guildId, container.channelId, container.platform);
                return;
            }

            let stats: BStatsServiceData;
            let title: string;

            switch (container.platform) {
                case 'velocity':
                    stats = await this.bStatsService.getVelocityStats();
                    title = '☁️ PoloCloud Velocity';
                    break;
                case 'bungeecord':
                    stats = await this.bStatsService.getBungeeCordStats();
                    title = '☁️ PoloCloud BungeeCord';
                    break;
                case 'all':
                    stats = await this.bStatsService.getAllStats();
                    title = '☁️ PoloCloud Combined';
                    break;
                default:
                    this.logger.warn(`Unknown platform ${container.platform}, removing container`);
                    this.removeContainer(container.guildId, container.channelId, container.platform);
                    return;
            }

            const newContainer = BStatsContainerBuilder.createBStatsContainer(stats, title);

            await message.edit({
                components: [newContainer],
                flags: MessageFlags.IsComponentsV2
            });
            this.logger.info(`Updated bStats container in guild ${container.guildId}, channel ${container.channelId}, platform ${container.platform}`);
        } catch (error) {
            this.logger.error(`Error updating bStats container for guild ${container.guildId}, channel ${container.channelId}, platform ${container.platform}:`, error);
        }
    }

    public stop(): void {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.logger.info('BStats update service stopped');
        }
    }
}