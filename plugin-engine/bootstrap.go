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
)

func main() {

	var wg sync.WaitGroup

	utils.PluginEngineLogger.Info("Starting Plugin Engine")

	decodedBytes, err := base64.StdEncoding.DecodeString(os.Args[1])

	if err != nil {

		utils.PluginEngineLogger.Error(fmt.Sprintf("base64 decoding error: %s", err.Error()))

		return

	}

	contexts := make([]map[string]interface{}, 0)

	err = json.Unmarshal(decodedBytes, &contexts)

	if err != nil {
		utils.PluginEngineLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

		return
	}

	utils.PluginEngineLogger.Info(string(decodedBytes))

	for _, context := range contexts {

		wg.Add(1)

		go func(context map[string]interface{}) {

			defer wg.Done()

			errors := make([]map[string]interface{}, 0)

			defer func(context map[string]interface{}, contexts []map[string]interface{}) {

				if r := recover(); r != nil {

					//fmt.Println(r)

					utils.PluginEngineLogger.Error("error occurred!")

					context[utils.Status] = utils.Failed

					//utils.SendResponse(contexts)

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

					utils.PluginEngineLogger.Error("Unsupported plugin type!")
				}
			}

			context[utils.Error] = errors

			// TODO: for collect result is array and for disc result is map...generalize it
			if len(context[utils.Result].(map[string]interface{})) <= 0 && len(errors) > 0 {

				context[utils.Status] = utils.Failed

			} else {
				context[utils.Status] = utils.Success
			}

		}(context)

	}

	wg.Wait()

	utils.SendResponse(contexts)
}
