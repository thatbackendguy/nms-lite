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

	server.Start()

	PluginEngineLogger.Info("Starting Plugin Engine")

	for {

		select {

		case request := <-consts.Requests:

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

			wg := sync.WaitGroup{}

			jobQueue := make(chan worker.Job, len(contexts))

			resultQueue := make(chan map[string]interface{}, len(contexts))

			go func() {

				worker.CreateWorkerPool(&wg, jobQueue)

				wg.Wait()

				close(jobQueue)

				close(resultQueue)

				return
			}()

			for _, context := range contexts {

				job := worker.Job{

					Context: context,

					Result: resultQueue,
				}

				jobQueue <- job

			}

			results := make([]map[string]interface{}, 0, len(contexts))

			for i := 0; i < len(contexts); i++ {

				result := <-resultQueue

				results = append(results, result)

			}

			jsonOutput, err := json.Marshal(results)

			if err != nil {

				PluginEngineLogger.Error("JSON Marshal Error: " + err.Error())

				continue

			}

			encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

			PluginEngineLogger.Info(encodedString)

			consts.Responses <- encodedString

			PluginEngineLogger.Info(fmt.Sprintf("Result sent to socket"))
		}

	}

}
