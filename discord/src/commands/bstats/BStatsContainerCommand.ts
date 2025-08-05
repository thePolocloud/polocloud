import { ChatInputCommandInteraction, SlashCommandBuilder, TextChannel, PermissionFlagsBits, MessageFlags } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { BStatsUpdateService } from '../../services/bstats/BStatsUpdateService';
import { BStatsService } from '../../services/bstats/BStatsService';
import { BStatsContainerBuilder } from '../../utils/BStatsContainerBuilder';

export class BStatsContainerCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('bstatscontainer')
        .setDescription('Creates a permanent bStats container in the current channel')
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
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;
    private updateService: BStatsUpdateService;
    private bStatsService: BStatsService;

    constructor(updateService: BStatsUpdateService) {
        this.logger = new Logger('BStatsContainerCommand');
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

            const container = BStatsContainerBuilder.createBStatsContainer(stats, title);
            const message = await channel.send({
                components: [container],
                flags: MessageFlags.IsComponentsV2
            });

            await this.updateService.addContainer(interaction.guildId!, channel.id, message.id, platform);

            await interaction.editReply(`bStats container created successfully! [View Message](${message.url})\n\nThe container will automatically update every 15 minutes.`);

        } catch (error) {
            this.logger.error('Error executing BStatsContainer command:', error);
            await interaction.editReply('Error creating bStats container. Please try again later.');
        }
    }
}