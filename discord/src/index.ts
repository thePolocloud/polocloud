import { config } from 'dotenv';
import { Bot } from './structures/Bot';
import { Logger } from './utils/Logger';

config();

const logger = new Logger('Main');

async function main(): Promise<void> {
    try {
        logger.info('Starting PoloCloud Discord Bot...');

        const bot = new Bot();
        await bot.start();

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