import { ChatInputCommandInteraction, SlashCommandBuilder, TextChannel, PermissionFlagsBits } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';
import { BStatsUpdateService } from '../services/bstats/BStatsUpdateService';

export class RemoveBStatsEmbedCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('removebstatsembed')
        .setDescription('Removes the bStats embed from the current channel')
        .addStringOption(option =>
            option
                .setName('platform')
                .setDescription('Choose the platform to remove')
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

    constructor(updateService: BStatsUpdateService) {
        this.logger = new Logger('RemoveBStatsEmbedCommand');
        this.updateService = updateService;
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

            await this.updateService.removeEmbed(interaction.guildId!, channel.id, platform);

            try {
                const messages = await channel.messages.fetch({ limit: 100 });
                let targetTitle: string;

                switch (platform) {
                    case 'velocity':
                        targetTitle = '☁️ PoloCloud Velocity';
                        break;
                    case 'bungeecord':
                        targetTitle = '☁️ PoloCloud BungeeCord';
                        break;
                    case 'all':
                        targetTitle = '☁️ PoloCloud Combined';
                        break;
                    default:
                        await interaction.editReply('Invalid platform selected.');
                        return;
                }

                const bStatsMessage = messages.find(msg => {
                    const embed = msg.embeds[0];
                    return embed && embed.title === targetTitle;
                });

                if (bStatsMessage) {
                    await bStatsMessage.delete();
                    await interaction.editReply(`${platform} bStats embed removed successfully!`);
                } else {
                    await interaction.editReply(`${platform} bStats embed removed from tracking. (Message not found in recent messages)`);
                }
            } catch (error) {
                this.logger.error('Error deleting bStats message:', error);
                await interaction.editReply(`${platform} bStats embed removed from tracking. (Could not delete message)`);
            }

        } catch (error) {
            this.logger.error('Error executing RemoveBStatsEmbed command:', error);
            await interaction.editReply('Error removing bStats embed. Please try again later.');
        }
    }
}