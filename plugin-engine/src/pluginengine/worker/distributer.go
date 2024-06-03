package worker

import (
	"fmt"
	"plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/plugins/snmp"
	"plugin-engine/src/pluginengine/utils"
	"strings"
	"sync"
)

const (
	RequestType   = "request.type"
	Discovery     = "Discovery"
	Collect       = "Collect"
	PluginName    = "plugin.name"
	NetworkDevice = "Network"
)

var workerPoolLogger = utils.GetLogger(consts.LogFilesPath, "worker-pool")

const numWorkers = 10

type Job struct {
	Result chan map[string]interface{}

	Context map[string]interface{}
}

func CreateWorkerPool(wg *sync.WaitGroup, jobQueue chan Job) {

	for i := 0; i < numWorkers; i++ {

		wg.Add(1)

		go worker(wg, jobQueue)

	}
}

func worker(wg *sync.WaitGroup, jobQueue chan Job) {

	defer wg.Done()

	for job := range jobQueue {

		processJob(job.Context)

		job.Result <- job.Context

	}

	return
}

func processJob(context map[string]interface{}) {

	defer func(context map[string]interface{}) {

		if r := recover(); r != nil {

			workerPoolLogger.Error(fmt.Sprintf("some error occurred!, reason : %v", r))

			context[consts.Status] = consts.Failed
		}

	}(context)

	if pluginName, ok := context[PluginName].(string); ok {

		switch pluginName {

		case NetworkDevice:

			if requestType, ok := context[RequestType].(string); ok {

				if strings.EqualFold(requestType, Discovery) {

					workerPoolLogger.Trace(fmt.Sprintf("Discovery request: %v", context[consts.ObjectIp]))

					snmp.Discovery(context)

				} else if strings.EqualFold(requestType, Collect) {

					workerPoolLogger.Trace(fmt.Sprintf("Collect request: %v", context[consts.ObjectIp]))

					snmp.Collect(context)

				} else {

					workerPoolLogger.Trace(fmt.Sprintf("Check availability request: %v", context[consts.ObjectIp]))

					utils.CheckAvailability(context)

				}
			}

		default:

			workerPoolLogger.Error("Unsupported plugin type!")
		}
	}

	return
}
