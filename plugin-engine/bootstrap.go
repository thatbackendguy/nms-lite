package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"plugin-engine/global"
	"plugin-engine/logger"
	"plugin-engine/plugins/snmp"
	"plugin-engine/server"
	"plugin-engine/utils"
	"strings"
	"sync"
)

var PluginEngineLogger = logger.NewLogger(global.LogFilesPath, global.SystemLoggerName)

const (
	RequestType   = "request.type"
	Discovery     = "Discovery"
	Collect       = "Collect"
	PluginName    = "plugin.name"
	NetworkDevice = "Network"
)

func main() {

	defer func() {

		if err := recover(); err != nil {

			PluginEngineLogger.Error(err)

		}
	}()

	puller, pusher, err := server.Init()

	if err != nil {

		PluginEngineLogger.Error(err.Error())

		return
	}

	err = server.Start(puller, pusher)

	if err != nil {

		PluginEngineLogger.Error(err.Error())

		return
	}

	wg := sync.WaitGroup{}

	PluginEngineLogger.Info("Starting Plugin Engine")

	for {
		select {

		case encodedContext := <-global.Requests:

			var contexts []map[string]interface{}

			decodedContext, err := base64.StdEncoding.DecodeString(encodedContext)

			if err != nil {

				PluginEngineLogger.Error(fmt.Sprintf("base64 decoding error: %s", err.Error()))

				return

			}

			err = json.Unmarshal(decodedContext, &contexts)

			if err != nil {

				PluginEngineLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

				return
			}

			PluginEngineLogger.Info(string(decodedContext))

			for _, context := range contexts {

				wg.Add(1)

				go func(context map[string]interface{}) {

					defer wg.Done()

					errors := make([]map[string]interface{}, 0)

					defer func(context map[string]interface{}, contexts []map[string]interface{}) {

						if r := recover(); r != nil {

							PluginEngineLogger.Error(fmt.Sprintf("some error occurred!, reason : %v", r))

							context[global.Status] = global.Failed

						}
					}(context, contexts)

					if pluginName, ok := context[PluginName].(string); ok {

						switch pluginName {

						case NetworkDevice:

							if requestType, ok := context[RequestType].(string); ok {

								if strings.EqualFold(requestType, Discovery) {

									PluginEngineLogger.Trace(fmt.Sprintf("Discovery request: %v", context[snmp.ObjectIp]))

									snmp.Discovery(context, &errors)

								} else if strings.EqualFold(requestType, Collect) {

									PluginEngineLogger.Trace(fmt.Sprintf("Collect request: %v", context[snmp.ObjectIp]))

									snmp.Collect(context, &errors)

								} else {
									PluginEngineLogger.Trace(fmt.Sprintf("Check availability request: %v", context[snmp.ObjectIp]))

									utils.CheckAvailability(context)
								}
							}
						default:

							PluginEngineLogger.Error("Unsupported plugin type!")
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

				}(context)

			}

			wg.Wait()

			//result := utils.SendResponse(contexts)

			jsonOutput, err := json.Marshal(contexts)

			if err != nil {

				log.Fatal("JSON Marshal Error: ", err)

			}

			encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

			PluginEngineLogger.Info(encodedString)

			global.Responses <- encodedString

			PluginEngineLogger.Info(fmt.Sprintf("Result sent to socket"))

		}
	}
}
