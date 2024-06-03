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
)

var (
	Requests = make(chan string, 10)

	Responses = make(chan string, 10)
)
