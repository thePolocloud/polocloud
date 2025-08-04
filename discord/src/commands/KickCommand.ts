import { ChatInputCommandInteraction, SlashCommandBuilder, Colors, PermissionFlagsBits, GuildMember, ContainerBuilder, MessageFlags } from 'discord.js';
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
                const notificationContainer = this.createKickNotificationContainer(targetUser, interaction, reason, guild);

                await targetUser.send({
                    components: [notificationContainer],
                    flags: MessageFlags.IsComponentsV2
                }).catch(() => {
                    this.logger.info(`Could not send kick notification to ${targetUser.tag} - DMs likely disabled`);
                });
            } catch (error) {
                this.logger.warn(`Could not send kick notification to ${targetUser.tag}:`, error);
            }

            await targetMember.kick(reason);

            const container = this.createKickSuccessContainer(targetUser, interaction, reason);

            await interaction.editReply({
                components: [container],
                flags: MessageFlags.IsComponentsV2
            });

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

    private createKickSuccessContainer(targetUser: any, interaction: any, reason: string): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Red);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 👢 User Successfully Kicked\n\n**${targetUser.tag}** has been kicked from the server.`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 👤 Kicked User\n\n**${targetUser.tag}**\n\`${targetUser.id}\``)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 🛡️ Moderator\n\n**${interaction.user.tag}**\n\`${interaction.user.id}\``)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📅 Date & Time\n\n<t:${Math.floor(Date.now() / 1000)}:F>`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📝 Reason\n\n${reason.length > 0 ? `\`\`\`${reason}\`\`\`` : '*No reason provided*'}`)
        );

        return container;
    }

    private createKickNotificationContainer(targetUser: any, interaction: any, reason: string, guild: any): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Red);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 🚫 Kick Notification\n\n**${targetUser.tag}** has been kicked from **${guild.name}** for the following reason:`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📝 Reason\n\n\`\`\`${reason}\`\`\`\n\nThis notification was sent by **${interaction.user.tag}**`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📅 Date & Time\n\n<t:${Math.floor(Date.now() / 1000)}:F>`)
        );

        return container;
    }
}