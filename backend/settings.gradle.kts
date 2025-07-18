rootProject.name = "haven-backend"

include("platform-bom")
include("shared-kernel")
include("event-store")

// bounded contexts
include("modules:client-profile")
include("modules:case-mgmt")
include("modules:program-enrollment")
include("modules:incident-tracking")
include("modules:user-access")
include("modules:reporting")

// applications
include("apps:api-app")
include("apps:reporting-app")
include("apps:worker-app")