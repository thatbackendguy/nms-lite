package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	. "plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/plugins/snmp"
	"plugin-engine/src/pluginengine/server"
	"plugin-engine/src/pluginengine/utils"
	"strings"
)

var PluginEngineLogger = utils.GetLogger(LogFilesPath, SystemLoggerName)

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
		request := <-Requests

		PluginEngineLogger.Trace(request)

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

		PluginEngineLogger.Trace(string(decodedContext))

		for _, context := range contexts {

			if pluginName, ok := context[PluginName].(string); ok {

				switch pluginName {

				case NetworkDevice:

					if requestType, ok := context[RequestType].(string); ok {

						if strings.EqualFold(requestType, Discovery) {

							PluginEngineLogger.Trace(fmt.Sprintf("Discovery request: %v", context[ObjectIp]))

							go snmp.Discovery(context)

						} else if strings.EqualFold(requestType, Collect) {

							PluginEngineLogger.Trace(fmt.Sprintf("Collect request: %v", context[ObjectIp]))

							go snmp.Collect(context)

						} else {

							PluginEngineLogger.Trace(fmt.Sprintf("Check availability request: %v", context[ObjectIp]))

							go utils.CheckAvailability(context)

						}
					}

				default:

					PluginEngineLogger.Error("Unsupported plugin type!")
				}
			}

		}
	}

}
