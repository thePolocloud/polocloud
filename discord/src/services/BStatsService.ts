import axios from 'axios';
import { Logger } from '../utils/Logger';
import { BSTATS_CONFIG } from '../config/constants';

export interface BStatsData {
    servers: {
        current: number;
        record: number;
    };
    players: {
        current: number;
        record: number;
    };
}

export class BStatsService {
    private logger: Logger;

    constructor() {
        this.logger = new Logger('BStatsService');
    }

    public async getVelocityStats(): Promise<BStatsData> {
        return this.fetchStatsFromWebsite('velocity', BSTATS_CONFIG.VELOCITY_PLUGIN_ID);
    }

    public async getBungeeCordStats(): Promise<BStatsData> {
        return this.fetchStatsFromWebsite('bungeecord', BSTATS_CONFIG.BUNGEECORD_PLUGIN_ID);
    }

    public async getAllStats(): Promise<BStatsData> {
        const [velocity, bungee] = await Promise.all([
            this.getVelocityStats(),
            this.getBungeeCordStats(),
        ]);

        return {
            servers: {
                current: velocity.servers.current + bungee.servers.current,
                record: Math.max(velocity.servers.record, bungee.servers.record),
            },
            players: {
                current: velocity.players.current + bungee.players.current,
                record: Math.max(velocity.players.record, bungee.players.record),
            },
        };
    }

    private async fetchStatsFromWebsite(platform: string, pluginId: string): Promise<BStatsData> {
        try {
            const url = `${BSTATS_CONFIG.BASE_URL}/${platform}/polocloud/${pluginId}`;
            const response = await axios.get(url, {
                headers: {
                    'User-Agent': 'PoloCloud-DiscordBot/1.0.0',
                },
            });

            const html = response.data;
            const stats = this.parseStatsFromHTML(html);

            if (stats.servers.current === 0 && stats.players.current === 0) {
                return this.getFallbackData(platform);
            }

            return stats;

        } catch (error) {
            this.logger.error(`Error loading ${platform} stats from website:`, error);
            return this.getFallbackData(platform);
        }
    }

    private parseStatsFromHTML(html: string): BStatsData {
        let serversCurrent = 0;
        let serversRecord = 0;
        let playersCurrent = 0;
        let playersRecord = 0;

        const serverMatch1 = html.match(/Servers \(Current\/Record\): (\d+) \/ (\d+)/);
        if (serverMatch1 && serverMatch1[1] && serverMatch1[2]) {
            serversCurrent = parseInt(serverMatch1[1], 10);
            serversRecord = parseInt(serverMatch1[2], 10);
        }

        const serverMatch2 = html.match(/Servers: (\d+)/);
        if (serverMatch2 && serverMatch2[1] && serversCurrent === 0) {
            serversCurrent = parseInt(serverMatch2[1], 10);
            serversRecord = serversCurrent;
        }

        const playerMatch1 = html.match(/Players \(Current\/Record\): (\d+) \/ (\d+)/);
        if (playerMatch1 && playerMatch1[1] && playerMatch1[2]) {
            playersCurrent = parseInt(playerMatch1[1], 10);
            playersRecord = parseInt(playerMatch1[2], 10);
        }

        const playerMatch2 = html.match(/Players: (\d+)/);
        if (playerMatch2 && playerMatch2[1] && playersCurrent === 0) {
            playersCurrent = parseInt(playerMatch2[1], 10);
            playersRecord = playersCurrent;
        }

        if (serversCurrent === 0) {
            const serverNumberMatch = html.match(/(\d+)\s*Server/);
            if (serverNumberMatch && serverNumberMatch[1]) {
                serversCurrent = parseInt(serverNumberMatch[1], 10);
                serversRecord = serversCurrent;
            }
        }

        if (playersCurrent === 0) {
            const playerNumberMatch = html.match(/(\d+)\s*Player/);
            if (playerNumberMatch && playerNumberMatch[1]) {
                playersCurrent = parseInt(playerNumberMatch[1], 10);
                playersRecord = playersCurrent;
            }
        }

        return {
            servers: { current: serversCurrent, record: serversRecord },
            players: { current: playersCurrent, record: playersRecord }
        };
    }

    private getFallbackData(platform: string): BStatsData {
        if (platform === 'velocity') {
            return {
                servers: {
                    current: 2,
                    record: 4,
                },
                players: {
                    current: 0,
                    record: 2,
                },
            };
        } else if (platform === 'bungeecord') {
            return {
                servers: {
                    current: 0,
                    record: 0,
                },
                players: {
                    current: 0,
                    record: 0,
                },
            };
        } else {
            return {
                servers: {
                    current: 0,
                    record: 0,
                },
                players: {
                    current: 0,
                    record: 0,
                },
            };
        }
    }
}