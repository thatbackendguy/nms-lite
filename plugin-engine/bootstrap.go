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

const (
	RequestType   = "request.type"
	Discovery     = "Discovery"
	PluginName    = "plugin.name"
	NetworkDevice = "Network"
	LoggerName    = "bootstrap"
)

var pluginEngineLogger = utils.NewLogger(utils.LogFilesPath, LoggerName)

func main() {

	var wg sync.WaitGroup

	pluginEngineLogger.Info("Starting Plugin Engine")

	decodedBytes, err := base64.StdEncoding.DecodeString(os.Args[1])

	if err != nil {

		pluginEngineLogger.Error(fmt.Sprintf("base64 decoding error: %s", err.Error()))

		return

	}

	contexts := make([]map[string]interface{}, 0)

	err = json.Unmarshal(decodedBytes, &contexts)

	if err != nil {
		pluginEngineLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

		return
	}

	pluginEngineLogger.Debug(string(decodedBytes))

	for _, context := range contexts {

		wg.Add(1)

		go func(context map[string]interface{}) {

			defer wg.Done()

			errors := make([]map[string]interface{}, 0)

			defer func(context map[string]interface{}, contexts []map[string]interface{}) {

				if r := recover(); r != nil {

					pluginEngineLogger.Error("error occurred!")

					context[utils.Status] = utils.Failed

					utils.SendResponse(contexts)

					return
				}
			}(context, contexts)

			if pluginName, ok := context[PluginName].(string); ok {

				switch pluginName {

				case NetworkDevice:

					if requestType, ok := context[RequestType].(string); ok {

						if strings.EqualFold(requestType, Discovery) {

							snmp.Discovery(context, &errors)

						} else {

							snmp.Collect(context, &errors)

						}
					}
				default:

					pluginEngineLogger.Error("Unsupported plugin type!")
				}
			}
			if len(errors) > 0 {
				context[utils.Status] = utils.Failed

				context[utils.Error] = errors
			} else {
				context[utils.Status] = utils.Success
			}

		}(context)

	}

	wg.Wait()

	utils.SendResponse(contexts)
}
