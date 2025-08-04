import { Client } from 'discord.js';
import { Logger } from '../utils/Logger';

export class Bot {
    private client: Client;
    private logger: Logger;

    constructor(client: Client) {
        this.client = client;
        this.logger = new Logger('Bot');
    }
}