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
	DeviceType    = "device.type"
	NetworkDevice = "Network"
)

func main() {

	var wg sync.WaitGroup

	utils.LoggerInit()

	utils.Loggers["pluginEngine"].Info("Starting Plugin Engine")

	decodedBytes, err := base64.StdEncoding.DecodeString(os.Args[1])

	if err != nil {

		utils.Loggers["pluginEngine"].Error(fmt.Sprintf("Base64 decoding error: %s", err.Error()))

		return

	}

	context := make([]map[string]interface{}, 0)

	err = json.Unmarshal(decodedBytes, &context)

	if err != nil {
		utils.Loggers["pluginEngine"].Error(fmt.Sprintf("Unable to convert JSON string to map: %s", err.Error()))

		return
	}

	wg.Add(len(context))

	for _, device := range context {

		go func(device map[string]interface{}) {

			errors := make([]map[string]interface{}, 0)

			switch device[DeviceType].(string) {

			case NetworkDevice:

				requestType := device[RequestType].(string)

				if strings.EqualFold(requestType, Discovery) {

					snmp.Discovery(device, &errors)

				} else {

					snmp.Collect(device, &errors)

				}

			default:

				utils.Loggers["pluginEngine"].Error("Unsupported device type!")
			}

			if len(errors) > 0 {
				device[utils.Status] = utils.Failed

				device[utils.Error] = errors
			} else {
				device[utils.Status] = utils.Success
			}

			wg.Done()
		}(device)

	}

	wg.Wait()
	utils.SendResponse(context)
}
