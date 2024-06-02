package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"plugin-engine/global"
	"plugin-engine/logger"
	"plugin-engine/server"
	"plugin-engine/workerpool"
	"sync"
)

var PluginEngineLogger = logger.NewLogger(global.LogFilesPath, global.SystemLoggerName)

func main() {

	defer func() {

		if err := recover(); err != nil {

			PluginEngineLogger.Error(err)

		}

	}()

	receiver, sender, err := server.Init()

	if err != nil {

		PluginEngineLogger.Error(err.Error())

		return

	}

	err = server.Start(receiver, sender)

	if err != nil {

		PluginEngineLogger.Error("Error in starting zmq server: " + err.Error())

		return

	}

	PluginEngineLogger.Info("Starting Plugin Engine")

	for {

		select {

		case encodedContext := <-global.Requests:

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

			jobQueue := make(chan workerpool.Job, len(contexts))

			resultQueue := make(chan map[string]interface{}, len(contexts))

			for _, context := range contexts {

				job := workerpool.Job{

					Context: context,

					Result: resultQueue,
				}

				jobQueue <- job

			}

			wg := sync.WaitGroup{}

			go func() {

				workerpool.CreateWorkerPool(&wg, jobQueue)

				wg.Wait()

				close(jobQueue)

				close(resultQueue)
			}()

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

			global.Responses <- encodedString

			PluginEngineLogger.Info(fmt.Sprintf("Result sent to socket"))
		}

	}

}
