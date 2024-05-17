package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"plugin-engine/plugins/snmp"
	"plugin-engine/utils"
	"strings"
	"sync"
)

var PluginEngineLogger = utils.NewLogger(utils.LogFilesPath, utils.SystemLoggerName)

const (
	RequestType   = "request.type"
	Discovery     = "Discovery"
	PluginName    = "plugin.name"
	NetworkDevice = "Network"
)

func main() {

	wg := sync.WaitGroup{}

	PluginEngineLogger.Info("Starting Plugin Engine")

	decodedBytes, err := base64.StdEncoding.DecodeString(os.Args[1])

	if err != nil {

		PluginEngineLogger.Error(fmt.Sprintf("base64 decoding error: %s", err.Error()))

		return

	}

	contexts := make([]map[string]interface{}, 0)

	err = json.Unmarshal(decodedBytes, &contexts)

	if err != nil {
		PluginEngineLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

		return
	}

	PluginEngineLogger.Info(string(decodedBytes))

	for _, context := range contexts {

		wg.Add(1)

		go func(context map[string]interface{}) {

			defer wg.Done()

			errors := make([]map[string]interface{}, 0)

			defer func(context map[string]interface{}, contexts []map[string]interface{}) {

				if r := recover(); r != nil {

					PluginEngineLogger.Error(fmt.Sprintf("some error occurred!, reason : %v", r))

					context[utils.Status] = utils.Failed

				}
			}(context, contexts)

			if pluginName, ok := context[PluginName].(string); ok {

				switch pluginName {

				case NetworkDevice:

					if requestType, ok := context[RequestType].(string); ok {

						if strings.EqualFold(requestType, Discovery) {

							PluginEngineLogger.Trace(fmt.Sprintf("Discovery request: %v", context[snmp.ObjectIp]))

							snmp.Discovery(context, &errors)

						} else {

							PluginEngineLogger.Trace(fmt.Sprintf("Collect request: %v", context[snmp.ObjectIp]))

							snmp.Collect(context, &errors)

						}
					}
				default:

					PluginEngineLogger.Error("Unsupported plugin type!")
				}
			}

			context[utils.Error] = errors

			if _, ok := context[utils.Result]; ok {

				if len(context[utils.Result].(map[string]interface{})) <= 0 && len(errors) > 0 {

					context[utils.Status] = utils.Failed

				} else {
					context[utils.Status] = utils.Success
				}
			} else {
				context[utils.Status] = utils.Failed
			}

		}(context)

	}

	wg.Wait()

	utils.SendResponse(contexts)
}
