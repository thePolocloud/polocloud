import { config } from 'dotenv';
import { Bot } from './structures/Bot';
import { Logger } from './utils/Logger';

config();

const logger = new Logger('Main');
let bot: Bot;

async function main(): Promise<void> {
    try {
        logger.info('Starting PoloCloud Discord Bot...');

        bot = new Bot();
        await bot.start();

    } catch (error) {
        logger.error('Error starting bot:', error);
        process.exit(1);
    }
}

process.on('SIGINT', async () => {
    console.log('\nReceived SIGINT. Shutting down gracefully...');
    if (bot) {
        await bot.stop();
    }
    process.exit(0);
});

process.on('SIGTERM', async () => {
    console.log('\nReceived SIGTERM. Shutting down gracefully...');
    if (bot) {
        await bot.stop();
    }
    process.exit(0);
});

main().catch((error) => {
    logger.error('Unhandled error:', error);
    process.exit(1);
});