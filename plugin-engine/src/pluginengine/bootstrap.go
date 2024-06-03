package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/server"
	"plugin-engine/src/pluginengine/utils"
	"plugin-engine/src/pluginengine/worker"
	"sync"
)

var PluginEngineLogger = utils.GetLogger(consts.LogFilesPath, consts.SystemLoggerName)

func main() {

	defer func() {

		if err := recover(); err != nil {

			PluginEngineLogger.Error(fmt.Sprintf("Panic recovered: %v", err))

		}

	}()

	err := server.Start()

	if err != nil {

		PluginEngineLogger.Error("ZMQ server failed to start: " + err.Error())

		return

	}

	PluginEngineLogger.Info("Starting Plugin Engine")

	for {
		request := <-consts.Requests

		PluginEngineLogger.Debug(request)

		var contexts []map[string]interface{}

		decodedContext, err := base64.StdEncoding.DecodeString(request)

		if err != nil {

			PluginEngineLogger.Error(fmt.Sprintf("base64 decoding error: %s", err.Error()))

			continue

		}

		err = json.Unmarshal(decodedContext, &contexts)

		if err != nil {

			PluginEngineLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

			continue

		}

		PluginEngineLogger.Info(string(decodedContext))

		var wg sync.WaitGroup

		jobQueue := make(chan worker.Job, len(contexts))

		worker.CreateWorkerPool(&wg, jobQueue)

		for _, context := range contexts {

			job := worker.Job{

				Context: context,
			}

			jobQueue <- job

		}

		close(jobQueue)

		wg.Wait()
	}

}
