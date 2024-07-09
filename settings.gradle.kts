rootProject.name = "polocloud"
include("api")
include("runner")
include("base")
include("instance")
include("modules")
include("plugin")
include("addons")
include("addons:proxy-addon")
findProject(":addons:proxy-addon")?.name = "proxy-addon"
include("addons:sign-addon")
findProject(":addons:sign-addon")?.name = "sign-addon"
include("addons:hubcommand-addon")
findProject(":addons:hubcommand-addon")?.name = "hubcommand-addon"
include("addons:notify-addon")
findProject(":addons:notify-addon")?.name = "notify-addon"
include("modules:rest-api")
findProject(":modules:rest-api")?.name = "rest-api"
