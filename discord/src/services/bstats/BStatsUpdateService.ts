import { Client, TextChannel } from 'discord.js';
import { Logger } from '../../utils/Logger';
import { BStatsService, BStatsData } from './BStatsService';
import { BStatsEmbedBuilder } from '../../utils/BStatsEmbedBuilder';
import { BSTATS_CONFIG } from '../../config/constants';

interface StoredBStatsEmbed {
    guildId: string;
    channelId: string;
    messageId: string;
    platform: string;
}

export class BStatsUpdateService {
    private client: Client;
    private logger: Logger;
    private updateInterval: NodeJS.Timeout | null = null;
    private storedEmbeds: Map<string, StoredBStatsEmbed> = new Map();
    private bStatsService: BStatsService;

    constructor(client: Client) {
        this.client = client;
        this.logger = new Logger('BStatsUpdateService');
        this.bStatsService = new BStatsService();
    }

    public start(): void {
        this.updateInterval = setInterval(async () => {
            await this.updateAllEmbeds();
        }, BSTATS_CONFIG.UPDATE_INTERVAL);

        this.logger.info('BStats Update Service started');
    }

    public stop(): void {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
        this.logger.info('BStats Update Service stopped');
    }

    public async addEmbed(guildId: string, channelId: string, messageId: string, platform: string): Promise<void> {
        const key = `${guildId}-${channelId}-${platform}`;
        this.storedEmbeds.set(key, { guildId, channelId, messageId, platform });
        this.logger.info(`Added bStats embed for tracking: ${key}`);
    }

    public async removeEmbed(guildId: string, channelId: string, platform: string): Promise<void> {
        const key = `${guildId}-${channelId}-${platform}`;
        this.storedEmbeds.delete(key);
        this.logger.info(`Removed bStats embed from tracking: ${key}`);
    }

    private async updateAllEmbeds(): Promise<void> {
        this.logger.info('Starting bStats embed update...');

        for (const [key, embed] of this.storedEmbeds) {
            try {
                await this.updateEmbed(embed);
            } catch (error) {
                this.logger.error(`Failed to update bStats embed ${key}:`, error);
            }
        }

        this.logger.info('BStats embed update completed');
    }

    private async updateEmbed(embed: StoredBStatsEmbed): Promise<void> {
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

            let stats: BStatsData;
            let title: string;

            switch (embed.platform) {
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
                    this.logger.warn(`Unknown platform: ${embed.platform}`);
                    return;
            }

            const newEmbed = BStatsEmbedBuilder.createBStatsEmbed(stats, title);

            await message.edit({ embeds: [newEmbed] });
            this.logger.info(`Updated bStats embed in guild ${embed.guildId}, channel ${embed.channelId}, platform ${embed.platform}`);

        } catch (error) {
            this.logger.error(`Error updating bStats embed:`, error);
            throw error;
        }
    }
}