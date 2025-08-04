import { ChatInputCommandInteraction, SlashCommandBuilder, TextChannel, PermissionFlagsBits } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';
import { BStatsUpdateService } from '../services/BStatsUpdateService';
import { BStatsService } from '../services/BStatsService';
import { BStatsEmbedBuilder } from '../utils/BStatsEmbedBuilder';

export class BStatsEmbedCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('bstatsembed')
        .setDescription('Creates a permanent bStats embed in the current channel')
        .addStringOption(option =>
            option
                .setName('platform')
                .setDescription('Choose the platform for statistics')
                .setRequired(true)
                .addChoices(
                    { name: 'Velocity', value: 'velocity' },
                    { name: 'BungeeCord', value: 'bungeecord' },
                    { name: 'All (Combined)', value: 'all' }
                )
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.ManageMessages);

    private logger: Logger;
    private updateService: BStatsUpdateService;
    private bStatsService: BStatsService;

    constructor(updateService: BStatsUpdateService) {
        this.logger = new Logger('BStatsEmbedCommand');
        this.updateService = updateService;
        this.bStatsService = new BStatsService();
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const channel = interaction.channel as TextChannel;
            if (!channel) {
                await interaction.editReply('This command can only be used in a text channel.');
                return;
            }

            const platform = interaction.options.getString('platform', true);
            let stats;
            let title: string;

            switch (platform) {
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
                    await interaction.editReply('Invalid platform selected.');
                    return;
            }

            const embed = BStatsEmbedBuilder.createBStatsEmbed(stats, title);
            const message = await channel.send({ embeds: [embed] });

            await this.updateService.addEmbed(interaction.guildId!, channel.id, message.id, platform);

            await interaction.editReply(`bStats embed created successfully! [View Message](${message.url})\n\nThe embed will automatically update every 15 minutes.`);

        } catch (error) {
            this.logger.error('Error executing BStatsEmbed command:', error);

            if (!interaction.replied && !interaction.deferred) {
                await interaction.reply({
                    content: 'Error creating bStats embed. Please try again later.',
                    ephemeral: true
                });
            } else {
                await interaction.editReply('Error creating bStats embed. Please try again later.');
            }
        }
    }
}