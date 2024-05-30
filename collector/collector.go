package main

import (
	"collector/logger"
	"encoding/json"
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"os"
	"path/filepath"
	"strings"
)

var collectorLogger = logger.NewLogger("go-engine/logs", "collector")

func main() {
	zmqContext, _ := zmq.NewContext()

	defer zmqContext.Term()

	socket, _ := zmqContext.NewSocket(zmq.PULL)

	defer socket.Close()

	socket.Bind("tcp://*:9999")

	for {

		data, _ := socket.RecvBytes(0)

		contexts := make([]map[string]interface{}, 0)

		err := json.Unmarshal(data, &contexts)

		if err != nil {

			collectorLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

			return
		}

		for _, result := range contexts {
			if strings.EqualFold(result["status"].(string), "success") {

				pollTime := result["poll.time"].(string)

				objectIp := result["object.ip"].(string)

				if interfacesData, ok := result["result"].(map[string]interface{})["interface"].([]interface{}); ok {

					for _, value := range interfacesData {
						if interfaceData, ok := value.(map[string]interface{}); ok {

							interfaceName := interfaceData["interface.name"].(string)

							folderPath := "./metrics-result/" + objectIp + "/" + interfaceName

							err = os.MkdirAll(fmt.Sprintf(folderPath), 0755)

							if err != nil {

								collectorLogger.Info("Error creating folder:" + err.Error())

								return
							}

							collectorLogger.Info("Folder created successfully")

							for name, metrics := range interfaceData {
								filePath := filepath.Join(folderPath, name)

								file, err := os.OpenFile(filePath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)

								defer file.Close()

								if err != nil {

									collectorLogger.Error("Error opening file: " + filePath)

									return
								}

								_, err = file.WriteString(fmt.Sprintf("%v,%s\n", metrics, pollTime))

								if err != nil {
									collectorLogger.Error("Error writing to file:" + err.Error())

									return
								}

								collectorLogger.Info("Data appended to file successfully")
							}
						}
					}
				}

			}

		}
	}
}
