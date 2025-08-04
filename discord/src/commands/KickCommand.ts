import { ChatInputCommandInteraction, SlashCommandBuilder, EmbedBuilder, Colors, PermissionFlagsBits, GuildMember } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';

export class KickCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('kick')
        .setDescription('Kicks a user from the server')
        .addUserOption(option =>
            option
                .setName('user')
                .setDescription('The user to kick')
                .setRequired(true)
        )
        .addStringOption(option =>
            option
                .setName('reason')
                .setDescription('Reason for kicking the user')
                .setRequired(false)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.KickMembers);

    private logger: Logger;

    constructor() {
        this.logger = new Logger('KickCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const targetUser = interaction.options.getUser('user');
            const reason = interaction.options.getString('reason') || 'No reason provided';
            const guild = interaction.guild;

            if (!guild) {
                await interaction.editReply('This command can only be used in a server.');
                return;
            }

            if (!targetUser) {
                await interaction.editReply('Please specify a valid user to kick.');
                return;
            }

            if (targetUser.id === interaction.user.id) {
                await interaction.editReply('You cannot kick yourself.');
                return;
            }

            if (targetUser.id === interaction.client.user?.id) {
                await interaction.editReply('I cannot kick myself.');
                return;
            }

            const targetMember = await guild.members.fetch(targetUser.id).catch(() => null);

            if (!targetMember) {
                await interaction.editReply('The specified user is not a member of this server.');
                return;
            }

            if (!targetMember.kickable) {
                await interaction.editReply('I cannot kick this user. They may have higher permissions than me.');
                return;
            }

            const commandMember = interaction.member as GuildMember;
            if (targetMember.roles.highest.position >= commandMember.roles.highest.position) {
                await interaction.editReply('You cannot kick this user. They have equal or higher permissions than you.');
                return;
            }

            try {
                const notificationEmbed = new EmbedBuilder()
                    .setTitle('🚫 You have been kicked')
                    .setDescription(`You have been kicked from **${guild.name}**.`)
                    .setColor(Colors.Red)
                    .setThumbnail(guild.iconURL({ size: 256 }))
                    .addFields(
                        {
                            name: '🛡️ Moderator',
                            value: `**${interaction.user.tag}**`,
                            inline: true
                        },
                        {
                            name: '📅 Date & Time',
                            value: `<t:${Math.floor(Date.now() / 1000)}:F>`,
                            inline: true
                        },
                        {
                            name: '📝 Reason',
                            value: reason.length > 0 ? `\`\`\`${reason}\`\`\`` : '*No reason provided*',
                            inline: false
                        }
                    )
                    .setTimestamp()
                    .setFooter({
                        text: `PoloCloud Discord Bot • Kick Notification`,
                        iconURL: interaction.client.user?.displayAvatarURL()
                    });

                await targetUser.send({ embeds: [notificationEmbed] }).catch(() => {
                    this.logger.info(`Could not send kick notification to ${targetUser.tag} - DMs likely disabled`);
                });
            } catch (error) {
                this.logger.warn(`Could not send kick notification to ${targetUser.tag}:`, error);
            }

            await targetMember.kick(reason);

            const embed = new EmbedBuilder()
                .setTitle('👢 User Successfully Kicked')
                .setDescription(`**${targetUser.tag}** has been kicked from the server.`)
                .setColor(Colors.Red)
                .setThumbnail(targetUser.displayAvatarURL({ size: 256 }))
                .addFields(
                    {
                        name: '👤 Kicked User',
                        value: `**${targetUser.tag}**\n\`${targetUser.id}\``,
                        inline: true
                    },
                    {
                        name: '🛡️ Staff',
                        value: `**${interaction.user.tag}**\n\`${interaction.user.id}\``,
                        inline: true
                    },
                    {
                        name: '📅 Date & Time',
                        value: `<t:${Math.floor(Date.now() / 1000)}:F>`,
                        inline: true
                    },
                    {
                        name: '📝 Reason',
                        value: reason.length > 0 ? `\`\`\`${reason}\`\`\`` : '*No reason provided*',
                        inline: false
                    }
                )
                .setTimestamp()
                .setFooter({
                    text: `PoloCloud Discord Bot • Kick Action`,
                    iconURL: interaction.client.user?.displayAvatarURL()
                });

            await interaction.editReply({ embeds: [embed] });

            this.logger.info(`User ${targetUser.tag} (${targetUser.id}) was kicked by ${interaction.user.tag} (${interaction.user.id}) for reason: ${reason}`);

        } catch (error) {
            this.logger.error('Error executing Kick command:', error);

            if (error instanceof Error && error.message.includes('Missing Permissions')) {
                await interaction.editReply('I do not have permission to kick users in this server.');
            } else {
                await interaction.editReply('An error occurred while trying to kick the user. Please try again later.');
            }
        }
    }
}