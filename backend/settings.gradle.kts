rootProject.name = "haven-backend"

include("platform-bom")
include("shared-kernel")
include("event-store")

// bounded contexts
include("modules:client-profile")
include("modules:case-mgmt")
include("modules:program-enrollment")
include("modules:service-delivery")
include("modules:incident-tracking")
include("modules:user-access")
include("modules:reporting")
include("modules:financial-assistance")
include("modules:safety-assessment")
include("modules:read-models")
include("modules:reporting-metadata")
include("modules:document-mgmt")

// applications
include("apps:api-app")
include("apps:reporting-app")
include("apps:worker-app")
