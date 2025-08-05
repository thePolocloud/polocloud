export interface BStatsServiceData {
    servers: {
        current: number;
        record: number;
    };
    players: {
        current: number;
        record: number;
    };
}

export interface BStatsData {
    servers: {
        current: number;
        record: number;
    };
    players: {
        current: number;
        record: number;
    };
    platform: string;
    lastUpdated: number;
}

export interface StoredBStatsContainer {
    guildId: string;
    channelId: string;
    messageId: string;
    platform: string;
    lastUpdate: number;
} 