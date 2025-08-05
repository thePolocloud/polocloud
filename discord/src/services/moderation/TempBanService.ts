import { Guild, Client } from 'discord.js';
import { Logger } from '../../utils/Logger';
import { TempBan } from '../../interfaces/TempBan';
import * as fs from 'fs';
import * as path from 'path';

export class TempBanService {
    private tempBans: Map<string, TempBan> = new Map();
    private logger: Logger;
    private dataPath: string;
    private checkInterval?: NodeJS.Timeout;
    private client?: Client;

    constructor(client?: Client) {
        this.logger = new Logger('TempBanService');
        if (client) {
            this.client = client;
        }
        this.dataPath = path.join(process.cwd(), 'data', 'tempbans.json');
        this.loadTempBans();
        this.startUnbanChecker();
    }

    public setClient(client: Client): void {
        this.client = client;
    }

    private loadTempBans(): void {
        try {
            if (!fs.existsSync(this.dataPath)) {
                this.ensureDataDirectory();
                this.saveTempBans();
                return;
            }

            const data = fs.readFileSync(this.dataPath, 'utf8');
            const tempBansArray: TempBan[] = JSON.parse(data);
            
            this.tempBans.clear();
            tempBansArray.forEach(ban => {
                const key = `${ban.guildId}-${ban.userId}`;
                this.tempBans.set(key, ban);
            });

            this.logger.info(`Loaded ${this.tempBans.size} temporary bans from storage`);
        } catch (error) {
            this.logger.error('Error loading temp bans:', error);
        }
    }

    private saveTempBans(): void {
        try {
            this.ensureDataDirectory();
            const tempBansArray = Array.from(this.tempBans.values());
            fs.writeFileSync(this.dataPath, JSON.stringify(tempBansArray, null, 2));
        } catch (error) {
            this.logger.error('Error saving temp bans:', error);
        }
    }

    private ensureDataDirectory(): void {
        const dir = path.dirname(this.dataPath);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
    }

    public addTempBan(
        guildId: string,
        userId: string,
        moderatorId: string,
        reason: string,
        durationMs: number,
        durationText: string
    ): void {
        const key = `${guildId}-${userId}`;
        const tempBan: TempBan = {
            userId,
            guildId,
            moderatorId,
            reason,
            banTime: Date.now(),
            unbanTime: Date.now() + durationMs,
            duration: durationText
        };

        this.tempBans.set(key, tempBan);
        this.saveTempBans();
        this.logger.info(`Added temp ban for ${userId} in guild ${guildId} for ${durationText}`);
    }

    public scheduleUnban(user: any, durationMs: number, reason: string): void {
        const guildId = user.guild?.id;
        if (!guildId) {
            this.logger.error('Cannot schedule unban: no guild found');
            return;
        }

        this.addTempBan(
            guildId,
            user.id,
            user.moderatorId || 'Unknown',
            reason,
            durationMs,
            this.formatDuration(durationMs)
        );
    }

    private formatDuration(milliseconds: number): string {
        const seconds = Math.floor(milliseconds / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);
        const weeks = Math.floor(days / 7);
        const months = Math.floor(days / 30);
        const years = Math.floor(days / 365);

        if (years > 0) return `${years} year${years > 1 ? 's' : ''}`;
        if (months > 0) return `${months} month${months > 1 ? 's' : ''}`;
        if (weeks > 0) return `${weeks} week${weeks > 1 ? 's' : ''}`;
        if (days > 0) return `${days} day${days > 1 ? 's' : ''}`;
        if (hours > 0) return `${hours} hour${hours > 1 ? 's' : ''}`;
        if (minutes > 0) return `${minutes} minute${minutes > 1 ? 's' : ''}`;
        return `${seconds} second${seconds > 1 ? 's' : ''}`;
    }

    public removeTempBan(guildId: string, userId: string): boolean {
        const key = `${guildId}-${userId}`;
        const removed = this.tempBans.delete(key);
        if (removed) {
            this.saveTempBans();
            this.logger.info(`Removed temp ban for ${userId} in guild ${guildId}`);
        }
        return removed;
    }

    public getTempBan(guildId: string, userId: string): TempBan | undefined {
        const key = `${guildId}-${userId}`;
        return this.tempBans.get(key);
    }

    public getAllTempBans(): TempBan[] {
        return Array.from(this.tempBans.values());
    }

    public getExpiredTempBans(): TempBan[] {
        const now = Date.now();
        return Array.from(this.tempBans.values()).filter(ban => ban.unbanTime <= now);
    }

    private startUnbanChecker(): void {
        this.checkInterval = setInterval(async () => {
            const expiredBans = this.getExpiredTempBans();
            
            for (const ban of expiredBans) {
                try {
                    const guild = await this.getGuild(ban.guildId);
                    if (guild) {
                        await guild.members.unban(ban.userId, `Automatic unban after ${ban.duration}`);
                        this.removeTempBan(ban.guildId, ban.userId);
                        this.logger.info(`Automatically unbanned ${ban.userId} from guild ${ban.guildId} after ${ban.duration}`);
                    }
                } catch (error) {
                    this.logger.error(`Failed to automatically unban ${ban.userId} from guild ${ban.guildId}:`, error);
                    this.removeTempBan(ban.guildId, ban.userId);
                }
            }
        }, 60000);

        this.logger.info('Temp ban checker started');
    }

    private async getGuild(guildId: string): Promise<Guild | null> {
        try {
            if (!this.client) {
                this.logger.error('Client not set for TempBanService');
                return null;
            }
            
            const guild = this.client.guilds.cache.get(guildId);
            return guild || null;
        } catch (error) {
            this.logger.error(`Error getting guild ${guildId}:`, error);
            return null;
        }
    }

    public stop(): void {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
            this.logger.info('Temp ban checker stopped');
        }
    }
} 