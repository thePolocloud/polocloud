import {
    ChatInputCommandInteraction,
    SlashCommandBuilder,
    PermissionFlagsBits,
    TextChannel,
    ContainerBuilder,
    MessageFlags,
    Colors
} from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { BOT_CONFIG, APPLY_CONFIG } from '../../config/constants';
import { ApplyService} from "../../services/apply/ApplyService";

export class ApplyCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('apply')
        .setDescription('Create the application system container in the configured channel')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;
    private applyService: ApplyService;

    constructor(applyService: ApplyService) {
        this.logger = new Logger('ApplyCommand');
        this.applyService = applyService;
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });
            
            if (!APPLY_CONFIG.CHANNEL_ID) {
                await interaction.editReply('Application channel ID is not configured. Please set APPLY_CHANNEL_ID in your environment variables.');
                return;
            }
            
            const applyChannel = interaction.guild?.channels.cache.get(APPLY_CONFIG.CHANNEL_ID) as TextChannel;

            if (!applyChannel) {
                await interaction.editReply('Application channel not found. Please check your APPLY_CHANNEL_ID configuration.');
                return;
            }
            
            if (!APPLY_CONFIG.CATEGORY_ID) {
                await interaction.editReply('Application category ID is not configured. Please set APPLY_CATEGORY_ID in your environment variables.');
                return;
            }
            
            const categoryChannel = interaction.guild?.channels.cache.get(APPLY_CONFIG.CATEGORY_ID);
            if (!categoryChannel) {
                await interaction.editReply('Application category not found. Please check your APPLY_CATEGORY_ID configuration.');
                return;
            }

            await this.applyService.createApplyContainer(applyChannel);

            const successContainer = this.createSuccessContainer(applyChannel);

            await interaction.editReply({
                components: [successContainer],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`Application system created by ${interaction.user.tag} in channel #${applyChannel.name}`);

        } catch (error) {
            this.logger.error('Error executing Apply command:', error);

            const errorContainer = this.createErrorContainer();

            if (!interaction.replied && !interaction.deferred) {
                await interaction.reply({
                    components: [errorContainer],
                    flags: MessageFlags.IsComponentsV2,
                    ephemeral: true
                });
            } else {
                await interaction.editReply({
                    components: [errorContainer],
                    flags: MessageFlags.IsComponentsV2
                });
            }
        }
    }

    private createSuccessContainer(applyChannel: TextChannel): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Green);
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# ✅ Application System Created\n\nApplication system has been set up in <#${applyChannel.id}>`)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        const configText = `## 📋 Configuration\n\n**Channel:** <#${applyChannel.id}>\n**Category:** <#${APPLY_CONFIG.CATEGORY_ID}>\n**Max Applications per User:** \`${APPLY_CONFIG.MAX_APPLICATIONS_PER_USER}\``;
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(configText)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        const categoriesText = `## 📝 Application Types\n\n${APPLY_CONFIG.CATEGORIES.map(cat => `${cat.emoji} ${cat.label}`).join('\n')}`;
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(categoriesText)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
        );

        return container;
    }

    private createErrorContainer(): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Red);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# Error\n\nAn error occurred while creating the application system. Please check your configuration and try again.`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
        );

        return container;
    }
} 