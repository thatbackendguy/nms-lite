package engine

import (
	"fmt"
	. "plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/plugins/snmp"
	"plugin-engine/src/pluginengine/utils"
	"strings"
)

const (
	RequestType   = "request.type"
	Discovery     = "Discovery"
	Collect       = "Collect"
	PluginName    = "plugin.name"
	NetworkDevice = "Network"
)

var logger = utils.GetLogger(LogFilesPath, "engine")

func Start() {

	defer func() {
		if err := recover(); err != nil {

			logger.Error(fmt.Sprintf("recovered from panic: %v", err))

			Start()
		}
	}()

	for {
		request := <-Requests

		for _, context := range request {

			if pluginName, ok := context[PluginName].(string); ok {

				switch pluginName {

				case NetworkDevice:

					if requestType, ok := context[RequestType].(string); ok {

						if strings.EqualFold(requestType, Discovery) {

							logger.Trace(fmt.Sprintf("Discovery request: %v", context[ObjectIp]))

							go snmp.Discovery(context)

						} else if strings.EqualFold(requestType, Collect) {

							logger.Trace(fmt.Sprintf("Collect request: %v", context[ObjectIp]))

							go snmp.Collect(context)

						} else {

							logger.Trace(fmt.Sprintf("Check availability request: %v", context[ObjectIp]))

							go utils.CheckAvailability(context)

						}
					}

				default:

					logger.Error("Unsupported plugin type!")
				}
			}
		}
	}
}
