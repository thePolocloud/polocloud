import { Client, GatewayIntentBits, ActivityType } from 'discord.js';
import { config } from 'dotenv';
import { Bot } from './structures/Bot';
import { Logger } from './utils/Logger';

// Load environment variables
config();

const logger = new Logger('Main');

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

        client.on('ready', async () => {
            logger.info(`Bot is online as ${client.user?.tag}`);

            client.user?.setActivity(process.env['BOT_STATUS'] || 'PoloCloud', {
                type: ActivityType.Watching,
            });

            await bot.start();
        });

        client.on('interactionCreate', async (interaction) => {
            await bot.handleInteraction(interaction);
        });

        await client.login(process.env['DISCORD_TOKEN']);

    } catch (error) {
        logger.error('Error starting bot:', error);
        process.exit(1);
    }
}

// Graceful Shutdown
process.on('SIGINT', async () => {
    logger.info('Bot is shutting down...');
    // TODO: Add bot.stop() when available
    process.exit(0);
});

process.on('SIGTERM', async () => {
    logger.info('Bot is shutting down...');
    // TODO: Add bot.stop() when available
    process.exit(0);
});

main().catch((error) => {
    logger.error('Unhandled error:', error);
    process.exit(1);
});