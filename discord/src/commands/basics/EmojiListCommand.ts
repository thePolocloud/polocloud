import {
    ChatInputCommandInteraction,
    SlashCommandBuilder,
    PermissionFlagsBits,
    ContainerBuilder,
    MessageFlags,
    Colors
} from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { BOT_CONFIG } from '../../config/constants';

export class EmojiListCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('emojilist')
        .setDescription('List all custom emojis in the server with their IDs')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;

    constructor() {
        this.logger = new Logger('EmojiListCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });
            
            const guild = interaction.guild;
            if (!guild) {
                await interaction.editReply('This command can only be used in a server.');
                return;
            }

            const emojis = guild.emojis.cache;
            
            if (emojis.size === 0) {
                await interaction.editReply('No custom emojis found in this server.');
                return;
            }

            const emojiList = emojis.map(emoji => {
                return `${emoji} \`:${emoji.name}:\` → \`<:${emoji.name}:${emoji.id}>\` → ID: \`${emoji.id}\``;
            }).join('\n');

            const container = new ContainerBuilder()
                .setAccentColor(Colors.Blue);
            
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`# 🎨 Custom Emojis in ${guild.name}\n\nTotal: **${emojis.size}** custom emojis`)
            );

            container.addSeparatorComponents(
                separator => separator
            );
            
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 📋 Emoji List\n\n${emojiList}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );
            
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
            );

            await interaction.editReply({
                components: [container],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`Emoji list displayed by ${interaction.user.tag} in guild ${guild.name}`);

        } catch (error) {
            this.logger.error('Error executing EmojiList command:', error);
            await interaction.editReply('An error occurred while fetching emoji list. Please try again.');
        }
    }
} 