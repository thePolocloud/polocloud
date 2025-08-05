export interface TempBan {
    userId: string;
    guildId: string;
    moderatorId: string;
    reason: string;
    banTime: number;
    unbanTime: number;
    duration: string;
} 