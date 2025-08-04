import { Client, GatewayIntentBits, ActivityType } from 'discord.js';
import { config } from "dotenv";
import { Bot } from "./structures/Bot";
import { Logger } from "./utils/Logger";

config();

const logger = new Logger('Main')

async function main(): Promise<void> {
    try {
        logger.info('Starting PoloCloud Discord Bot...');

        const client = new Client({
            intents: [
                GatewayIntentBits.Guilds,
                GatewayIntentBits.GuildMessages,
                GatewayIntentBits.MessageContent,
            ],
        });

        const bot = new Bot(client);

        // Event Handlers
        client.on('ready', () => {
            logger.info(`Bot is online as ${client.user?.tag}`);

            // Set bot status
            client.user?.setActivity(process.env['BOT_STATUS'] || 'PoloCloud', {
                type: ActivityType.Watching,
            });
        });

        // Start bot
        await client.login(process.env['DISCORD_TOKEN']);

    } catch (error) {
        logger.error('Error starting bot:', error);
        process.exit(1);
    }
}

process.on('SIGINT', () => {
    logger.info('Bot is shutting down...');
    process.exit(0);
});

process.on('SIGTERM', () => {
    logger.info('Bot is shutting down...');
    process.exit(0);
});

main().catch((error) => {
    logger.error('Unhandled error:', error);
    process.exit(1);
});