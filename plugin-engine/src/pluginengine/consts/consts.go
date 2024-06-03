package consts

const (
	Result = "result"

	Status = "status"

	Success = "success"

	Failed = "failed"

	Error = "error"

	ErrorMsg = "error.message"

	ErrorCode = "error.code"

	LogFilesPath = "go-engine/logs"

	SystemLoggerName = "system"

	InterfacePhysicalAddress = "interface.physical.address"

	Localhost = "127.0.0.1"

	DefaultSnmpPort = 161
)
const (
	ObjectIp      = "object.ip"
	Community     = "community"
	Port          = "port"
	Version       = "version"
	CredProfileId = "credential.profile.id"
	Credentials   = "credentials"
	Interface     = "interface"
	PollTime      = "poll.time"
)

const (
	SystemName        = "system.name"
	SystemDescription = "system.description"
	SystemObjectId    = "system.objectId"
	SystemUptime      = "system.uptime"
	SystemInterfaces  = "system.interfaces"
	SystemLocation    = "system.location"
)

const (
	InterfaceIndexKey               = "interface.index"
	InterfaceNameKey                = "interface.name"
	InterfaceOperationalStatusKey   = "interface.operational.status"
	InterfaceAdminStatusKey         = "interface.admin.status"
	InterfaceDescriptionKey         = "interface.description"
	InterfaceSentErrorPacketKey     = "interface.sent.error.packet"
	InterfaceReceivedErrorPacketKey = "interface.received.error.packet"
	InterfaceSentOctetsKey          = "interface.sent.octets"
	InterfaceReceivedOctetsKey      = "interface.received.octets"
	InterfaceSpeedKey               = "interface.speed"
	InterfaceAliasKey               = "interface.alias"
)

var (
	Requests = make(chan string, 10)

	Responses = make(chan map[string]interface{}, 10)
)
