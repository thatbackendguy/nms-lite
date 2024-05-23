package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"log"
	"os"
	"plugin-engine/global"
	"plugin-engine/logger"
	"plugin-engine/plugins/snmp"
	"strings"
	"sync"
	"time"
)

var PluginEngineLogger = logger.NewLogger(global.LogFilesPath, global.SystemLoggerName)

const (
	RequestType   = "request.type"
	Discovery     = "Discovery"
	PluginName    = "plugin.name"
	NetworkDevice = "Network"
)

func main() {
	agent := false

	context, _ := zmq.NewContext()

	defer context.Term()

	// Create a new PUB socket
	socket, _ := context.NewSocket(zmq.PUSH)

	defer socket.Close()

	if len(os.Args) > 1 {
		if len(os.Args[1]) < 10 && os.Args[1] == "--agent" {
			agent = true
		}
	}

	stringContext := make([]byte, 0)

	stringContext, err := os.ReadFile("/home/yash/Documents/GitHub/nms-lite/plugin-engine/context-config.json")

	if err != nil {

		PluginEngineLogger.Error("Error reading file:" + err.Error())

		return
	}

	PluginEngineLogger.Info(fmt.Sprintf("file read: %v", string(stringContext)))

	wg := sync.WaitGroup{}

	for {

		PluginEngineLogger.Info("Starting Plugin Engine")

		contexts := make([]map[string]interface{}, 0)

		if agent == false {

			stringContext, err = base64.StdEncoding.DecodeString(os.Args[1])

			if err != nil {

				PluginEngineLogger.Error(fmt.Sprintf("base64 decoding error: %s", err.Error()))

				return

			}
		}

		err = json.Unmarshal(stringContext, &contexts)

		if err != nil {

			PluginEngineLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

			return
		}

		PluginEngineLogger.Info(string(stringContext))

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

							} else {

								PluginEngineLogger.Trace(fmt.Sprintf("Collect request: %v", context[snmp.ObjectIp]))

								snmp.Collect(context, &errors)

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

		if agent {
			// Connect to the subscriber's address
			err := socket.Connect("tcp://localhost:9090")

			if err != nil {

				PluginEngineLogger.Error(err.Error())

			}

			send, err := socket.Send(encodedString, 0)

			if err != nil {

				PluginEngineLogger.Error(err.Error())

			}

			PluginEngineLogger.Info(fmt.Sprintf("Result sent to socket: %v", send))

			time.Sleep(1 * time.Minute)

		} else {

			fmt.Println(encodedString)

			break
		}
	}
}
