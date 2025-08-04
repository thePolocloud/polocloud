import { ChatInputCommandInteraction, SlashCommandBuilder, PermissionFlagsBits, TextChannel, ContainerBuilder, MessageFlags } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';
import { ContributorsUpdateService } from '../services/contributors/ContributorsUpdateService';

export class RemoveContributorsEmbedCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('removecontributorsembed')
        .setDescription('Removes the permanent contributors container from this channel')
        .setDefaultMemberPermissions(PermissionFlagsBits.ManageMessages);

    private logger: Logger;
    private contributorsUpdateService: ContributorsUpdateService;

    constructor(contributorsUpdateService: ContributorsUpdateService) {
        this.logger = new Logger('RemoveContributorsEmbedCommand');
        this.contributorsUpdateService = contributorsUpdateService;
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const guild = interaction.guild;
            if (!guild) {
                await interaction.editReply('This command can only be used in a server.');
                return;
            }

            const channel = interaction.channel as TextChannel;
            if (!channel) {
                await interaction.editReply('This command can only be used in a text channel.');
                return;
            }

            const existingMessages = await channel.messages.fetch({ limit: 100 });
            const existingContributorsMessage = existingMessages.find(msg => {
                if (!msg.components.length || !msg.flags?.has(MessageFlags.IsComponentsV2)) {
                    return false;
                }

                const container = msg.components[0];
                if (!(container instanceof ContainerBuilder)) {
                    return false;
                }

                try {
                    const containerData = container.toJSON();
                    const content = JSON.stringify(containerData).toLowerCase();
                    return content.includes('contributors') || content.includes('polocloud contributors');
                } catch (error) {
                    return true;
                }
            });

            if (!existingContributorsMessage) {
                await interaction.editReply('No contributors container found in this channel.');
                return;
            }

            await this.contributorsUpdateService.removeContainer(guild.id, channel.id);

            await existingContributorsMessage.delete();

            await interaction.editReply('✅ Contributors container removed successfully!');

            this.logger.info(`Removed contributors container from guild ${guild.id}, channel ${channel.id}`);

        } catch (error) {
            this.logger.error('Error executing RemoveContributorsEmbed command:', error);
            await interaction.editReply('An error occurred while removing the contributors container. Please try again later.');
        }
    }
}