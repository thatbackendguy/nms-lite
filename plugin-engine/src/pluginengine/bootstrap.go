package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	. "plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/server"
	"plugin-engine/src/pluginengine/utils"
	"plugin-engine/src/pluginengine/workerpool"
	"sync"
)

var PluginEngineLogger = utils.GetLogger(LogFilesPath, SystemLoggerName)

func main() {

	defer func() {

		if err := recover(); err != nil {

			PluginEngineLogger.Error(fmt.Sprintf("Panic recovered: %v", err))

		}

	}()

	receiver, sender, err := server.Init()

	if err != nil {

		PluginEngineLogger.Error("Error creating zmq sockets server" + err.Error())

		return

	}

	server.Start(receiver, sender)

	PluginEngineLogger.Info("Starting Plugin Engine")

	for {

		select {

		case encodedContext := <-Requests:

			var contexts []map[string]interface{}

			decodedContext, err := base64.StdEncoding.DecodeString(encodedContext)

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

			jobQueue := make(chan workerpool.Job, len(contexts))

			resultQueue := make(chan map[string]interface{}, len(contexts))

			go func() {

				workerpool.CreateWorkerPool(&wg, jobQueue)

				wg.Wait()

				close(jobQueue)

				close(resultQueue)

				return
			}()

			for _, context := range contexts {

				job := workerpool.Job{

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

			Responses <- encodedString

			PluginEngineLogger.Info(fmt.Sprintf("Result sent to socket"))
		}

	}

}
