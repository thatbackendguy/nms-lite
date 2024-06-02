package workerpool

import (
	"fmt"
	"plugin-engine/global"
	"plugin-engine/logger"
	"plugin-engine/plugins/snmp"
	"plugin-engine/utils"
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

var workerPoolLogger = logger.NewLogger(global.LogFilesPath, "worker-pool")

const numWorkers = 10 // Adjust this value based on your requirements

type Job struct {
	Context map[string]interface{}

	Result chan map[string]interface{}
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
}

func processJob(context map[string]interface{}) {

	errors := make([]map[string]interface{}, 0)

	defer func(context map[string]interface{}) {

		if r := recover(); r != nil {

			workerPoolLogger.Error(fmt.Sprintf("some error occurred!, reason : %v", r))

			context[global.Status] = global.Failed
		}

	}(context)

	if pluginName, ok := context[PluginName].(string); ok {

		switch pluginName {

		case NetworkDevice:

			if requestType, ok := context[RequestType].(string); ok {

				if strings.EqualFold(requestType, Discovery) {

					workerPoolLogger.Trace(fmt.Sprintf("Discovery request: %v", context[snmp.ObjectIp]))

					snmp.Discovery(context, &errors)

				} else if strings.EqualFold(requestType, Collect) {

					workerPoolLogger.Trace(fmt.Sprintf("Collect request: %v", context[snmp.ObjectIp]))

					snmp.Collect(context, &errors)

				} else {

					workerPoolLogger.Trace(fmt.Sprintf("Check availability request: %v", context[snmp.ObjectIp]))

					utils.CheckAvailability(context)

				}
			}

		default:

			workerPoolLogger.Error("Unsupported plugin type!")
		}
	}

	context[global.Error] = errors

	if _, ok := context[global.Result]; ok {

		if len(context[global.Result].(map[string]interface{})) <= 0 && len(errors) > 0 {

			context[global.Status] = global.Failed

		} else {

			context[global.Status] = global.Success

		}

	} else {

		context[global.Status] = global.Failed

	}
}
