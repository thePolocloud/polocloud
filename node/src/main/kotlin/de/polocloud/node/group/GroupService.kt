package de.polocloud.node.group

import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.property.Properties
import org.slf4j.LoggerFactory

open class GroupService(private val platformService: PlatformService = PlatformService()) {

    private var logger = LoggerFactory.getLogger(GroupService::class.java)

    fun run() {
        GroupTemplateService.ensureGlobalTemplates()
        logger.info("Found {} groups", GroupRepository.count())
    }

    open fun findAll() = GroupRepository.findAll()

    open fun exists(name: String) = GroupRepository.exists(name)

    open fun find(name: String) = GroupRepository.find(name)

    fun create(name: String, memory: Int, startThreshold: Double, minOnline: Long, maxOnline: Long, platform: String, version: String) : Group =
        create(Group(name, memory, startThreshold, minOnline, maxOnline, platform, version))

    /**
     * Persists [group], assigning its default templates first if none were set:
     * [GroupTemplateService.GLOBAL], the role-specific global template
     * ([GroupTemplateService.GLOBAL_PROXY] or [GroupTemplateService.GLOBAL_SERVER],
     * depending on the group's platform type) and finally a template named after the
     * group itself. Both entry points — the terminal `group create` command and the
     * `CreateGroup` RPC — converge here, so the defaults apply regardless of path.
     *
     * A caller that already set [Group.templates] explicitly (e.g. re-creating a group
     * from a backup) is left untouched.
     */
    open fun create(group: Group): Group {
        val withTemplates = applyDefaultTemplates(group)
        GroupRepository.save(withTemplates)
        return withTemplates
    }

    /**
     * Returns [group] unchanged if it already has templates, otherwise a copy with the
     * default list assigned (and their folders ensured on disk). Split out from [create]
     * — which also persists — so the defaulting itself is testable without a database.
     */
    internal fun applyDefaultTemplates(group: Group): Group {
        if (group.templates.isNotEmpty()) return group
        val templates = defaultTemplatesFor(group)
        templates.forEach(GroupTemplateService::ensure)
        return group.copy(templatesJson = TemplateCodec.encode(templates))
    }

    private fun defaultTemplatesFor(group: Group): List<String> {
        val roleTemplate = roleTemplateFor(platformService.find(group.platform)?.type)
        return listOf(GroupTemplateService.GLOBAL, roleTemplate, group.name)
    }

    companion object {
        /**
         * [GroupTemplateService.GLOBAL_PROXY] for a "PROXY" platform type,
         * [GroupTemplateService.GLOBAL_SERVER] otherwise — including when [platformType]
         * is `null` (platform not loaded/resolvable), which is treated as "not a proxy"
         * rather than failing group creation over it.
         */
        internal fun roleTemplateFor(platformType: String?): String =
            if (platformType.equals("PROXY", ignoreCase = true)) GroupTemplateService.GLOBAL_PROXY else GroupTemplateService.GLOBAL_SERVER
    }

    open fun update(group: Group): Group {
        GroupRepository.save(group)
        // Notify consumers (e.g. the bridge's fallback tracking) of the new state live,
        // regardless of whether the update came from gRPC or the node terminal.
        ClusterEventService.call(GroupUpdatedEvent(group.name, Properties.of(group.properties)))
        return group
    }

    fun list() = GroupRepository.findAll()

    /**
     * Deletes [group]. Callers must stop its running/queued services across the cluster
     * first (see [de.polocloud.node.services.cluster.ClusterGroupShutdown]) — the group
     * table's foreign-key constraint otherwise rejects the delete while any Service row
     * still references it.
     */
    open fun delete(group: Group) {
        GroupRepository.delete(group)
    }
}